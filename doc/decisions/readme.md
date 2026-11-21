# Registro de decisões (ADRs)

Cada arquivo registra **uma** decisão: o contexto, o que foi decidido, as consequências e as alternativas descartadas.
Servem para responder, daqui a um ano, "por que isto é assim?".

## Índice

| Nº | Decisão | Status |
|---|---|---|
| [0001](0001-fraude-antes-de-qualquer-efeito.md) | O antifraude decide antes de qualquer efeito, e as negadas saem com código genérico | Aceita |
| [0002](0002-contrato-fraudgateway-e-fallback.md) | `FraudGateway` como fronteira, e fallback quando ele falha | Aceita |
| [0003](0003-modelo-de-arvores-in-process.md) | Modelo de árvores avaliado dentro do processo | Aceita |
| [0004](0004-regras-duras-e-boost-no-logit.md) | Regras duras decidem; regras suaves somam no logit | Aceita |
| [0005](0005-pipeline-leve-em-vez-de-maquina-generica.md) | Pipeline leve em vez de máquina de estados genérica | Aceita |
| [0006](0006-estado-e-aprendizado-de-confianca.md) | Estado em porta própria; só se aprende confiança com aprovadas | Aceita |
| [0007](0007-dinheiro-com-bigdecimal.md) | Dinheiro como `BigDecimal` de escala 2 | Aceita |
| [0008](0008-multicanal-envelope-e-pipeline-por-canal.md) | Tentativa neutra, dados por canal e um pipeline por canal | Aceita |
| [0009](0009-blocklist-como-dado.md) | Blocklist como dado (arquivo ou memória) | Aceita |
| [0010](0010-logs-com-slf4j-direto.md) | Logs com SLF4J direto, sem classes próprias | Aceita |
| [0011](0011-revisao-manual-reter-saldo-e-expirar.md) | Revisão manual: reter o saldo e expirar o prazo | **Proposta** (não integrada) |
