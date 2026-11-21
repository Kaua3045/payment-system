# 0010. Logs com SLF4J direto, sem classes próprias

- **Status:** Aceita
- **Data:** 2026-11-21

## Contexto
A infraestrutura já tem o Logback, o agent do OpenTelemetry (que captura os logs, junta o `trace_id` e exporta para o
Elastic) e métricas em OpenTelemetry.

## Decisão
- Usar **SLF4J direto** nas classes que precisam logar, sem uma abstração própria.
- **Uma linha chave=valor** por decisão; o **nível controla o volume**: degradada em WARN, REVIEW e DENY em INFO,
  **APPROVE em DEBUG** (desligado em produção). Sem amostragem em código.
- **Nenhum dado pessoal** no log (só ids opacos, canal, veredito, motivos).
- O log de inicialização traz a **impressão digital do modelo** (`sha256`).
- A **contagem** (decisões, falhas, latência) fica nas **métricas**, e o detalhe por requisição nos atributos do span.

## Consequências
**Boas:** muito menos código; usa o que a infraestrutura já faz; fácil de explicar.
**Custos e riscos:** sem limitador próprio, uma queda do Redis pode gerar muitos WARN com stack: mitigar por métrica e por
configuração do framework; a mensagem da exceção pode vazar dado se o cliente a incluir.

## Alternativas descartadas
Um `FraudLogger` próprio, com **amostragem de aprovações, um limitador de falhas repetidas e um anonimizador com sal**. Foi
implementado e testado e depois **removido**, por duplicar o que o Logback e o OpenTelemetry já resolvem.

## Quando revisitar
Se uma queda de dependência inundar o log de fato (aí um limitador de uns 15 linhas só para o WARN de falha) ou se for
preciso campos estruturados (a API fluente do SLF4J 2 com pares chave-valor, e a opção do agent para exportá-los).