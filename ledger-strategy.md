# Arquitetura Ledger-Centric: A Verdade Absoluta e a Projeção de Saldo

Este documento define a transição do sistema para uma arquitetura onde o **Ledger é a fonte única da verdade**, e o saldo na conta é apenas uma **projeção assíncrona** para performance de leitura.

---

## 1. O Fluxo de Verdade (Ledger-Centric Authorization)

Nesta arquitetura, a validade de uma transação e o controle de reservas residem exclusivamente no **Ledger Service**. O `Balance Service` e a tabela `accounts` são apenas **projeções de leitura** (Read Models).

### 1.1. Fluxo de Autorização "Bancário" (Ledger-First)

1.  **Intenção de Gasto:** O orquestrador solicita uma reserva ao Ledger.
2.  **Cálculo da "Última Milha":** O Ledger calcula o saldo em tempo real: `(Snapshot + Delta de Entries) - Reservas Ativas`.
3.  **Reserva Atômica:** Se houver saldo, o Ledger grava uma `LedgerReservation` com um `expiresAt`. Isso é feito no mesmo banco de dados do Ledger, garantindo atomicidade.
4.  **Execução da Transação:** Com o saldo garantido (reservado), o sistema executa a lógica de negócio (Fraude, etc.).
5.  **Efetivação no Ledger:** 
    *   **Sucesso:** O Ledger converte a reserva em `LedgerEntry` (débito) e marca a reserva como `CONFIRMED`.
    *   **Falha:** O Ledger marca a reserva como `CANCELLED` ou deixa expirar (TTL).
6.  **Sincronização de Projeção:** O `Balance Service` escuta os eventos do Ledger e atualiza a visão do usuário.

---

## 2. A Entidade Ledger e Reservas

### 2.1. LedgerReservation (O Lock Lógico)
A reserva no Ledger é o que impede o gasto duplo (double-spending) em um ambiente distribuído.

```java
// domain/src/main/java/com/payment/system/domain/ledger/LedgerReservation.java
public class LedgerReservation extends AggregateRoot<LedgerReservationId> {
    private TransactionId transactionId;
    private AccountId accountId;
    private BigDecimal amount;
    private Instant expiresAt;
    private ReservationStatus status; // PENDING, CONFIRMED, CANCELLED

    // O saldo disponível para o Ledger é sempre:
    // Balance = (Snapshots + Entries) - SUM(Reservations WHERE status = PENDING AND expiresAt > NOW())
}
```

### 2.2. O Use Case Refatorado (Ledger-Centric)
```java
@Transactional
public TransactionOutput execute(CreateTransactionCommand cmd) {
    // 1. TODO: [FRAUDE] Verificação síncrona de risco.
    
    // 2. Tenta Reservar no Ledger (Juiz Supremo)
    // O LedgerRepo faz: SELECT (Saldo - Reservas) FOR UPDATE
    var reservation = ledgerService.reserve(cmd.senderId(), cmd.amount(), cmd.transactionId());

    try {
        // 3. Processamento Adicional (Se necessário)
        
        // 4. Efetivação: Converte Reserva em Entrada de Débito/Crédito
        ledgerService.confirm(reservation.getId());
        
        return TransactionOutput.success(cmd.transactionId());
    } catch (Exception e) {
        // 5. Compensação: Cancela a reserva no Ledger
        ledgerService.cancel(reservation.getId());
        throw e;
    }
}
```

### 2.3. Infra: Logs e Tracing (Observabilidade)
```java
// Exemplo de rastreabilidade com OpenTelemetry / SLF4J
log.info("[TRANSACTION_START] TraceID: {}, Sender: {}, Amount: {}", traceId, cmd.senderId(), cmd.amount());
try {
    // execução...
    log.info("[LEDGER_COMMITTED] TxID: {}, Status: SUCCESS", transaction.getId());
} catch (Exception e) {
    log.error("[TRANSACTION_FAILED] Reason: {}", e.getMessage());
    throw e;
}
```

---

## 3. O que modificar ou apagar no projeto existente?

1.  **MODIFICAR:** `Account.java` no domínio. Remova a lógica de "débito" que altera o atributo `balance` diretamente como fonte da verdade. O `balance` agora deve ser tratado como um `@Transient` ou um atributo de cache.
2.  **APAGAR:** Qualquer validação de saldo que aponte diretamente para a coluna `balance` da tabela `accounts` sem conferir o Ledger em casos críticos.
3.  **CRIAR:** Tabela `ledger_snapshots`. Sem ela, o `calculateBalance` via Ledger ficará lento com o tempo. Guarde o saldo consolidado a cada 1.000 transações.

---

## 4. Tratamento de Falhas e Idempotência

-   **Se o Ledger falhar:** O rollback do banco garante que a transação nunca existiu.
-   **Se a Projeção (Account) falhar:** O sistema continua íntegro. Um worker de reconciliação lerá o Ledger e forçará a atualização da tabela `accounts` para refletir a realidade.
-   **Idempotência:** Toda `Transaction` deve carregar um `idempotency_key` (ex: vindo do app ou do PIX) para evitar débitos duplicados no Ledger em caso de retry.

---
*Conclusão: Nesta visão, a Account é apenas um "Read Model" rápido. A inteligência e a segurança residem no Ledger e nos seus Snapshots.*

---

## 5. A Necessidade da Entidade Transaction

Você pode se perguntar: "Se o Ledger tem tudo, por que ainda preciso da tabela `transactions`?"

1.  **Status de Negócio:** A Transaction representa a intenção e o resultado final para o usuário (Ex: `WAITING_FRAUDE`, `REJECTED`, `COMPLETED`). O Ledger são apenas entradas contábeis.
2.  **Correlação (Linkage):** A Transaction é a "cola" que une o débito de uma conta ao crédito de outra. No Ledger, são linhas independentes.
3.  **Idempotência:** O `transaction_id` vindo da origem evita que o mesmo PIX gere entradas duplicadas no Ledger caso o cliente clique no botão duas vezes.

---

## 6. Estratégia de Ledger Snapshots (Alta Performance)

Para evitar somar milhões de linhas no `calculateBalance`, implementamos Snapshots.

### 6.1. Tabela de Snapshots
```sql
CREATE TABLE ledger_snapshots (
    account_id VARCHAR(26) PRIMARY KEY,
    last_ledger_id VARCHAR(26), -- O ID da última entrada processada
    balance NUMERIC(19,4),
    updated_at TIMESTAMP WITH TIME ZONE
);
```

### 6.2. Algoritmo de Saldo
```java
public BigDecimal calculateBalance(AccountId id) {
    // 1. Pega o último snapshot
    Snapshot snap = snapshotRepository.findLast(id); 
    
    // 2. Soma apenas o delta do Ledger a partir do last_ledger_id do snapshot
    BigDecimal delta = ledgerRepository.sumAmountSince(id, snap.getLastLedgerId());
    
    return snap.getBalance().add(delta);
}
```

---

## 7. Compensações e Falhas (Preparando para Microsserviços)

Mesmo estando em um monolito hoje, o design deve prever falhas em contextos diferentes.

1.  **Falha na Reserva/Validação:** Retorno imediato de erro ao usuário (síncrono).
2.  **Falha na Escrita do Ledger (Timeout/Crash):** 
    - Se o Ledger não confirmou, o PIX não aconteceu. 
    - **Compensação:** Um processo de reconciliação verifica transações em aberto no Balance Service que não possuem entradas no Ledger e as cancela (Rollback da reserva).
3.  **Falha na Projeção (Account Balance):**
    - O Ledger está OK, o dinheiro mudou de dono. O saldo na conta está "sujo" (desatualizado).
    - **Auto-Correção:** O consumidor do evento de saldo deve ser **idempotente**. Se ele falhar, o Kafka/Outbox tentará novamente até que a conta reflita o Ledger.

---

## 8. TODO: Sistema de Fraude (Future Implementation)

-   [ ] **Fraude Síncrona:** Inserir `fraudService.analyze(tx)` no UseCase antes do commit do Ledger. Se `Status == REJECTED`, abortar transação.
-   [ ] **Fraude Assíncrona:** Consumir eventos do Ledger para analisar padrões de comportamento maciços e bloquear a conta (`AccountStatus.BLOCKED`).

---
*Conclusão: O Ledger é o juiz, a Transaction é a certidão e a Account é apenas a placa de exibição.*

---

## 9. O Conceito de Reserva de Saldo (Ledger-Centric Reservation)

Conforme sua visão de engenharia, a reserva agora é uma **primitiva do Ledger**. O Ledger é quem "sabe" e "guarda" o dinheiro temporariamente.

### 9.1. Onde a reserva vive?
A reserva vive na tabela `ledger_reservations` (ou similar) no banco de dados do **Ledger Service**.

**Fluxo Técnico:**
1.  **Request:** "Quero transferir 100".
2.  **Lock & Reserve (Ledger):** 
    ```sql
    -- O Ledger valida o saldo real e cria a reserva em uma transação atômica
    INSERT INTO ledger_reservations (transaction_id, account_id, amount, status, expires_at)
    SELECT :txId, :accId, :amount, 'PENDING', :expiresAt
    WHERE (
        (SELECT COALESCE(balance, 0) FROM ledger_snapshots WHERE account_id = :accId) +
        (SELECT COALESCE(SUM(amount), 0) FROM ledger_entries WHERE account_id = :accId AND id > (SELECT last_ledger_id FROM ledger_snapshots WHERE account_id = :accId)) -
        (SELECT COALESCE(SUM(amount), 0) FROM ledger_reservations WHERE account_id = :accId AND status = 'PENDING' AND expires_at > NOW())
    ) >= :amount;
    ```
3.  **Garantia:** Se o `INSERT` funcionou, o dinheiro está reservado no "Juiz Supremo".
4.  **Processamento:** O orquestrador segue com o fluxo.
5.  **Conversão:** Ao confirmar, a reserva vira uma `ledger_entry` e a reserva original é marcada como `CONFIRMED`.

---

## 10. Resiliência e Falhas em Sistemas Distribuídos (Padrão Saga)

Agora que o Ledger faz a reserva, a Saga fica muito mais simples e segura.

### 10.1. O Fluxo de Sucesso
-   **Orchestrator:** Solicita reserva ao Ledger.
-   **Ledger:** Reserva criada (Status: `PENDING`).
-   **Orchestrator:** Executa lógica adicional.
-   **Orchestrator:** Manda Ledger efetivar.
-   **Ledger:** Cria `LedgerEntry` e fecha reserva (Status: `CONFIRMED`).

### 10.2. O Fluxo de Falha (Compensação)
Se qualquer passo após a reserva falhar:
1.  **Orchestrator:** Manda Ledger cancelar reserva.
2.  **Ledger:** Muda status da reserva para `CANCELLED`.
3.  **Expiração Automática:** Se o orquestrador sumir, a reserva tem um `expires_at`. Após esse tempo, o cálculo de saldo do Ledger ignora essa reserva, devolvendo o poder de compra automaticamente.

### 10.3. A Falha "Muda": O Zumbi da Transação
O maior problema é o **Timeout**. Você chamou o Ledger, mas não sabe se ele gravou ou não.
-   **Solução:** O Ledger Service deve ser **Idempotente**. O Orchestrator tenta de novo com o mesmo `transaction_id`. Se o Ledger responder "já gravei", o Orchestrator segue em frente. Se responder "gravei agora", também segue.

---

## 11. O Paradoxo do Saldo e a Solução dos Grandes Bancos

Você tocou no ponto central: "Se a Account reserva, ela é a verdade. E se ela estiver errada?". 

Grandes bancos (Itaú, Nubank) resolvem isso tratando o serviço de saldo como um **Agregador de Eventos de Alta Velocidade** que nunca "esquece" o Ledger.

### 11.1. O Balance Service como um "Projection do Ledger"
O segredo é que o `Account Balance` não é uma tabela estática, é um **resultado consolidado do Ledger** que vive em um cache de altíssima performance (como Redis ou bancos In-Memory).

**O Fluxo de "Última Milha":**
1.  **Request:** "Quero transferir 100".
2.  **Verificação de Ledger (Fast-Sum):** O serviço de saldo olha para o seu Snapshot + as transações que ainda não foram consolidadas no Ledger. 
    - *Nota:* Ele não olha para uma coluna `balance` que pode estar velha; ele olha para o **ponteiro da última transação processada**.
3.  **Reserva Preditiva:** Se o Ledger diz que você tem 100, a reserva é feita.
4.  **Escrita Determinística:** Quando o Ledger grava a transação, ele envia um ID de sequência. 
5.  **Reconciliação Automática:** Se por algum motivo o Balance Service achava que você tinha 100, mas o Ledger, ao tentar gravar, percebe que o saldo real (somando tudo) é menor, o **Ledger rejeita a escrita**.
    - O Ledger é o **Juiz Supremo**. Se ele diz "Não", a reserva na Account é cancelada.

### 11.2. E se a Account estiver inconsistente?
Se a Account diz que você tem 50 mas o Ledger diz que você tem 100:
- O sistema é conservador: ele usa o **menor valor** para autorizar.
- Periodicamente, o Balance Service faz um **Reset de Verdade**: ele apaga sua projeção local e reconstrói o saldo lendo o Ledger do zero (ou do último Snapshot).

### 11.3. A Verdade Distribuída
Em bancos modernos, o saldo que você vê no app é uma **Projeção de Leitura (Eventual)**. O saldo que o sistema de transação usa é a **Projeção de Escrita (Forte)**, que está sempre sincronizada com o `head` do Ledger.

**Em resumo:** A Account não é a fonte da verdade, ela é o **Guardião da Reserva** que é validado pelo Ledger a cada passo. O dinheiro só "existe" quando o Ledger aceita a entrada.

---

## 12. Estrutura do Ledger Service (O Juiz Supremo)

Para separar a responsabilidade, o `Ledger` assume o controle total da reserva. O `Balance Service` passa a ser apenas uma **Projeção de Leitura** alimentada por eventos.

### 12.1. Entidade LedgerReservation
```java
// domain/src/main/java/com/payment/system/domain/ledger/LedgerReservation.java
public class LedgerReservation extends AggregateRoot<LedgerReservationId> {
    private TransactionId transactionId;
    private AccountId accountId;
    private BigDecimal amount;
    private Instant expiresAt;
    private ReservationStatus status; // PENDING, CONFIRMED, CANCELLED

    // O Ledger usa esta entidade para autorizar novos débitos
}
```

### 12.2. O Novo UseCase de Transferência (Ledger-Centric)
Este código reflete como o sistema opera agora, com o Ledger sendo o dono da reserva.

```java
// application/src/main/java/com/payment/system/application/usecases/transactions/CreateTransactionUseCase.java
@Transactional
public TransactionOutput execute(CreateTransactionCommand cmd) {
    // 1. TODO: [FRAUDE] Verificação síncrona de risco.
    
    // 2. RESERVA NO LEDGER (Fonte da Verdade)
    // O Ledger valida Snapshot + Entries - Reservas PENDING
    var reservation = ledgerService.reserve(cmd.senderId(), cmd.amount(), cmd.transactionId());

    try {
        // 3. REGISTRO DEFINITIVO NO LEDGER
        // Ao confirmar, o Ledger:
        // - Cria LedgerEntry (Débito) para Account A
        // - Cria LedgerEntry (Crédito) para Account B
        // - Marca reserva como CONFIRMED
        ledgerService.confirm(reservation.getId());

        // 4. TODO: [FRAUDE] Disparar evento assíncrono.
        eventPublisher.publish(new TransactionFinalized(cmd.transactionId()));

    } catch (Exception e) {
        // 5. COMPENSAÇÃO (Saga Rollback)
        ledgerService.cancelReservation(reservation.getId());
        throw e;
    }

    return new TransactionOutput(cmd.transactionId(), "SUCCESS");
}
```

### 12.3. O Que Modificar Agora no Projeto:
1.  **LedgerRepository:** Adicionar métodos para `reserve()`, `confirm()` e `cancel()`.
2.  **SQL Migration:** Criar a tabela `ledger_reservations` vinculada ao contexto do Ledger.
3.  **Balance Service:** Transformá-lo em um `EventListener` que apenas atualiza a tabela `accounts` (projeção) quando o Ledger confirma ou cancela reservas.

---
*Conclusão: O Balance Service gerencia a "promessa" de dinheiro, enquanto o Ledger Service gerencia o "fato" do dinheiro.*

---

## 13. Implementação JDBC: Queries de Missão Crítica

Para garantir a performance de milhares de TPS, o SQL deve ser cirúrgico. Aqui estão as queries fundamentais para o `LedgerJdbcRepository`.

### 13.1. Reserva de Saldo (Ledger-Centric)
Esta query valida o saldo total (Snapshot + Entries - Active Reservations) e insere a reserva em um passo atômico.

```sql
-- Lock Pessimista no Snapshot para evitar concorrência no cálculo
SELECT balance FROM ledger_snapshots WHERE account_id = :accId FOR UPDATE;

-- Validação e Inserção da Reserva
INSERT INTO ledger_reservations (id, transaction_id, account_id, amount, status, expires_at)
SELECT :resId, :txId, :accId, :amount, 'PENDING', :expiresAt
WHERE (
    (SELECT balance FROM ledger_snapshots WHERE account_id = :accId) +
    (SELECT COALESCE(SUM(amount), 0) FROM ledger_entries WHERE account_id = :accId AND id > (SELECT last_ledger_id FROM ledger_snapshots WHERE account_id = :accId)) -
    (SELECT COALESCE(SUM(amount), 0) FROM ledger_reservations WHERE account_id = :accId AND status = 'PENDING' AND expires_at > NOW())
) >= :amount;
```

### 13.2. Confirmação (Efetivação Atômica)
Quando a transação é confirmada, transformamos a reserva em fatos contábeis.

```java
public void confirm(LedgerReservation res) {
    jdbcTemplate.batchUpdate(
        "UPDATE ledger_reservations SET status = 'CONFIRMED' WHERE id = ?",
        "INSERT INTO ledger_entries (id, account_id, amount, type, transaction_id) VALUES (?, ?, ?, ?, ?)",
        // ... Batching para Débito e Crédito ...
    );
}
```

### 13.3. Limpeza de Reservas Expiradas
Um worker deve rodar periodicamente para limpar o que o orquestrador esqueceu.

```sql
UPDATE ledger_reservations 
SET status = 'CANCELLED' 
WHERE status = 'PENDING' AND expires_at < NOW();
```

### 13.4. Cálculo de Saldo Real (Snapshot + Delta)
Usada pelo Ledger para validação final de "Última Milha".

```sql
SELECT 
    (snap.balance + COALESCE(SUM(l.amount), 0)) as true_balance
FROM ledger_snapshots snap
LEFT JOIN ledger_entries l ON l.account_id = snap.account_id AND l.id > snap.last_ledger_id
WHERE snap.account_id = :accId
GROUP BY snap.balance;
```

---

## 14. O Futuro: Ledger-Centric Authorization como Motor Global

Você questionou se esta é a melhor forma. A resposta é: **Sim, para consistência absoluta.** Ao centralizar a reserva no Ledger, você remove a incerteza de ter um serviço de saldo que "acha" que tem dinheiro e um Ledger que "sabe" que não tem.

### 14.1. Por que centralizar a Reserva no Ledger?
1.  **Verdade Única:** Não há "telefone sem fio" entre o Balance Service e o Ledger. A autorização é feita sobre o dado real.
2.  **Performance de Escrita:** Inserir uma reserva (`PENDING`) é um append atômico. Se o Ledger for Sharded por `AccountId`, a contenção de lock no Snapshot é mínima.
3.  **Simplicidade Distribuída:** O Ledger torna-se o **Ponto de Sincronização** de todo o sistema financeiro.

### 14.2. O Papel do Balance Service
Neste modelo, o `Balance Service` deixa de ser um autorizador e passa a ser uma **Projeção Pura**:
- Ele consome eventos `ReservationCreated`, `ReservationConfirmed` e `ReservationCancelled`.
- Ele serve o saldo para o cliente no App (Leitura eventual).
- Se ele estiver 1 segundo atrasado, não importa para a segurança do sistema, pois o próximo PIX baterá no Ledger para autorizar.

### 14.3. Existem formas melhores?
O modelo de **Ledger-Centric Reservation** é usado por sistemas de compensação bancária e brokers de alta frequência. É a forma mais segura de lidar com dinheiro.

### 14.4. TODO: Evolução para Microserviços (Ledger-First)
- [ ] Implementar o motor de autorização dentro do microsserviço de Ledger.
- [ ] Criar o `Reservations Cleaner` (Worker) para garantir que reservas expiradas não "sujem" o saldo disponível.
- [ ] Implementar snapshots automáticos no Ledger a cada X inserções para manter o cálculo de saldo (`Snapshot + Delta`) sempre performático.

---

## 15. Snapshots Agressivos vs. Projeção Ativa (Balance Service)

Você levantou um ponto excelente: **"E se eu simplesmente tirasse snapshots do Ledger o tempo todo (ex: a cada 10 transações ou a cada segundo) e usasse isso para autorizar?"**

Essa é uma alternativa válida, mas ela esbarra em problemas de **Contenção** e **Semântica de Autorização** que os grandes bancos resolvem com o Balance Service.

### 15.1. O Problema da "Janela de Incerteza"
Mesmo com um snapshot a cada 100ms, se você receber 50 PIXs simultâneos para a mesma conta:
1.  Todos os 50 processos lerão o mesmo Snapshot + o pequeno Delta.
2.  Todos os 50 validarão que "tem saldo" (baseado na foto do passado recente).
3.  Se você não tiver um **Mecanismo de Reserva (Lock)**, você pode autorizar mais do que o saldo real permite antes que o próximo Snapshot seja tirado.

### 15.2. Comparativo Técnico

| Característica | Snapshots Agressivos (Ledger-Only) | Balance Service (Reservations) |
| :--- | :--- | :--- |
| **Simplicidade** | Alta. Menos tabelas e serviços. | Média. Exige gestão de estado de reserva. |
| **Concorrência** | Baixa. Exige lock no "fim do Ledger", causando gargalo. | Alta. O Slot Sharding permite múltiplos débitos paralelos. |
| **Escrita (IOPS)** | Altíssima. Gerar snapshots constantes consome muito disco/CPU. | Otimizada. O saldo é atualizado em lote ou via eventos. |
| **Semântica** | "O que aconteceu?" (Passado). | "O que posso gastar agora?" (Futuro/Reserva). |

### 15.3. Por que não Snapshots Agressivos sozinhos?
1.  **Write Amplification:** Tirar snapshots constantes no banco de dados gera uma carga imensa de escrita que compete com as transações reais.
2.  **O Ledger é um Log de Append:** Inserir no Ledger é rápido porque é apenas um "append". Validar saldo no Ledger exige "leitura + soma + comparação", o que é inerentemente mais lento do que ler um valor pré-agregado (Balance).
3.  **Reservas são Obrigatórias:** Em sistemas distribuídos, a reserva protege o dinheiro durante o tempo de rede entre o "OK, você tem saldo" e o "Commit no Ledger". Sem reserva, você cai no risco de *Race Conditions*.

### 15.4. A Sinergia Ideal
Os bancos usam os dois:
-   **Balance Service (Reservas):** Para o **Hot Path** (Autorização rápida e concorrente).
-   **Snapshots de Ledger:** Para o **Cold Path** (Reconciliação, fechamento de dia, auditoria e recuperação de desastre).

### 15.5. Conclusão: O Risco de Inconsistência
Como você previu, confiar apenas em snapshots agressivos sem um motor de reserva leva à **inconsistência temporária** no momento da autorização, o que em sistemas financeiros significa **prejuízo real**. A Projeção de Saldo (Balance) com Reservas é, na verdade, um snapshot "vivo" e especializado para autorização.

---

## 16. Otimização do Use Case (Limpeza e Padrões de Projeto)

Você notou que o `DefaultCreateTransactionUseCase` está "poluído" com logs, métricas e telemetria. Para um grande engenheiro de software, o código deve ser **limpo e focado no domínio**. Aqui estão as estratégias para resolver isso:

### 16.1. O Problema: Acoplamento de Infraestrutura
O Use Case atual mistura:
1.  **Orquestração de Negócio:** (PixKey -> Reserva -> Ledger -> Confirmação).
2.  **Telemetria:** (Metrics, Tracer, Logs).
3.  **Tratamento de Erro:** (Try/Catch gigantesco, Classificação de Erros).

### 16.2. Estratégia A: Padrão Decorator (Limpeza de Métricas e Logs)
Em vez de injetar `Metrics` e `Tracer` dentro do Use Case, crie um Decorator que envolve a execução.

```java
// application/.../create/LoggingCreateTransactionUseCaseDecorator.java
public class LoggingCreateTransactionUseCaseDecorator extends CreateTransactionUseCase {
    private final CreateTransactionUseCase delegate;
    private final Metrics metrics;
    private final ApplicationLogger logger;

    @Override
    public CreateTransactionOutput execute(CreateTransactionCommand input) {
        final var startTime = System.currentTimeMillis();
        try {
            var output = delegate.execute(input);
            metrics.incrementCounter("usecase_success", 1, Map.of("usecase", "pix"));
            return output;
        } catch (Exception ex) {
            metrics.incrementCounter("usecase_error", 1, Map.of("error", ex.getClass().getSimpleName()));
            logger.error("Pix transfer failed", ex);
            throw ex;
        } finally {
            metrics.recordTime("usecase_duration", System.currentTimeMillis() - startTime);
        }
    }
}
```
**Resultado:** O Use Case original fica 100% livre de telemetria.

### 16.3. Estratégia B: Extração do Orquestrador de Saga
A lógica de Reserva -> Ledger -> Confirmação pode ser movida para um `TransactionSagaOrchestrator`.

```java
// O UseCase ficaria assim:
public CreateTransactionOutput execute(CreateTransactionCommand input) {
    // 1. Validações Simples
    validate(input);

    // 2. Orquestração Atômica/Saga
    return sagaOrchestrator.runTransfer(input);
}
```

### 16.4. Estratégia C: Aspect Oriented Programming (AOP)
Se você usa Spring ou similar, pode usar `@Around` advices para lidar com Tracing e Metrics sem tocar em uma linha de código do Use Case.

### 16.5. O "Pulo do Gato": Como o Use Case ficaria (Exemplo Prático)

Imagine o `DefaultCreateTransactionUseCase` focado apenas no negócio:

```java
public class DefaultCreateTransactionUseCase extends CreateTransactionUseCase {
    @Override
    public CreateTransactionOutput execute(final CreateTransactionCommand input) {
        // TODO: [FRAUDE] Verificação síncrona
        
        // 1. Resolve Destinatário (Poderia ser um Service separado)
        final var pixKey = pixKeyService.resolve(input.pixKey());
        
        // 2. Executa a Saga Financeira
        return transactionManager.execute(() -> {
            var tx = saga.reserve(input, pixKey);
            try {
                saga.commitToLedger(tx);
                saga.confirmBalance(tx);
                return CreateTransactionOutput.from(tx);
            } catch (Exception e) {
                saga.compensate(tx);
                throw e;
            }
        });
    }
}
```

---

## 17. Tracing em Partes Específicas via Decorator e Interfaces

Para ter trace em partes muito específicas (como a chamada ao Ledger ou a Busca de Chave Pix) sem sujar o código, usamos **Composição**.

### 17.1. Decorando o Repositório ou Service
Se você quer um trace específico para o Ledger, decore o `LedgerRepository`:

```java
public class TracingLedgerRepository implements LedgerRepository {
    private final LedgerRepository delegate;
    private final TracerWrapper tracer;

    @Override
    public void saveAll(List<LedgerEntry> entries) {
        tracer.runInSpan("db.ledger.save", () -> delegate.saveAll(entries));
    }
    
    @Override
    public BigDecimal calculateBalance(AccountId id) {
        return tracer.traceWithReturn("db.ledger.calculate_balance", 
            (span) -> delegate.calculateBalance(id));
    }
}
```
**Vantagem:** O Use Case chama `ledgerRepository.calculateBalance()` e o trace acontece "magicamente" por baixo, via o decorator que foi injetado na configuração.

### 17.2. Use Case de "Pix Key Resolution"
Você mencionou criar um use case para a chave PIX. Isso é excelente para:
1.  **Reutilização:** O depósito também precisa validar chaves.
2.  **Isolamento de Cache:** Você pode colocar cache de chaves PIX apenas nesse Use Case/Service.
3.  **Tracing Dedicado:** Um span específico para "resolve-pix-key".

### 17.3. O Fluxo Final Sugerido (Clean & Scalable)

1.  **`TransactionController`** -> chama o **`TracingMetricsDecorator(CreateTransactionUseCase)`**.
2.  **`CreateTransactionUseCase`** (O Orquestrador):
    *   Chama `PixKeyService.resolve()` (Decorado com Cache/Tracing).
    *   Chama `BalanceService.reserve()` (Decorado com Metrics).
    *   Chama `LedgerService.record()` (Decorado com Tracing/Logging).
    *   Chama `BalanceService.confirm()`.

---

## 18. Reserva de Saldo em Microsserviços (O Problema do Lock Distribuído)

Você levantou um ponto crucial: **"Como reservar saldo quando não temos mais o lock do banco de dados (porque os serviços estão separados)?"**

Em sistemas distribuídos, o `SELECT FOR UPDATE` não cruza a rede. Se o Account Service e o Ledger Service estão em bancos diferentes, você perde a atomicidade ACID tradicional.

### 18.1. O Problema: Double Spending e Race Conditions
Sem o lock físico, dois PIXs de 100 podem chegar ao Account Service simultaneamente. Se ambos lerem "Saldo: 100" ao mesmo tempo, ambos podem autorizar, resultando em um saldo negativo de -100 no Ledger.

### 18.2. A Solução: Máquina de Estados de Reserva (Entidade de Primeira Classe)

Sim, exatamente! O que você descreveu é a evolução natural do lock pessimista para o **Lock Lógico Distribuído**. Em vez de travar a linha no banco, você trava a intenção de gasto através de um estado.

#### A. A Anatomia da Reserva Distribuída
Em vez de apenas uma coluna `reserved_balance`, a reserva passa a ser uma entidade com ciclo de vida próprio no banco de dados do **Balance Service**:

1.  **Estado PENDING (Criação):**
    *   **Ação:** `POST /reservations`
    *   **Lógica:** O Balance Service verifica o saldo disponível (`Consolidado - Reservas Ativas`). Se OK, cria a reserva com `status = PENDING` e vincula ao `transactionId`.
    *   **Garantia:** O dinheiro está "bloqueado". Nenhuma outra transação pode usar esse valor enquanto esta reserva existir no estado PENDING.

2.  **O Chamado ao Ledger (Juiz Supremo):**
    *   O Orchestrator chama o Ledger Service.
    *   Se o Ledger aceitar (Sucesso), ele retorna OK.
    *   Se o Ledger rejeitar (ex: erro de integridade ou fraude tardia), ele retorna Erro.

3.  **Transição de Estado (Finalização):**
    *   **Sucesso:** O Orchestrator chama o Balance Service para `COMMIT` da reserva. O status muda para `COMMITTED`, e o saldo consolidado é atualizado (diminuído) definitivamente.
    *   **Falha:** O Orchestrator chama o Balance Service para `CANCEL` da reserva. O status muda para `CANCELLED` (ou a linha é deletada), liberando o saldo instantaneamente para o cliente.

#### B. Por que isso resolve o Lock?
Porque o **Account Service** (ou Balance Service) torna-se o **único dono** do estado de disponibilidade. Ele não precisa saber se o Ledger terminou; ele só precisa saber que, para aquele `transactionId`, existe uma promessa de débito de X.

#### C. E se o Orchestrator "sumir" no meio do processo?
Este é o ponto onde o lock de banco falharia (ficaria travado para sempre ou sofreria rollback). Na Máquina de Estados:
*   Usamos um **TTL (Time-To-Live)**. Se uma reserva ficar `PENDING` por mais de 30 segundos, um worker de reconciliação entra em ação.
*   Ele pergunta ao Ledger: "Ei, a transação XYZ aconteceu?". 
*   Se o Ledger disser "Sim", o worker confirma a reserva órfã. 
*   Se disser "Não", ele cancela.

### 18.3. Optimistic Locking (Versioning) - O Complemento Necessário
Mesmo com a máquina de estados, para garantir que a *criação* da reserva seja segura contra dois processos tentando criar reservas ao mesmo tempo:
```sql
-- No Account/Balance Service
UPDATE balances 
SET version = version + 1
WHERE account_id = :id AND version = :oldVersion AND (consolidated_balance - reserved_balance) >= :amount;
```
Isso garante a **Atomicidade Local** dentro do microsserviço de balanço.

### 18.4. O "Lock Lógico" via Idempotência
O verdadeiro "lock" em microsserviços é a **Idempotência no Ledger**. 
- O Ledger Service deve garantir que para um mesmo `transactionId`, ele só grave uma vez.
- Se o Account Service reservou o dinheiro, ele fica "bloqueado" logicamente por aquele `transactionId`. Nenhuma outra transação pode usar aquele valor até que a reserva expire ou seja confirmada.

### 18.5. Llidando com a Inconsistência (Reconciliação de Reservas Órfãs)
Em microsserviços, o maior risco são as **Reservas Zumbis** (dinheiro que fica reservado mas o Ledger nunca é chamado ou o Orchestrator morre).

**Solução:**
- **TTL (Time-To-Live):** Toda reserva nasce com um tempo de expiração (ex: 30 segundos).
- **Worker de Reconciliação:** Um processo em background busca reservas `PENDING` expiradas, consulta o Ledger Service para saber se a transação existe lá. Se não existir, ele cancela a reserva automaticamente, devolvendo o saldo.

### 18.5. Conclusão: De Lock Físico para Lock Lógico
A transição para microsserviços exige que você pare de confiar no banco de dados para "travar a porta" e passe a confiar na **orquestração de estados** e na **idempotência**. O `Balance Service` torna-se o juiz local de reservas, enquanto o `Ledger` permanece como o juiz supremo final.

---

## 20. Ledger-Centric Authorization: Centralizando a Verdade e a Reserva

Você trouxe uma proposta provocativa e muito comum em arquiteturas de altíssima escala: **"E se a reserva vivesse dentro do Ledger Service, já que ele tem a verdade?"**

Essa abordagem é chamada de **Ledger-Centric Authorization**. Nela, o Ledger não é apenas um log, mas o motor de decisão em tempo real.

### 20.1. Como funcionaria o Fluxo Simplificado

1.  **Request (Orchestrator):** "Tente debitar 100 da conta A para a B".
2.  **Ledger Service (Única Chamada):**
    *   **Consulta:** O Ledger soma Snapshots + Entradas + Reservas Ativas internas.
    *   **Decisão:** Se houver saldo, ele cria a `LedgerEntry` (ou uma Reserva interna no mesmo banco).
    *   **Resposta:** Retorna Sucesso/Falha imediatamente.
3.  **Balance Service (Projeção):** Apenas recebe o evento do Ledger para atualizar o cache que o usuário vê no App.

### 20.2. Vantagens: A Força da Verdade Única

*   **Atomicidade ACID Real:** Como o saldo real (Ledger) e a reserva estão no **mesmo banco de dados**, você pode fazer o check-and-reserve em uma única transação SQL. Zero risco de inconsistência entre "o que eu reservei" e "o que o ledger permite".
*   **Menos Round-trips:** Você elimina a necessidade de coordenar dois serviços (Balance e Ledger) para a autorização. Uma chamada resolve tudo.
*   **Simplicidade de Compensação:** Se o Ledger recusar, o Orchestrator nem precisa "cancelar reserva" em outro lugar. A falha é atômica no ponto de origem.

### 20.3. Os Desafios: O Preço da Centralização

*   **Hot Path de Escrita:** O Ledger Service torna-se o componente mais crítico e mais sobrecarregado do sistema. Ele precisa aguentar a carga de leitura (consulta de saldo) e escrita (reservas/lançamentos) simultaneamente.
*   **Acoplamento:** O Ledger deixa de ser um "contador passivo" para ser um "autorizador ativo". Isso exige que ele tenha lógica de negócio (quem pode gastar, limites diários, etc.) ou que ele exponha primitivas para isso.
*   **Escalabilidade Vertical vs Horizontal:** Bancos tradicionais costumam fazer isso em Mainframes ou bancos SQL gigantes (Ex: Nubank usa o `Financial Core` que centraliza essa verdade). Para escalar horizontalmente, você precisará de um **Sharding muito agressivo por AccountId**.

### 20.4. Veredito: Qual escolher?

*   **Escolha a Reserva no Account/Balance Service se:** Você tem muitos times diferentes, quer isolar a lógica de limites e produtos do motor contábil, e aceita a complexidade da Saga para ganhar desacoplamento.
*   **Escolha a Reserva no Ledger Service se:** A consistência absoluta é inegociável, você quer performance máxima de autorização e tem infraestrutura para escalar um serviço de escrita pesada (ex: Event Sourcing com snapshots rápidos).

**Conclusão:** A sua sugestão de tornar o Balance Service apenas uma "mera projeção" é o **estado da arte** para performance. O Ledger detém a chave do cofre e a conta apenas "escuta" o que aconteceu.

---

## 21. TODO Final: Roteiro de Decisão Ledger-Centric
- [ ] **Fase 1 (Ledger-First):** Centralizar as reservas e o cálculo de saldo no Ledger Repository, tratando o balance de Account como Read-Only.
- [ ] **Fase 2 (Snapshots):** Garantir que o cálculo de saldo `Snapshot + Delta - ActiveReservations` seja a única fonte de verdade no Use Case.
- [ ] **Fase 3 (Clean Architecture):** Aplicar Decorators para logs e telemetria, mantendo o orquestrador financeiro limpo e focado no fluxo do Ledger.

---
*Perspectiva de Engenharia de Sistemas Financeiros de Missão Crítica.*
