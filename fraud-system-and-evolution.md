### Evolução: Fraud System & Ledger Architecture

Para elevar o `payment-system` ao nível de sistemas bancários de larga escala, proponho as seguintes implementações:

---

### 1. Sistema de Fraude (Fraud System Pipeline)

Em vez de uma validação síncrona simples, um sistema de fraude moderno deve operar como um pipeline.

#### Arquitetura Sugerida:
1.  **Ingestion:** A transação chega e é marcada como `PENDING_FRAUD_ANALYSIS`.
2.  **Pipeline de Regras (Sync/Async):**
    *   **Rule Engine:** Regras estáticas (ex: "Transação > R$ 5.000,00 entre 22h e 06h").
    *   **Velocity Check:** Verifica quantas transações o usuário fez nos últimos X minutos.
    *   **Score Model (ML):** Um modelo de Machine Learning (ex: XGBoost ou Random Forest) que gera um score de 0 a 100 baseado no comportamento histórico.
3.  **Decision Maker:**
    *   `Score < 30`: Aprova automaticamente.
    *   `30 < Score < 70`: Envia para revisão manual ou pede um segundo fator (2FA).
    *   `Score > 70`: Bloqueia e notifica o time de compliance.

#### Implementação no Spring:
*   Use um **Message Broker** (Kafka ou RabbitMQ) para desacoplar a análise de fraude do fluxo principal.
*   Implemente o padrão **Chain of Responsibility** para as regras de fraude.

---

### 2. Transição para Ledger (Livro-Razão)

Mover o `balance` de `Account` para uma tabela de `LedgerEntries` é a mudança mais importante para segurança financeira.

#### Nova Estrutura de Dados:
*   **Table `ledger_entries`**:
    *   `id` (ULID)
    *   `account_id` (FK)
    *   `transaction_id` (FK)
    *   `amount` (Decimal - Positivo para crédito, Negativo para débito)
    *   `entry_type` (DEBIT, CREDIT)
    *   `created_at`

#### Fluxo de Operação:
1.  **Início:** Inicia transação de banco de dados.
2.  **Check:** Verifica se a soma atual das entries da conta origem permite o débito (ou use um saldo cacheado para performance).
3.  **Insert:** Insere uma linha de débito para a conta A.
4.  **Insert:** Insere uma linha de crédito para a conta B.
5.  **Audit:** A transação é gravada com o link para as entradas do ledger.
6.  **Commit:** Finaliza a transação.

#### Vantagens:
*   **Imutabilidade:** Você nunca altera um valor, apenas adiciona novos. Isso é regra de ouro em contabilidade.
*   **Auditoria Total:** É possível reconstruir o saldo de qualquer conta em qualquer ponto no tempo apenas somando as entradas.
*   **Consistência:** Resolve problemas de "dinheiro sumindo" por falhas de update.

---

### 3. Pipeline de Execução Recomendado

Para implementar essas mudanças sem quebrar o sistema atual:

1.  **Fase 1 (Shadow Ledger):** Comece a gravar no Ledger mas continue usando o campo `balance` da conta para lógica de negócio. Compare os valores para validar a integridade.
2.  **Fase 2 (Fraud Basic):** Implemente o Fraud System com regras estáticas antes de debitar o saldo.
3.  **Fase 3 (Migration):** Torne o Ledger a fonte da verdade e remova o campo `balance` da entidade `Account` (ou use-o apenas como cache).

Esta abordagem garante que o sistema seja resiliente, auditável e seguro contra fraudes.
