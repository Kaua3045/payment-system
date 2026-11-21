# Revisão manual (REVIEW)

> **Status: proposta desenhada mas não implementada.** Hoje `REVIEW` devolve uma exception `InternalErrorException`. A ideia é reter o saldo e expirar a transação, para que um analista ou autenticação extra decida. O cliente deve **tratar** o `MANUAL_REVIEW` como um veredito válido, não como erro.
> 
> Decisão: [0011](../decisions/0011-revisao-manual-reter-saldo-e-expirar.md).

## O que acontece no REVIEW, após implementação

Você cria a **reserva** (o saldo fica bloqueado), mas **não toca no ledger** (nenhum débito nem crédito). A transação fica
`IN_REVIEW` até o analista decidir. Aprovar liquida usando a reserva que já existe; negar libera a reserva; se o prazo estourar,
a transação **expira** e o saldo volta.

```
CreateTransaction → FraudService.assess → REVIEW
  [1 transação do banco]  insere a transação IN_REVIEW (com prazo) + RESERVA o saldo + abre o caso + outbox
  resposta ao cliente: 202 UNDER_REVIEW

Depois, por um de três caminhos (cada um numa transação do banco):
  analista APROVA → caso OPEN→APPROVED, transação IN_REVIEW→COMPLETED, liquida com a reserva existente
  analista NEGA   → caso OPEN→DENIED,   transação IN_REVIEW→DENIED,    libera a reserva
  prazo ESTOURA   → caso OPEN→EXPIRED,  transação IN_REVIEW→EXPIRED,   libera a reserva
```

## Por que reservar o saldo

| Opção | Consequência |
|---|---|
| **Reservar** | o dinheiro não pode ser gasto em outro lugar durante a análise e a aprovação quase sempre liquida sem surpresa. Custo: o cliente vê o dinheiro retido |
| Não reservar | o cliente pode gastar o saldo, e a aprovação falha por saldo insuficiente |

Em Pix existem regras sobre retenção de valores e prazos de análise: **combine os números com compliance**.

## Dados

```sql
ALTER TABLE transactions ADD COLUMN review_deadline timestamptz;
CREATE INDEX idx_tx_review_overdue ON transactions (review_deadline) WHERE status = 'IN_REVIEW';

CREATE TABLE review_cases (
  transaction_id uuid PRIMARY KEY REFERENCES transactions(id),
  status         text NOT NULL CHECK (status IN ('OPEN','APPROVED','DENIED','EXPIRED')),
  score          double precision,
  reasons        text[] NOT NULL DEFAULT '{}',
  opened_at      timestamptz NOT NULL,
  deadline       timestamptz NOT NULL,
  decision       text CHECK (decision IN ('APPROVE','DENY')),
  decided_by     text,
  note           text,
  decided_at     timestamptz,
  CHECK (status = 'OPEN' OR decided_at IS NOT NULL));
CREATE INDEX idx_review_cases_open ON review_cases (deadline) WHERE status = 'OPEN';
```
(Os tipos de id devem seguir os das suas tabelas: o seu DDL usa `VARCHAR(26)` para ULID.)

## As peças (`payments.review`)

| Classe | O que faz |
|---|---|
| `HoldForReview.open(pending, now)` | numa transação: insere a transação `IN_REVIEW` com prazo, reserva o saldo, abre o caso, grava o evento no outbox |
| `ResolveReviewUseCase` | o analista decide: aprovar liquida, negar libera a reserva. Idempotente |
| `ExpireReviewsUseCase.run(batch)` | encerra as revisões vencidas, **cada item na sua transação** |
| `ReviewExpiryJob` | `@Scheduled` que chama o anterior |
| `ReviewPolicy` | prazos (padrão: 30 min de revisão, 10 min de folga na reserva, 5 min de reserva refeita). **Placeholders** |
| `ReviewCaseRepository` | `insert`, `find`, `resolve` (update condicional), `findOpen` (a fila) |

## As decisões que valem entender

- **Quem chega primeiro vence, sem lock.** Aprovar, negar e expirar fazem o mesmo primeiro passo: `UPDATE ... WHERE status =
  'OPEN'` no caso. Só quem recebe 1 linha prossegue. Como tudo é uma transação do banco, quem perde desfaz o que fez.
- **Decisão repetida é idempotente** (devolve o estado atual, `alreadyResolved = true`); **decisão contrária é conflito** (409).
- **A reserva vale até o prazo + folga**, para a expiração da revisão agir **antes** do job genérico de expiração de reservas.
- **Reserva perdida mesmo assim:** a aprovação tenta reservar de novo. Com saldo, liquida; sem saldo, a transação vai para
  `FAILED` (`INSUFFICIENT_FUNDS`) e a **decisão do analista fica registrada**.
- **Expiração é fail-closed:** uma transação suspeita que ninguém aprovou **não sai**.
- **Dado inconsistente** (caso aberto, transação fora de `IN_REVIEW`) lança e desfaz tudo, em vez de "consertar" às cegas.
- **Auditoria:** `decided_by`, `decided_at`, `note`, `score` e `reasons` ficam no caso. Negar exige justificativa.

## O que a proposta **não** resolve

1. **Quatro olhos** para valores altos (um segundo analista aprova).
2. **O antifraude não aprende com a decisão do analista:** uma tentativa em REVIEW não ensina confiança, então a próxima
   parecida cai em REVIEW de novo. O remédio é consumir o evento de resultado (e é o que gera o rótulo de treino).
3. **Autenticação extra (step-up)** para a faixa de risco médio, resolvendo na hora sem humano.
4. **Como microsserviço**, o caso e a fila pertencem ao módulo de risco; o pagamento continua dono da retenção e da liquidação.

## Estado
