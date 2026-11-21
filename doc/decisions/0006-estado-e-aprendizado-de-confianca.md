# 0006. Estado em porta própria; só se aprende confiança com aprovadas

- **Status:** Aceita
- **Data:** 2026-11-21

## Contexto
As features de velocity, destinatário novo, aparelho novo e viagem impossível dependem do histórico da conta. Duas brechas
foram achadas: o retry contava a si mesmo na velocity, e tentativas não aprovadas atualizavam a "última localização",
limpando uma viagem impossível.

## Decisão
- O histórico fica atrás de uma porta: `StatsStore` (`load`, `record`). Hoje `InMemoryStatsStore`; em produção, Redis.
- `record` é **idempotente por `attempt.id`**, e `load` ignora o evento com o mesmo `id` (retry não conta a si mesmo).
- **Toda tentativa conta na velocity** (inclusive as negadas).
- **Só `APPROVE` ensina confiança:** destinatário, aparelho, valor médio e **localização**.

## Consequências
**Boas:** retry seguro; o fraudador não "treina" o perfil da vítima nem limpa a viagem impossível; a porta permite trocar a
implementação.
**Custos e riscos:** o `InMemoryStatsStore` só serve para uma instância e a memória cresce; uma tentativa em `REVIEW` não
ensina confiança, então, depois de o analista aprovar, a próxima parecida cai em `REVIEW` de novo ([0011](0011-revisao-manual-reter-saldo-e-expirar.md)).

## Alternativas descartadas
- **Aprender com toda tentativa:** deixa o fraudador envenenar o perfil.
- **Calcular tudo a partir do ledger a cada decisão:** lento demais para o orçamento de 50 ms.

## Quando revisitar
Ao implementar o Redis (script Lua atômico, TTL) e ao fechar o ciclo de aprendizado com os resultados dos analistas.