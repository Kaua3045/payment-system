# 0011. Revisão manual: reter o saldo e expirar o prazo

- **Status:** **Proposta** (desenhada e testada com um banco em memória; não integrada)
- **Data:** 2026-11-21

## Contexto
Um score na zona cinzenta devolve `REVIEW`. O antifraude é chamado antes de reservar ([0001](0001-fraude-antes-de-qualquer-efeito.md)),
então é preciso decidir o que fazer com a transação, a reserva e o ledger enquanto um analista decide.

## Decisão
- No `REVIEW`: **reservar o saldo** (bloqueado, mas **sem tocar no ledger**), gravar a transação `IN_REVIEW` com um prazo,
  abrir um **caso** na fila do analista e gravar um evento no outbox, tudo numa transação do banco. A resposta é `202 UNDER_REVIEW`.
- **Aprovar** liquida usando a reserva existente; **negar** libera a reserva; **vencer o prazo** expira a transação e libera
  o saldo (**fail-closed**).
- Quem decide a corrida entre analista, outro analista e expiração é o **update condicional** `WHERE status = 'OPEN'`, sem lock.
- Repetir a mesma decisão é **idempotente**; uma decisão contrária é **conflito**.
- Uma reserva perdida é refeita na aprovação; sem saldo, a transação vai para `FAILED` e a **decisão do analista fica registrada**.
- Prazos (30 min de revisão, 10 min de folga na reserva) são **placeholders**, a definir com negócio e compliance.

## Consequências
**Boas:** o dinheiro não é gasto durante a análise; nada é movido duas vezes; toda decisão tem auditoria.
**Custos e riscos:** o cliente vê o saldo retido; o antifraude não aprende com a decisão do analista (a próxima tentativa
parecida cai em `REVIEW` de novo); faltam quatro olhos para valores altos e a autenticação extra (step-up).

## Alternativas descartadas
- **Tratar `REVIEW` como `DENY`:** bloqueia gente legítima na zona cinzenta.
- **Não reservar o saldo:** o cliente pode gastá-lo e a aprovação falha por saldo insuficiente.
- **Aprovar e monitorar:** só serve para valores baixos.
- **Autenticação extra (step-up) no lugar do analista:** boa para a faixa de risco médio; fica como evolução, reaproveitando
  o mesmo estado `IN_REVIEW` e a mesma reserva.

## Quando revisitar
Ao integrar ao `CreateTransaction`, ao definir os prazos com compliance, e ao fechar o ciclo de aprendizado com as decisões.