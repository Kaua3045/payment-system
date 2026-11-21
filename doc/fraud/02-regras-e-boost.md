# Regras e boost

## Duas famílias de regras

| | Regra **dura** | Regra **suave** (boost) |
|---|---|---|
| O que faz | decide na hora e **encerra** a avaliação (o modelo nem roda) | **soma** um valor ao logit do modelo |
| Resultado | `RuleResult.Deny(motivo)` → DENY, score 1,0 | `RuleResult.Boost(motivo, logit)` |
| Quando usar | o determinístico e barato: blocklist, limites | evidência que não condena sozinha: rajada, viagem rápida |

Decisão: [0004](../decisions/0004-regras-duras-e-boost-no-logit.md).

## Como o boost entra no score

O score final **não** é uma média nem uma soma de probabilidades (0,7 + 0,6 daria 1,3). É uma soma de **logits**:

```
logit_final = logit do modelo + soma dos boosts das regras suaves que dispararam
score       = sigmoid(logit_final)
veredito    = Verdict.of(score, reviewAt, denyAt)
```

No espaço do logit as evidências **acumulam de forma coerente**, e a sigmoid devolve sempre algo entre 0 e 1.
**+1 no logit multiplica as odds por e ≈ 2,7; +2 por ≈ 7,4; +3 por ≈ 20.** O mesmo boost pesa muito num caso de risco
baixo e quase nada num caso que já era quase certo.

### Exemplo: viagem impossível

Cliente com histórico limpo (destinatário e aparelho conhecidos, valor normal) aparece em Manaus 10 minutos depois de São Paulo.
- **Só o modelo** (modelo de exemplo): logit = −3,0 − 0,3 − 0,1 − 0,1 + 1,5 = **−2,0** → score 0,119 → `APPROVE`.
- A regra `IMPOSSIBLE_TRAVEL` dispara (`speed_scaled` = 1,0 ≥ 0,9) e soma **+3,0**.
- logit final = −2,0 + 3,0 = **+1,0** → score **0,731** → `REVIEW`, com o motivo `IMPOSSIBLE_TRAVEL`.

## As regras do módulo (em ordem: duras e baratas primeiro)

| # | Regra | Tipo | Parâmetro (padrão) | Dispara quando | Motivo |
|---|---|---|---|---|---|
| 1 | `blocklist` | dura | arquivo ou memória | origem, destino, instrumento ou aparelho na lista | `BLOCKLIST_ORIGIN`, `_DESTINATION`, `_INSTRUMENT`, `_DEVICE` |
| 2 | `amountLimit` | dura | R$ 5.000,00 | valor **maior que** o limite | `AMOUNT_LIMIT` |
| 3 | `nightAmountLimit` | dura | R$ 1.000,00, só Pix e transferência | `night` = 1 e valor acima do limite | `NIGHT_LIMIT` |
| 4 | velocity | suave | 3 tentativas/min, boost **+2,0** | `count_1m_log` ≥ ln(1 + 3) = 1,3863 | `VELOCITY_1M` |
| 5 | viagem impossível | suave | 900 km/h, boost **+3,0** | `speed_scaled` ≥ 0,9 | `IMPOSSIBLE_TRAVEL` |

O limite noturno é uma regra de transferência/Pix, não de cartão. **Confirme os valores e a janela com compliance.**

## Do score ao veredito

`score < reviewAt` → APPROVE; `reviewAt ≤ score < denyAt` → REVIEW; `score ≥ denyAt` → DENY. Os padrões **0,30 e 0,80 são
pontos de partida**. Limiar baixo pega mais fraude e bloqueia mais gente legítima; limiar alto faz o contrário. A escolha
vem do custo de cada erro para o negócio, e o relatório do script de treino ajuda (ver [03](03-modelo-e-treino.md)).

## Como criar uma regra

- **Sobre uma feature:** uma linha em `FraudModule.rules(...)`:
  ```java
  Rules.featureAtLeast(FeatureVector.COUNT_1H_LOG, Math.log1p(10), "VELOCITY_1H", 1.0)   // >= 10 tentativas em 1 h: +1,0
  ```
- **Com lógica própria:** implemente `Rules.Rule` (`check(Attempt, FeatureVector) → RuleResult`), **sem I/O**.

## Armadilhas

- `BigDecimal.equals` considera a escala (5000 ≠ 5000.00): as regras usam `compareTo`.
- O limiar das regras suaves é em **escala de feature** (`ln(1 + 3)`), não em contagem.
- Uma regra suave com logit grande demais vira uma regra dura disfarçada: +10 já leva quase qualquer caso a DENY.

## Estado

Cada regra tem teste. O exemplo da viagem impossível foi executado de ponta a ponta (score 0,731, `REVIEW`).