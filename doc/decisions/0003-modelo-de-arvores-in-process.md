# 0003. Modelo de árvores avaliado dentro do processo

- **Status:** Aceita
- **Data:** 2026-11-21

## Contexto
É preciso um score de risco em microssegundos, explicável, sem uma dependência pesada. O estudo do módulo foi inspirado num
curso (domínio de fraude, features, árvore de decisão, sigmoid, `ParseTreeModel` com validação agressiva).

## Decisão
- Treinar um conjunto de árvores (gradient boosting) **offline em Python** (scikit-learn) e exportar para um **formato de
  texto próprio** (`TREEMODEL 1`).
- Avaliar **dentro da JVM**, com um avaliador e um parser próprios. O parser **valida tudo** (o arquivo é entrada não
  confiável) e um modelo inválido impede o serviço de subir.
- O contrato com o treino é a lista de **features, em ordem fixa**; o parser recusa um arquivo com nomes ou ordem diferentes.
- A sigmoid é implementada **sem `java.lang.Math`** (série de Taylor), com paridade verificada contra o Python (diferença
  máxima de ~2e-16).

## Consequências
**Boas:** sem rede nem dependência de runtime; microssegundos por chamada; o caminho de cada decisão é explicável; paridade
verificável por arquivo (`parity.csv`).
**Custos e riscos:** um formato e um parser para manter; só árvores (nada de modelos profundos); retreinar exige manter as
mesmas features; a `exp` escrita à mão foi uma escolha de estudo e controle: **`Math.exp` seria aceitável** em produção.

## Alternativas descartadas
- **Serviço de ML externo:** uma ida e volta de rede custa mais do que toda a avaliação e cria um ponto de falha.
- **ONNX Runtime / XGBoost4J:** dependência pesada (nativa) para um modelo que cabe num arquivo de texto.
- **Regressão logística:** não captura interações (valor alto + aparelho novo + destinatário novo).
- **Só regras:** não pesam combinações e viram uma lista infinita.

## Quando revisitar
Se o modelo precisar de algo que árvores não dão, se a manutenção do parser pesar, ou se `Math.exp` passar a ser preferível.