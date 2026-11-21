# Referência: classe por classe

## `frauds` (Pastas dentro de cada modulo)
| Classe | O que é / métodos |
|---|---|
| `Attempt` (record) | uma tentativa em qualquer canal: `id` (chave de idempotência), `origin`, `destination`, `amount` (BigDecimal), `at`, `deviceId?`, `location?`, `destinationCreatedAt?`, `details`. O construtor valida (nulos, `id` vazio, `amount` > 0) e normaliza para **escala 2**, **recusando** o que perderia precisão. `channel()`; fábricas `pix(...)`, `transfer(...)`, `card(...)` |
| `ChannelDetails` (selada) | dados do canal: `Pix(key, keyCreatedAt)`, `AccountTransfer()`, `CardPayment(cardToken, cardAddedAt)`. Acesso neutro: `channel()`, `blockKey()` (`key:...`, `card:...` ou `null`), `instrumentCreatedAt()` |
| `Channel` (enum) / `GeoPoint` (record) | `PIX`, `TRANSFER`, `CARD` / `lat`, `lon`, `countryCode` |
| `Assessment` (record) | `verdict`, `score` (NaN quando só houve fallback), `reasons`, `degraded`. `Assessment.fallback(verdict, why)` |
| `Verdict` (enum) | `of(score, reviewAt, denyAt)` |
| `Stats` (record) | histórico cru da conta (contadores, valores, perfil, `lastSeen`). `Stats.empty()` |
| `StatsStore`, `FraudService` | as duas portas: `load`/`record` e `assess` |

## `frauds.features`
| Classe | Métodos |
|---|---|
| `FeatureVector` | constantes dos índices 0 a 11; `NAMES`, `SIZE`; `of(double...)` recusa tamanho errado e NaN/Infinito; `builder()` (`set`, `build`); `get(i)` |
| `FeatureExtractor` | `extract(attempt, stats)` → as 12 features ([01](01-features.md)) |
| `GeoUtils` | `haversineKm(a, b)`: distância na esfera, erro ~0,5% |

## `frauds.rules`
| Classe | Membro |
|---|---|
| `BlockList` | `contains(key)`: só memória. Chaves `acc:`, `key:`, `card:`, `dev:` |
| `RuleResult` | `Pass`, `Deny(reason)`, `Boost(reason, logit)` (recusa logit NaN/Infinito) |
| `Rules` | `amountLimit(max)`, `blocklist(list)`, `nightAmountLimit(max, canais)`, `featureAtLeast(i, limiar, motivo, logit)` |

## `frauds.model`
| Classe | Membro |
|---|---|
| `Sigmoid` | `exp(x)`: escreve `x = k·ln2 + r`, série de Taylor em Horner (14 termos) para `e^r`, `2^k` montado nos bits do expoente, satura em ±700. `sigmoid(z)`: dois ramos (z ≥ 0 e z < 0), NaN propaga |
| `TreeModel` | `rawScore(x)` = logit (BASE + folhas), `score(x)` = sigmoid, `treeCount()`, `nodeCount()`, `baseScore()`. Construtor package-private: só o parser cria. Percorre as árvores de forma **iterativa**, sem alocação |
| `TreeModelParser` | `parse(Path)`, `parse(Reader)`; validação agressiva ([03](03-modelo-e-treino.md)); `ModelFormatException` com o número da linha |

## `frauds.pipeline`
| Classe | Membro |
|---|---|
| `FraudCase` | o caso: `attempt`, `stats`, `features`, `boostLogit`, `score`, `verdict`, `degraded`, `reasons`, `path`. `toAssessment()` lança se ninguém decidiu |
| `FraudStep` | `name()`, `run(c)`, `onFailure(c, e)` |
| `FraudPipeline` | `run(attempt)`: executa as etapas, pula as seguintes quando alguém decide, em falha loga, marca `!`, registra `FALLBACK:<etapa>:<exceção>` e chama `onFailure` |
| `EnrichStep` | `stats.load` + `extract`. Falha: `Stats.empty()` e degradado |
| `RulesStep` | `Deny` → motivo + DENY (1,0) e encerra; `Boost` → soma em `boostLogit` e anota. Falha: REVIEW degradado |
| `ScoreStep` | `raw = rawScore`; recusa NaN/Infinito; `score = sigmoid(raw + boostLogit)`. Falha: aprova até `failOpenBelow` (sem boost), senão REVIEW |

## `frauds.usecase`
`AssessFraudUseCase.doExecute`: escolhe o pipeline do canal (sem pipeline: REVIEW degradado e log), executa, **registra** no
`StatsStore` (sempre, inclusive quando uma regra dura encerrou cedo), loga a decisão e coloca as `tag`s
(`fraud.path`, `verdict`, `score`, `degraded`, `reasons`). `recordQuietly` engole (e loga) a falha de registro.

## `frauds.infra`

| Classe | Membro |
|---|---|
| `InMemoryStatsStore` | `load`, `record` ([04](04-estado-pipeline-e-falhas.md)) |
| `InMemoryBlockList` | `block`, `unblock`, `contains`: dev e testes |
| `BlockKeys` | `require(chave)` valida `tipo:valor` (`acc`, `key`, `card`, `dev`) |

## `frauds.client`

| Classe | Membro |
|---|---|
| `FraudGatewayImpl` | em qualquer `RuntimeException`: loga; valor ≤ `failOpenBelow` → APPROVE, senão REVIEW; sempre `degraded` |

## `frauds.config`

| Classe | Membro |
|---|---|
| `FraudSettings` | limiares, limites (`BigDecimal`), velocity, viagem, `failOpenBelow`. `defaults(...)` |
| `FraudModule` | `create(settings, blocklist, stats)`: lê o modelo (inválido: **não sobe**), monta o pipeline do Pix, loga `fraud.startup` com o `sha256`. `rules(...)` é a **lista e a ordem das regras** |
| `FraudConfiguration` | beans Spring: `FraudSettings` (de `@Value`), `StatsStore`, `BlockList` (arquivo ou memória), `FraudModule`, `FraudService` |




