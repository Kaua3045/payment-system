# Antifraude: visão geral

## O que é

O antifraude recebe uma **tentativa** de movimentar dinheiro (`Attempt`) e devolve uma **avaliação** (`Assessment`):
um **veredito** (`APPROVE`, `REVIEW` ou `DENY`), um **score** (probabilidade de fraude, de 0 a 1) e os **motivos**.
Tudo acontece em memória, em microssegundos, dentro de um orçamento de 50 ms.

## O fluxo de uma chamada

```
CreateTransaction
  1. checa a idempotência, lê contas e chave Pix
  2. FraudService.assess(Attempt)          ← ANTES de qualquer efeito (decisão 0001)
       └─ FraudGatewayImpl             se QUALQUER coisa falhar: valor pequeno aprova, o resto vai para análise
                 └─ AssessFraudUseCase
                      1. escolhe o pipeline do CANAL (hoje só Pix)
                      2. FraudPipeline:
                           EnrichStep → histórico da conta (StatsStore) + 13 features
                           RulesStep  → regras DURAS (negam e encerram) e SUAVES (somam "boost")
                           ScoreStep  → modelo de árvores (logit) + boost → sigmoid → score → veredito
                      3. registra a tentativa no StatsStore (alimenta a próxima decisão)
                      4. loga a decisão
  3. segue conforme o veredito:
       APPROVE → reserva saldo, escreve no ledger
       DENY    → grava a transação como negada, com código público genérico (DECLINED)
       REVIEW  → (proposta) retém o saldo e manda para um analista  (ver 06-revisao-manual.md)
```

## Os três vereditos

| Veredito | Regra | Significado |
|---|---|---|
| `APPROVE` | score < `reviewAt` (padrão 0,30) | segue o pagamento |
| `REVIEW` | `reviewAt` ≤ score < `denyAt` (padrão 0,80) | zona cinzenta: exige análise ou autenticação extra |
| `DENY` | score ≥ `denyAt`, ou uma regra dura disparou | nega |

## As peças, em uma frase cada

| Peça | O que faz | Documento |
|---|---|---|
| **Features** | traduz a tentativa em 13 números | [01](01-features.md) |
| **Regras duras** | negam na hora (blocklist, limites) | [02](02-regras-e-boost.md) |
| **Regras suaves (boost)** | somam risco ao modelo (rajada, viagem impossível) | [02](02-regras-e-boost.md) |
| **Modelo de árvores** | dá o score a partir das features | [03](03-modelo-e-treino.md) |
| **StatsStore** | a memória sobre cada conta (velocity, destinatários conhecidos) | [04](04-estado-pipeline-e-falhas.md) |
| **Pipeline** | encadeia as etapas e aplica a política de falha de cada uma | [04](04-estado-pipeline-e-falhas.md) |

## Glossário

| Termo | Significado |
|---|---|
| **Feature** | um número que descreve a tentativa (ex.: `new_payee` = 1) |
| **Logit** | o score bruto, de −∞ a +∞. É onde as evidências **somam** |
| **Sigmoid** | converte o logit em probabilidade (0 a 1) |
| **Score** | essa probabilidade de fraude |
| **Odds** | p ÷ (1 − p). Somar +1 ao logit multiplica as odds por e ≈ 2,7 |
| **Regra dura** | nega na hora e encerra a avaliação |
| **Regra suave / boost** | soma um valor ao logit do modelo |
| **Degradado** | alguma etapa falhou e aplicou a sua política de falha |
| **Fail-open** | na falha, aprovar (só valor pequeno) |
| **Fail-closed** | na falha ou no prazo vencido, não deixar passar |

## Limites atuais (o que **não** está pronto)

- O estado (`InMemoryStatsStore`) só funciona com **uma instância** e a memória só cresce. Produção pede Redis.
- Só o canal **Pix** tem pipeline. Transferência e cartão voltam `REVIEW` degradado.
- **Não há modelo treinado com dados reais.** Os limiares 0,30 e 0,80 são pontos de partida.
- O fluxo de **revisão manual** está desenhado mas não foi implementado. Hoje `REVIEW` devolve uma exception `InternalErrorException`
- Hoje o antifraude roda **dentro** da aplicação. O serviço HTTP e os eventos Kafka são futuros.

## Em 2 minutos

Antes de mexer em dinheiro, o pagamento pergunta ao antifraude. Ele transforma a tentativa em 13 números (valor, quanto
foge do normal da conta, velocidade de tentativas, destinatário e aparelho novos, horário, viagem impossível...). Regras
simples negam o óbvio (conta bloqueada, limite estourado). Um conjunto de 40 árvores de decisão, treinado offline, dá um
score de risco, e regras suaves somam risco extra em casos como "apareceu em outra cidade 10 minutos depois". O score vira
aprovar, analisar ou negar. Se o antifraude falhar, o pagamento não cai: valor pequeno passa, o resto vai para análise.
Mais em [09-como-explicar.md](09-como-explicar.md).