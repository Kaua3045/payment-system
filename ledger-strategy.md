# Arquitetura Ledger-Centric: A Verdade Absoluta e a Projeção de Saldo

Este documento define a transição do sistema para uma arquitetura onde o **Ledger é a fonte única da verdade**, e o saldo na conta é apenas uma **projeção assíncrona** para performance de leitura.

---

## 1. O Fluxo de Verdade (Ledger-First)

Nesta arquitetura, a validade de uma transação não depende da tabela `accounts`, mas sim da integridade do `ledger`.

1.  **Request:** Chega a intenção de PIX.
2.  **Validação de Saldo (Ledger):** O sistema consulta o Ledger (usando Snapshots + Entradas recentes) para garantir que há saldo.
3.  **Persistência no Ledger:** Grava-se a `Transaction` e as `LedgerEntries` de forma atômica. Uma vez gravado aqui, o dinheiro "mudou de dono".
4.  **Projeção (Account):** Um evento é disparado (Outbox/Kafka) para atualizar o saldo na tabela `accounts`. Se isso falhar, o Ledger ainda tem a verdade.

---

## 2. Implementação Sugerida (Código e Estrutura)

### 2.1. Domain: A Entidade Ledger
```java
// domain/src/main/java/com/payment/system/domain/ledger/LedgerEntry.java
public record LedgerEntry(
    LedgerEntryId id,
    AccountId accountId,
    TransactionId transactionId,
    BigDecimal amount,
    LedgerType type,
    Instant createdAt
) {
    public static LedgerEntry newDebit(AccountId accId, TransactionId txId, BigDecimal amount) {
        return new LedgerEntry(LedgerEntryId.unique(), accId, txId, amount.negate(), LedgerType.DEBIT, Instant.now());
    }
    // ... newCredit ...
}
```

### 2.2. Use Case: O Motor de Execução
```java
// application/src/main/java/com/payment/system/application/usecases/transactions/CreateTransactionUseCase.java
@Transactional
public TransactionOutput execute(CreateTransactionCommand cmd) {
    // TODO: [FRAUDE] Inserir verificação de risco síncrona aqui antes de qualquer consulta
    
    // 1. Validação de Saldo Real via Ledger (Snapshot + Delta)
    BigDecimal actualBalance = ledgerRepository.calculateBalance(cmd.senderId());
    if (actualBalance.compareTo(cmd.amount()) < 0) {
        throw new InsufficientFundsException();
    }

    // 2. Criação do Agregado de Transação
    var transaction = Transaction.newTransaction(cmd.senderId(), cmd.receiverId(), cmd.amount());
    
    // 3. Persistência Atômica (Fonte da Verdade)
    // Aqui salvamos Transaction e LedgerEntries no mesmo commit
    transactionRepository.save(transaction); 
    ledgerRepository.save(LedgerEntry.newDebit(cmd.senderId(), transaction.getId(), cmd.amount()));
    ledgerRepository.save(LedgerEntry.newCredit(cmd.receiverId(), transaction.getId(), cmd.amount()));

    // 4. Disparo da Projeção (Async)
    // domainEventPublisher.publish(new TransactionCompleted(transaction));
    
    return TransactionOutput.from(transaction);
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

## 9. O Conceito de Reserva de Saldo (Balance Reservation)

Você perguntou: "Crio um Ledger de reserva?". A resposta curta é: **Não**. O Ledger é para fatos consumados. A reserva é um estado transiente para garantir que o dinheiro não seja gasto duas vezes enquanto o Ledger processa.

### 9.1. Onde a reserva vive?
A reserva deve viver no **Account Service** (ou contexto de conta). Ela pode ser uma coluna `reserved_balance` ou uma tabela lateral `account_reservations`.

**Fluxo Técnico:**
1.  **Request:** "Quero transferir 100".
2.  **Lock & Reserve:** `UPDATE accounts SET reserved_balance = reserved_balance + 100 WHERE id = :id AND (balance - reserved_balance) >= 100`.
3.  **Garantia:** Se o passo acima funcionou, você tem 100 "congelados". Ninguém mais toca neles.
4.  **Ledger:** Agora você chama o Ledger Service com segurança.
5.  **Liberação:** Após o Ledger confirmar, você faz: `UPDATE accounts SET balance = balance - 100, reserved_balance = reserved_balance - 100 WHERE id = :id`.

---

## 10. Resiliência e Falhas em Sistemas Distribuídos (Padrão Saga)

Quando mudarmos para microsserviços, o banco de dados não será o mesmo. Usamos o padrão **Saga Orquestrada**.

### 10.1. O Fluxo de Sucesso
-   **App:** Chama Orchestrator.
-   **Orchestrator:** Manda Account Service reservar (Status: `RESERVED`).
-   **Orchestrator:** Manda Ledger Service gravar (Status: `COMMITTED`).
-   **Orchestrator:** Manda Account Service confirmar (Status: `FINALIZED`).

### 10.2. O Fluxo de Falha (Compensação)
Se o Ledger Service estiver fora do ar ou retornar erro após a reserva:
1.  **Detector de Falha:** O Orchestrator percebe o erro ou timeout.
2.  **Compensação:** Ele envia um comando `CancelReservation` para o Account Service.
3.  **Ação:** O Account Service subtrai o valor do `reserved_balance`, liberando o saldo para o cliente novamente.

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

## 12. Estrutura do Balance Service (O Guardião do Saldo)

Para separar a responsabilidade, o `Account` deixa de ter o saldo como "verdade" e passamos a ter um contexto de **Balance**. Mesmo que hoje esteja no mesmo repositório, trate-o como um serviço isolado.

### 12.1. Entidade Balance e Reservation
```java
// domain/src/main/java/com/payment/system/domain/balance/Balance.java
public class Balance {
    private AccountId accountId;
    private BigDecimal consolidatedBalance; // O que o Ledger confirmou por último
    private List<BalanceReservation> reservations; // O que está "no limbo"

    public BigDecimal getAvailableBalance() {
        BigDecimal reservedSum = reservations.stream()
            .map(BalanceReservation::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        return consolidatedBalance.subtract(reservedSum);
    }
}

// domain/src/main/java/com/payment/system/domain/balance/BalanceReservation.java
public record BalanceReservation(
    TransactionId transactionId, // Link vital para compensação
    BigDecimal amount,
    Instant expiresAt,
    ReservationStatus status // PENDING, CONFIRMED, CANCELLED
) {}
```

### 12.2. O Novo UseCase de Transferência (Visão Futura)
Este código reflete como o sistema opera hoje (monorepo) mas preparado para ser cortado em microsserviços amanhã.

```java
// application/src/main/java/com/payment/system/application/usecases/transactions/CreateTransactionUseCase.java
@Transactional
public TransactionOutput execute(CreateTransactionCommand cmd) {
    // 1. TODO: [FRAUDE] Verificação síncrona de risco. Se falhar, nem reserva o saldo.
    fraudService.verify(cmd);

    // 2. RESERVA DE SALDO (Contexto: Balance Service)
    // Aqui fazemos o LOCK PESSIMISTA na tabela de 'balances' ou 'reservations'
    // Se o microserviço for separado, isso seria uma chamada gRPC/HTTP.
    var reservationId = balanceService.reserve(cmd.senderId(), cmd.amount(), cmd.transactionId());

    try {
        // 3. REGISTRO NO LEDGER (Contexto: Ledger Service - Fonte da Verdade)
        // O Ledger valida se o saldo REAL (Snapshot + Delta) ainda é válido.
        var ledgerEntries = ledgerService.record(cmd.toTransaction());

        // 4. CONFIRMAÇÃO (Contexto: Balance Service)
        // Libera a reserva e atualiza o 'consolidatedBalance' de forma definitiva.
        balanceService.confirm(reservationId);

        // 5. TODO: [FRAUDE] Disparar evento assíncrono para análise de comportamento.
        eventPublisher.publish(new TransactionFinalized(cmd.transactionId()));

    } catch (Exception e) {
        // 6. COMPENSAÇÃO (Saga Rollback)
        // Se o Ledger falhar ou der timeout, precisamos devolver os 100 ao 'availableBalance'
        balanceService.cancelReservation(reservationId);
        throw e;
    }

    return new TransactionOutput(cmd.transactionId(), "SUCCESS");
}
```

### 12.3. O Que Modificar Agora no Projeto:
1.  **Account Entity:** Remova o campo `balance`. Crie uma nova tabela `balances` e `balance_reservations`.
2.  **Transaction Entity:** Ela agora serve como o `Orchestrator ID`.
3.  **Logs de Tracing:** Em cada passo acima, logue o `transactionId`. Isso será sua única salvação para debugar quando os serviços estiverem separados.

---
*Conclusão: O Balance Service gerencia a "promessa" de dinheiro, enquanto o Ledger Service gerencia o "fato" do dinheiro.*

---

## 13. Implementação JDBC: Queries de Missão Crítica

Para garantir a performance de milhares de TPS, o SQL deve ser cirúrgico. Aqui estão as queries fundamentais para o `AccountJdbcRepository` e `LedgerJdbcRepository`.

### 13.1. Reserva de Saldo (Com Slot Sharding)
Esta query garante que você não trave a conta inteira, apenas um dos slots de reserva.

```sql
-- Busca saldo disponível somando slots e subtraindo reservas
-- O 'FOR UPDATE' garante que ninguém mude o saldo enquanto decidimos
SELECT 
    (SUM(s.balance) - COALESCE((SELECT SUM(amount) FROM balance_reservations WHERE account_id = :accId AND status = 'PENDING'), 0)) as available_balance
FROM balance_slots s
WHERE s.account_id = :accId
FOR UPDATE;

-- Se disponível, insere a reserva
INSERT INTO balance_reservations (transaction_id, account_id, amount, status, created_at)
VALUES (:txId, :accId, :amount, 'PENDING', NOW());
```

### 13.2. Persistência em Lote (Otimização JDBC)
No `LedgerJdbcRepository`, usamos `addBatch()` para enviar Transaction + 2 LedgerEntries em um único round-trip.

```java
public void persistAtomic(Transaction tx, List<LedgerEntry> entries) {
    jdbcTemplate.batchUpdate(
        "INSERT INTO transactions (...) VALUES (...)",
        "INSERT INTO ledger_entries (id, account_id, amount, type) VALUES (?, ?, ?, ?)",
        new BatchPreparedStatementSetter() {
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                LedgerEntry entry = entries.get(i);
                ps.setString(1, entry.id());
                ps.setString(2, entry.accountId());
                ps.setBigDecimal(3, entry.amount());
                ps.setString(4, entry.type().name());
            }
            public int getBatchSize() { return entries.size(); }
        }
    );
}
```

### 13.3. Confirmação (Efetivação do Saldo)
Após o Ledger confirmar, atualizamos o saldo consolidado e limpamos a reserva.

```sql
-- 1. Atualiza o saldo real no slot 0 (ou slot específico)
UPDATE balance_slots 
SET balance = balance - :amount 
WHERE account_id = :accId AND slot_id = :slotId;

-- 2. Marca a reserva como CONFIRMADA (ou deleta)
UPDATE balance_reservations 
SET status = 'CONFIRMED' 
WHERE transaction_id = :txId;
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
*Perspectiva de Engenharia de Sistemas Financeiros de Missão Crítica.*
