# 0001. O antifraude decide antes de qualquer efeito, e as negadas saem com código genérico

- **Status:** Aceita
- **Data:** 2026-11-21

## Contexto
O `CreateTransaction` reserva saldo e escreve no ledger. Se o antifraude fosse consultado depois disso, uma negativa exigiria
compensar (liberar a reserva, estornar). Além disso, a API precisa suportar retries do cliente.

## Decisão
1. O `CreateTransaction` chama `FraudGateway.assess(Attempt)` **depois** da checagem de idempotência e das leituras, e **antes**
   de reservar saldo.
2. A chamada fica **fora da transação do banco**, para não segurar conexão nem lock enquanto espera.
3. O `Attempt.id` é a **chave de idempotência** da operação: um retry com a mesma chave é seguro (o `StatsStore` ignora o
   próprio evento).
4. Uma negada é **persistida** do lado de pagamentos (transação `DENIED`), com o código público genérico **`DECLINED`**. Os
   motivos internos (`reasons`) **nunca** saem na API.

## Consequências
**Boas:** nada para compensar quando a resposta é negar; a latência do antifraude não segura locks; retries seguros; um
fraudador não aprende as regras por tentativa e erro.
**Custos e riscos:** a latência do antifraude entra no caminho crítico (orçamento de 50 ms); uma aprovação do antifraude ainda
pode falhar adiante (saldo insuficiente), o que é normal.

## Alternativas descartadas
- **Antifraude depois da reserva:** exige compensação e mantém saldo bloqueado à toa.
- **Antifraude assíncrono, depois do fato:** o dinheiro já se moveu. Serve para monitoramento, não para decidir.
- **Devolver os motivos ao cliente:** ajuda o fraudador a contornar as regras.

## Quando revisitar
Se o antifraude virar um serviço remoto (timeouts e orçamento mudam), ou se a revisão manual for integrada ([0011](0011-revisao-manual-reter-saldo-e-expirar.md)).