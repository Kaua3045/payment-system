# 0004. Regras duras decidem; regras suaves somam no logit

- **Status:** Aceita
- **Data:** 2026-11-21

## Contexto
Alguns sinais são determinísticos e baratos (conta bloqueada, limite regulatório); outros são evidências que não condenam
sozinhas (rajada de tentativas, viagem impossível). É preciso combinar ambos com o modelo.

## Decisão
- **Regra dura:** devolve `Deny(motivo)`, decide DENY (score 1,0) e **encerra** o pipeline: o modelo nem roda.
- **Regra suave:** devolve `Boost(motivo, logit)`, e o logit é **somado** ao logit do modelo antes da sigmoid.
- Ordem das regras: duras e baratas primeiro (blocklist, limite, limite noturno), depois as suaves.
- Os valores iniciais (+2,0 para velocity, +3,0 para viagem impossível) são pontos de partida.

## Consequências
**Boas:** as evidências acumulam de forma coerente (+1 no logit multiplica as odds por ~2,7), o score continua entre 0 e 1, e
o mesmo boost pesa muito em casos de risco baixo e pouco em casos já quase certos. Cada regra é uma linha.
**Custos e riscos:** um boost grande demais vira uma regra dura disfarçada; os valores dos boosts não foram calibrados com
dados reais.

## Alternativas descartadas
- **Somar ou tirar a média de probabilidades:** 0,7 + 0,6 = 1,3; não faz sentido.
- **Regras só duras:** perdem a gradação.
- **Colocar as regras suaves como features do modelo:** exigiria retreinar para cada ajuste.

## Quando revisitar
Quando houver dados reais: calibrar os boosts (ou promover um sinal a feature do modelo).