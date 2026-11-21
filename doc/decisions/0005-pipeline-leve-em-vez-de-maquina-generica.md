# 0005. Pipeline leve em vez de máquina de estados genérica

- **Status:** Aceita
- **Data:** 2026-11-21

## Contexto
A primeira versão seguiu o curso: uma máquina genérica (`Machine`), 5 handlers, fallback por etapa, um `Batcher`, cache
semântico no desempate e recarga a quente do modelo. Eram **1.498 linhas em 44 arquivos** para 3 a 5 etapas curtas.

## Decisão
Substituir por uma **lista de etapas** (`FraudStep`: `name`, `run`, `onFailure`) sobre um `FraudCase` mutável por chamada, com
**parada antecipada** quando uma etapa decide e a **política de falha dentro de cada etapa**. Removidos: a máquina genérica,
o `Batcher`, o cache semântico e a recarga a quente (trocar o modelo é um deploy).

## Consequências
**Boas:** muito menos código; mais fácil de ler e de explicar; mais rápido (medido: p50 de 3,9 µs na máquina completa contra
2,3 µs na sequência direta).
**Custos e riscos:** sem recarga a quente, trocar o modelo exige deploy; uma máquina de estados só voltaria a valer com
muitos estados, desvios e persistência/retomada.

## Alternativas descartadas
- **Manter a máquina genérica:** complexidade sem benefício para 3 a 5 etapas curtas.
- **Chamada sequencial dentro do use case, sem abstração:** funciona, mas perde o `path`, a política de falha por etapa e a
  facilidade de acrescentar uma etapa.

## Quando revisitar
Se o fluxo ganhar muitas etapas, desvios condicionais ou precisar ser persistido e retomado.