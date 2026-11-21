# 0008. Tentativa neutra, dados por canal e um pipeline por canal

- **Status:** Aceita
- **Data:** 2026-11-21

## Contexto
O antifraude nasceu para o Pix, mas deve atender também transferência entre contas e cartão, sem espalhar `if (canal)`.

## Decisão
- `Attempt` é um **envelope neutro** (`id`, `origin`, `destination`, `amount`, `at`, `deviceId`, `location`,
  `destinationCreatedAt`, `details`).
- Os dados do canal ficam numa interface **selada** `ChannelDetails` (`Pix`, `AccountTransfer`, `CardPayment`) com acesso
  neutro (`blockKey()`, `instrumentCreatedAt()`). O compilador sabe quais canais existem.
- As features têm **nomes neutros** (`counterparty_new`, `instrument_recent`).
- Há **um pipeline por canal** (`Map<Channel, FraudPipeline>`); hoje só o Pix.
- Canal sem pipeline → `REVIEW` degradado (`UNSUPPORTED_CHANNEL:<canal>`): nunca fail-open nem aprovação.
- O limite noturno vale só para os canais configurados (Pix e transferência).

## Consequências
**Boas:** um novo canal é um tipo de `ChannelDetails`, um modelo, regras e uma linha no `Map`.
**Custos e riscos:** o vetor de 13 features é compartilhado; features específicas de cartão exigiriam estendê-lo ou ter um
esquema por pipeline.

## Alternativas descartadas
- **Um `Attempt` por canal:** duplica o resto do código.
- **`if (canal)` espalhado:** não escala.

## Quando revisitar (em aberto)
Um modelo único ou um por canal (recomendado: por canal); o esquema de features para cartão; estado compartilhado entre
canais (hoje a chave é a conta de origem); `FraudSettings` por canal.