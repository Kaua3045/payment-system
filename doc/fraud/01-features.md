# Features: o que o modelo enxerga

O modelo **não sabe** o que é um Pix, uma conta ou reais: ele só recebe **13 números**. As features traduzem a tentativa
para esses números. Duas famílias: **velocity** (comportamento no tempo) e **contexto** (destinatário, aparelho, hora,
local).

## As 12 features
A ordem é **fixa** e é um contrato com o treino em Python: o parser do modelo recusa um arquivo cujos nomes ou ordem
divirjam de `FeatureVector.NAMES`.

| Idx | Nome | Fórmula | Fonte | Por que existe |
|---|---|---|---|---|
| 0 | `log_amount` | `ln(1 + valor em reais)` | `Attempt` | valor absoluto maior = mais risco |
| 1 | `amount_ratio_log` | `ln(1 + min(valor ÷ média da conta, 50))`; sem histórico a razão é 1 | `Attempt` + `Stats` | o valor é alto **para esta conta**? |
| 2 | `count_1m_log` | `ln(1 + tentativas anteriores no último minuto)` | `Stats` | rajadas |
| 3 | `count_1h_log` | idem, na última hora | `Stats` | |
| 4 | `count_24h_log` | idem, nas últimas 24 h | `Stats` | |
| 5 | `distinct_payees_log` | `ln(1 + destinatários distintos em 24 h)` | `Stats` | pagar muita gente diferente |
| 6 | `new_payee` | 1 se o destino nunca foi pago e aprovado antes | `Stats` | o golpe típico paga um destinatário novo |
| 8 | `night` | 1 se a hora local (America/Sao_Paulo) é ≥ 20 ou < 6 | `Attempt.at` | golpes concentram-se à noite |
| 9 | `counterparty_new` | 1 se a conta de destino tem **menos de 7 dias**; idade desconhecida: 0 | `Attempt` | conta "laranja" recém-aberta |
| 10 | `instrument_recent` | 1 se a chave Pix ou o cartão foi cadastrado há **menos de 24 h**; desconhecido ou inexistente: 0 | `ChannelDetails` | chave Pix recém-criada |
| 11 | `country_changed` | 1 se o país difere do da última localização **confiável** | `Attempt` + `Stats` | |
| 12 | `speed_scaled` | `min((km ÷ horas) ÷ 1000, 1)`, com horas = `max(tempo desde a última localização, 1/60)` | `Attempt` + `Stats` | "viagem impossível": 1,0 = 1000 km/h ou mais |

Os nomes são **neutros de canal** (`counterparty_new`, `instrument_recent`): valem para Pix, transferência e cartão
([decisão 0008](../decisions/0008-multicanal-envelope-e-pipeline-por-canal.md)).

## Detalhes que importam

- **Por que `ln(1 + x)`?** Contagens e valores têm cauda longa (quase todos pequenos, poucos enormes). O log comprime a
  escala, e o `1 +` faz o zero continuar zero (`ln 1 = 0`).
- **A tentativa não conta a si mesma.** O `StatsStore.load` ignora um evento com o mesmo `id`, então um retry não vira
  histórico dele mesmo.
- **Negadas contam na velocity**, mas só **aprovadas** ensinam confiança (destinatário, aparelho, média, localização).
  Ver [decisão 0006](../decisions/0006-estado-e-aprendizado-de-confianca.md).
- **Dado ausente não penaliza:** idade desconhecida vira 9999 (nunca "recente"); sem localização, as features de geo são 0.
- **Piso de 1 minuto na velocidade:** com tempo ~0, qualquer ruído de GPS ou IP viraria velocidade infinita.
- **`double` só aqui.** O valor real continua `BigDecimal` no `Attempt` ([decisão 0007](../decisions/0007-dinheiro-com-bigdecimal.md)).

## Exemplo completo

Maria (média histórica R$150, em São Paulo) paga **R$900 a um destinatário novo**, às 13h (Brasília).
Nas últimas 24 h ela fez 5 tentativas (2 no último minuto, 3 na última hora) para 2 destinatários.

| Idx | Feature | Valor | Conta |
|---|---|---|---|
| 0 | `log_amount` | 6,803505 | ln(1 + 900) |
| 1 | `amount_ratio_log` | 1,945910 | 900 ÷ 150 = 6 → ln(1 + 6) |
| 2 | `count_1m_log` | 1,098612 | ln(1 + 2) |
| 3 | `count_1h_log` | 1,386294 | ln(1 + 3) |
| 4 | `count_24h_log` | 1,791759 | ln(1 + 5) |
| 5 | `distinct_payees_log` | 1,098612 | ln(1 + 2) |
| 6 | `new_payee` | **1** | destino nunca pago |
| 8 | `night` | 0 | 13h |
| 9 a 12 | demais | 0 | sem outros sinais |

Esse vetor, passado pelo modelo de exemplo, dá score **0,378 → REVIEW** (ver [03](03-modelo-e-treino.md)).

## Como mudar ou criar uma feature

Mudar o **significado** de uma feature, ou criar uma nova, obriga a **retreinar** o modelo, porque o treino precisa produzir
exatamente os mesmos números.
1. Constante e nome no **fim** de `FeatureVector` (e em `NAMES`).
2. O `FeatureExtractor` passa a preenchê-la.
3. Retreinar com o mesmo nome e a mesma ordem.

O parser recusa o modelo antigo (os nomes não batem), então a troca nunca passa despercebida.

## Estado

O exemplo numérico foi calculado executando o `FeatureExtractor`. A ordem e os nomes são verificados pelo parser do
modelo (testado, inclusive com um arquivo de features trocadas).