# O modelo de árvores e o treino

Decisões: [0003](../decisions/0003-modelo-de-arvores-in-process.md).

## A ideia

O modelo é um **conjunto de árvores de decisão** treinado por *gradient boosting*: dezenas de árvores minúsculas de
"se/senão". Cada árvore olha poucas features, cai em uma **folha** e devolve um número pequeno. O logit é a soma de todos eles:

```
logit = BASE + folha(árvore 0) + folha(árvore 1) + ... + folha(árvore N-1)
score = sigmoid(logit)
```

**Boosting:** a árvore 0 faz uma estimativa grosseira, a árvore 1 é treinada para corrigir o erro que sobrou, a árvore 2
corrige o que ainda sobrou, e assim por diante.

Por que árvores e não uma soma de pesos simples: elas capturam **interações**. "Valor alto **e** aparelho novo **e**
destinatário novo" é muito mais perigoso do que a soma de cada sinal, e uma regressão linear não representa isso.

## O arquivo `model.tree`

```
TREEMODEL 1                            versão do formato
FEATURES log_amount,...,speed_scaled   os 13 nomes, NESTA ORDEM (contrato com o treino)
BASE -3.7018571619297052               ponto de partida do logit
TREES 40                               quantas árvores
TREE 15                                a árvore 0 tem 15 nós
0 S 1 1.2714597582817078 1 8           nó 0 = corte (Split)
3 L -0.0320...                         nó 3 = folha (Leaf)
END
```


| Elemento | Significado |
|---|---|
| `BASE` | o logit da **taxa de fraude do treino**: `ln(p ÷ (1 − p))`. Com 2,41% de fraude, −3,70 |
| `i S f t l r` | o nó `i` olha a feature `f` com limiar `t`; se `valor <= t` vai para o nó `l`, senão para `r` |
| `i L v` | o nó `i` é uma folha que soma `v` ao logit. **A learning rate já está multiplicada dentro dela** |
| features binárias | limiar 0,5: "`<= 0.5`" significa "a flag é falsa" |

O parser **valida tudo** (o arquivo é entrada não confiável): versão, nomes e ordem das features, limites (10.000 árvores,
100.000 nós por árvore, 2.000.000 no total, profundidade 64), números finitos (`NaN` e `Infinity` são recusados), nós
sequenciais, filho sempre depois do pai (sem ciclo), exatamente um pai por nó. **Modelo inválido = o serviço não sobe.**

## O modelo de exemplo (`ml/dev-model.tree`): 4 árvores, feito à mão

`BASE = −3,0` (risco de 4,7% sem nenhum sinal). Cada árvore testa uma ideia:

| Árvore | Pergunta | Folhas |
|---|---|---|
| 0 | o destinatário é novo? | não −0,3 · sim +1,2 |
| 1 | o aparelho é novo? (se sim, `log_amount` > 6?) | conhecido −0,1 · novo+pequeno +0,8 · novo+alto +2,0 |
| 2 | o valor está muito acima do normal da conta? (`amount_ratio_log` > 1,3) | não −0,1 · sim +1,4 |
| 3 | a conta de destino é recente? Se não, `speed_scaled` > 0,6? | recente +1,0 · antiga sem viagem 0,0 · antiga com viagem +1,5 |

Passo a passo com o vetor de [01](01-features.md) (R$900, destinatário novo):

| Etapa | Caminho | Folha | Soma |
|---|---|---|---|
| partida | | | −3,0 |
| árvore 0 | `new_payee` 1 > 0,5 | **+1,2** | −1,8 |
| árvore 2 | `amount_ratio_log` 1,946 > 1,3 | **+1,4** | −0,5 |
| árvore 3 | `counterparty_new` 0 → `speed_scaled` 0 ≤ 0,6 | 0,0 | **−0,5** |

logit = −0,4 → `sigmoid(−0,4)` = **0,4013** → entre 0,30 e 0,80 → **REVIEW**.

Os cinco cenários de referência (histórico de 6 pagamentos aprovados de R$150):

| Cenário | Veredito | Score |
|---|---|---|
| A. conhecido, R$150 | APPROVE | 0,029 |
| B. destinatário novo, R$150 | APPROVE | 0,119 |
| C. destinatário novo, R$900 | REVIEW | 0,378 |
| D. destinatário novo, R$900 | DENY | 0,832 |
| E. conhecido, R$150, Manaus 10 min depois de SP | REVIEW | 0,731 (modelo 0,119 + boost +3,0) |

## Treino (`ml/train_and_export.py`)

```bash
pip install numpy scikit-learn
python3 train_and_export.py                      # dados SINTÉTICOS: só para ver o fluxo funcionar
python3 train_and_export.py --csv decisoes.csv   # dados REAIS
```

| Parâmetro | Valor | O que faz |
|---|---|---|
| `N_TREES` | 40 | quantas árvores. Mais = mais capacidade, arquivo maior, risco de decorar o treino |
| `MAX_DEPTH` | 3 | profundidade de cada árvore: até 7 cortes e 8 folhas (15 nós) |
| `LEARNING_RATE` | 0,1 | quanto cada árvore corrige das anteriores |

**Dados reais:** o serviço deve **gravar o `FeatureVector` junto de cada decisão**, e o rótulo (fraude confirmada) é
juntado quando chegar, dias ou semanas depois. O CSV tem `ts`, as 12 features e `label`. O script **ordena por tempo**,
treina no passado e valida nos 20% mais recentes (nunca split aleatório) e imprime ROC-AUC, PR-AUC e, para cada precisão-alvo,
o limiar e o recall, que servem para escolher `reviewAt` e `denyAt`. Saídas: `model.tree` e `parity.csv`.

**Os dados sintéticos do script não significam nada:** eu mesmo defini o peso de cada feature ao gerá-los.

## Paridade Java × Python

O `parity.csv` traz vetores com a probabilidade que o Python previu. Comparado ao `TreeModel.score(...)` do Java, a
diferença máxima foi **2,2e-16** (4.000 vetores, modelo sintético) e **5,6e-17** (modelo treinado via CSV).

## Armadilhas

- **Training/serving skew:** o treino precisa calcular as features exatamente como o Java. Por isso o parser confere os nomes.
- **`<=` vs `<`:** o Java e o scikit-learn vão à esquerda com `<=`. O XGBoost usa `<`: ao importar, converta o limiar.
- **float32:** o scikit-learn percorre as árvores em float32. O script treina com valores já em float32 para a paridade ser
  exata. Em produção, uma feature em `double` bem na borda do limiar pode cair do outro lado (~1e-7): é normal.
- **Trocar o modelo é um deploy** (não há recarga a quente) e o `sha256` aparece no log de inicialização.

## Estado

O modelo de exemplo, a paridade e os dois modos do treino (sintético e CSV) foram executados. **Não existe modelo treinado
com dados reais**, então os números de desempenho (ROC-AUC etc.) do script só valem para dados reais.