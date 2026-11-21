# 0002. `FraudGateway` como fronteira, e fallback quando ele falha

- **Status:** Aceita
- **Data:** 2026-11-21

## Contexto
O antifraude hoje roda dentro da aplicação, mas deve virar um microsserviço (HTTP síncrono para a decisão, eventos Kafka
para estado, aprendizado e auditoria). O pagamento não pode cair por causa dele.

## Decisão
- O módulo de pagamentos conhece **uma interface**: `FraudGateway.assess(Attempt) → Assessment`. Hoje a implementação é chamar direto o usecase `AssessFraudUsecase`,
    no futuro a ideia é receber um `AttemptRequest` e chamar um serviço HTTP remoto que vai retornar `AssessmentResult`. Hoje está assim porque esta tudo dentro do mesmo processo, mas a interface é a mesma.
- `Assessment` traz `verdict`, `score`, `reasons` e `degraded`.
- O `FraudGatewayImpl`: em **qualquer** `RuntimeException`, valor ≤ `failOpenBelow` (padrão R$ 200,00)
  **aprova**, o resto vai para `REVIEW`, sempre com `degraded = true` (para reprocessar depois).
- O **timeout** fica no cliente (HTTP e Redis), não em threads dentro do módulo.

## Consequências
**Boas:** a troca para HTTP é local; o pagamento não cai; decisões degradadas ficam visíveis e reprocessáveis.
**Custos e riscos:** aprovar valor pequeno numa falha é um risco aceito (e configurável); `REVIEW` em falha depende de a
revisão manual existir ([0011](0011-revisao-manual-reter-saldo-e-expirar.md)).

## Alternativas descartadas
- **Sempre aprovar na falha:** risco alto demais para valores grandes.
- **Sempre negar na falha:** derruba a disponibilidade numa queda do antifraude.
- **Timeout com threads/futures no módulo:** complexidade e vazamento de threads; o cliente HTTP/Redis já sabe expirar.

## Quando revisitar
Ao criar o cliente HTTP (definir timeout e política de retry) e ao definir o corte `failOpenBelow` com negócio e compliance.