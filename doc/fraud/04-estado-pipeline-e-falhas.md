# Estado, pipeline e falhas

Decisões: [0005](../decisions/0005-pipeline-leve-em-vez-de-maquina-generica.md),
[0006](../decisions/0006-estado-e-aprendizado-de-confianca.md),
[0002](../decisions/0002-contrato-fraudservice-e-fallback.md).

## O estado: `StatsStore`

É a **memória** do antifraude sobre cada conta de origem. Sem ele não existe velocity, destinatário novo nem viagem impossível.

| Método | Quando | O que faz |
|---|---|---|
| `load(attempt)` | antes de decidir | devolve o `Stats`: contadores por janela, valores, perfil e última localização confiável |
| `record(attempt, resultado)` | depois de decidir | alimenta o histórico. **Idempotente** por `attempt.id` |

Regras do `InMemoryStatsStore`:
1. **Velocity conta tudo**, inclusive tentativas negadas (quem tenta 20 vezes é suspeito).
2. **Confiança só se aprende com aprovadas:** destinatário, aparelho, valor médio **e localização**. Senão o fraudador
   "treina" o perfil da vítima, e uma tentativa suspeita limparia a viagem impossível.
3. **O retry não conta a si mesmo** no `load`.
4. **Janela deslizante de 24 h**, relativa à hora da tentativa.
5. Cada conta é um bloco sincronizado.

**Limites:** só funciona com **uma instância**, **nunca esquece contas** (a memória cresce) e perde tudo ao reiniciar. Em
produção: Redis (um ZSET de eventos por conta, um HASH de perfil, SETs de destinatários e aparelhos, TTL, e um script Lua
que lê e grava de forma atômica em um round-trip).

## O pipeline

Uma **lista de etapas** (`FraudStep`) executadas em ordem sobre um `FraudCase` (um por chamada, nunca compartilhado):

```
EnrichStep  →  RulesStep  →  ScoreStep
```

- Quando uma etapa **decide** (uma regra dura nega), as seguintes são **puladas**.
- O `path` registra o caminho (`enrich>rules>score`); um `!` marca uma etapa que falhou (`enrich!>rules>score`).
- Há **um pipeline por canal** (`Map<Channel, FraudPipeline>`); hoje só o Pix.

## Política de falha (cada etapa traz a sua)

| Etapa | Se falhar | Degradado |
|---|---|---|
| `enrich` | segue sem histórico (`Stats.empty()`); as regras duras ainda protegem | sim |
| `rules` | `REVIEW` (não dá para saber se uma regra dura negaria) | sim |
| `score` | até R$ 200 e sem sinal suave **aprova** (fail-open); senão `REVIEW` | sim |

O score `NaN` ou infinito é tratado como falha: `NaN` é falso em todo `if (score >= x)` e viraria `APPROVE` em silêncio.

## Outras proteções

- **Canal sem pipeline** → `REVIEW` degradado, motivo `UNSUPPORTED_CHANNEL:<canal>`. É erro de configuração: não pode
  aprovar às cegas nem cair no fail-open.
- **`FallbackFraudService`** (lado de pagamentos): se **qualquer** `RuntimeException` escapar do antifraude, valor ≤
  `failOpenBelow` aprova, o resto vai para `REVIEW`, sempre `degraded = true`. O **timeout** fica no cliente (HTTP/Redis),
  não em threads.
- **Falhar em registrar** no `StatsStore` nunca muda uma decisão já tomada: é logado, e a decisão segue.
- **Retry com a mesma `Idempotency-Key`** é seguro: o `Attempt.id` é a chave, e o `StatsStore` a ignora.

## Duas brechas que já foram corrigidas (com teste)

1. **O retry contava a si mesmo** na velocity. Agora o `load` ignora o evento com o mesmo `id`.
2. **`lastSeen` era atualizado por tentativas não aprovadas**, o que "limpava" uma viagem impossível (a 1ª tentativa de
   Manaus passava a ser o "último local" e a 2ª já não parecia suspeita). Agora só `APPROVE` move a localização.

## Estado

Pipeline, políticas de falha, canal sem pipeline, idempotência e as duas brechas têm teste (suíte de 32 testes que passou
antes da simplificação dos logs; **não foi reexecutada** depois do reinício do ambiente). O `InMemoryStatsStore` não foi
testado sob concorrência real nem com carga de longa duração (a memória cresce).