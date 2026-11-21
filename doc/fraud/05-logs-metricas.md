# Logs, métricas

Decisão: [0010](../decisions/0010-logs-com-slf4j-direto.md).

## Logs

O módulo usa **SLF4J direto**, sem classes de log próprias. O Logback exporta, e o agent do OpenTelemetry captura os logs,
junta o `trace_id` e os envia ao Elastic. Uma linha **chave=valor** por decisão relevante; o **nível** controla o volume.

| Evento | Nível | Onde |
|---|---|---|
| `fraud.startup` (modelo, `sha256`, parâmetros) | INFO | `FraudModule.create` |
| `fraud.decision` degradada | **WARN** | `AssessFraudUseCase` |
| `fraud.decision` REVIEW ou DENY | INFO | `AssessFraudUseCase` |
| `fraud.decision` APPROVE | **DEBUG** (desligado em produção) | `AssessFraudUseCase` |
| `fraud.stage_failure step=... attempt=...` (com stack) | WARN | `FraudPipeline` |
| `fraud.record_failure`, `fraud.fallback`, `fraud.unsupported_channel` | WARN | use case e `FallbackFraudService` |

Linhas reais (geradas pelos testes):
```
INFO  Decision attempt=att-1 channel=PIX verdict=DENY score=1.0000 degraded=false path=enrich>rules reasons=[BLOCKLIST_DESTINATION] us=5950
WARN  Step=enrich attempt=att-4   (+ stack trace)
WARN  fDecision attempt=att-4 channel=PIX verdict=APPROVE score=0.2497 degraded=true path=enrich!>rules>score reasons=[FALLBACK:enrich:IllegalStateException] us=5668
INFO  fraud.startup model=dev-model.tree sha256=663865bf7b7e trees=4 nodes=16 base=-3.0000 channels=[PIX] reviewAt=0.3 denyAt=0.8 amountLimit=5000.00 nightLimit=1000.00 failOpenBelow=200.00
```
(O `us=5950` é a primeira chamada, com a JVM fria.)

Como ler: **`path`** mostra por onde a decisão passou (`enrich>rules` = uma regra dura decidiu e o modelo nem rodou).
**`reasons`** são os motivos (ver [08](08-operacao.md)). **`sha256`** é a impressão digital do modelo: "qual modelo decidiu isto?"
é a primeira pergunta de uma auditoria.

### O que **não** logar

| Não logar | Por quê |
|---|---|
| conta, aparelho, chave Pix, token de cartão, localização | dado pessoal (LGPD) |
| valor exato | sensível |
| conteúdo da blocklist | revela investigações |
| o `FeatureVector` de cada decisão **no log** | é um dataset, não um log: grave-o numa tabela ou evento, com retenção e acesso próprios |

Cuidado: o log de falha inclui a **mensagem da exceção**. Se o seu cliente de Redis colocar o nome da chave na mensagem
(e a chave contém a conta), isso vaza dado.

### Falha repetida inundando o log

Uma queda do Redis a 500 TPS gera ~500 WARN com stack por segundo. Opções: contar por **métrica**; um limite por taxa no
framework (o `BurstFilter` do Log4j2); e **cuidado** com o `DuplicateMessageFilter` do Logback, que pela documentação não
tem janela de tempo e esconderia a **próxima** queda. Não foram testados aqui: confirme na sua versão.

## Métricas (DEFINIR AINDA MELHOR)

 - `fraud.decisions` não implementada ainda
 - `fraud.latency` não implementada ainda
 - `fraud.stage.failures` não implementada ainda
 - `fraud.rule.triggered` não implementada ainda
 - `fraud.blocklist.size` não implementada ainda

| Métrica | Tipo | Tags (poucas!) |
|---|---|---|
| `fraud.decisions` | contador | `channel`, `verdict`, `degraded` |
| `fraud.latency` | histograma (ms) | `channel` |
| `fraud.stage.failures` | contador | `step` |
| `fraud.rule.triggered` | contador | `rule` |
| `fraud.blocklist.size` | gauge | |
| `fraud.recors` | contador | `outcome` |

**Nunca** use conta, valor ou `attempt` como tag: a cardinalidade explode o custo. Alertas sugeridos: falhas de
etapa acima de 1% do tráfego, qualquer `unsupported_channel`, `sha256` mudando sem deploy, p99 acima de 40 ms.