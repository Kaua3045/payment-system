# Como explicar o antifraude

## Em 30 segundos

"Antes de mexer em dinheiro, o pagamento pergunta ao antifraude. Ele olha 13 sinais da tentativa, aplica regras para o
óbvio e um modelo de árvores para o resto, e devolve aprovar, analisar ou negar. Se ele falhar, o pagamento não cai."

## Em 2 minutos

Veja [00-visao-geral.md](00-visao-geral.md), a seção "Em 2 minutos".

## Roteiro de quadro branco (10 minutos)

Desenhe um passo de cada vez:

1. **A caixa do pagamento e a caixa do antifraude**, com uma seta: "antes de qualquer efeito" (0001).
2. **O que entra:** uma tentativa (quem, para quem, quanto, quando, de onde, com qual aparelho).
3. **As 12 features:** "tudo vira número". Mostre o exemplo de R$900 para um destinatário novo ([01](01-features.md)).
4. **As regras:** as duras negam e encerram; as suaves somam risco. Desenhe uma régua (logit) e some o boost ([02](02-regras-e-boost.md)).
5. **O modelo:** desenhe **uma árvore** com 3 perguntas e uma folha. "São 40 dessas; as folhas somam" ([03](03-modelo-e-treino.md)).
6. **O score:** a sigmoid achata a régua em 0 a 1; dois limiares dão os três vereditos.
7. **A memória:** o `StatsStore` alimenta as features do próximo pagamento ([04](04-estado-pipeline-e-falhas.md)).
8. **Quando quebra:** cada etapa tem a sua política de falha, e o pagamento nunca cai.
9. **O que falta:** o que não está pronto ([00](00-visao-geral.md), "Limites atuais").

## Perguntas difíceis

**Por que o antifraude roda antes da reserva?** Para não precisar compensar: nada mudou se a resposta for negar. E fora da
transação do banco para a chamada não segurar conexão nem lock. ([0001](../decisions/0001-fraude-antes-de-qualquer-efeito.md))

**Por que árvores, e não só regras ou uma rede neural?** Regras sozinhas não pesam combinações; redes pedem mais dados,
infraestrutura e são mais difíceis de explicar. Árvores capturam interações, rodam em microssegundos dentro do processo e
dá para mostrar o caminho de cada decisão. ([0003](../decisions/0003-modelo-de-arvores-in-process.md))

**Por que não chamar um serviço de ML?** Uma ida e volta de rede custa mais do que toda a avaliação (µs contra ms), e
adiciona um novo ponto de falha. ([0003](../decisions/0003-modelo-de-arvores-in-process.md))

**Por que somar logits e não probabilidades?** Probabilidades não se somam (0,7 + 0,6 = 1,3). No logit as evidências
acumulam e a sigmoid devolve sempre um valor entre 0 e 1. +1 multiplica as odds por ~2,7. ([0004](../decisions/0004-regras-duras-e-boost-no-logit.md))

**E se o Redis (o estado) cair?** A etapa `enrich` segue sem histórico e marca a decisão como degradada; as regras duras
ainda protegem. Nada impede o pagamento. ([04](04-estado-pipeline-e-falhas.md))

**Por que aprovar valor pequeno quando o antifraude falha?** É um trade-off de disponibilidade: bloquear todo mundo numa
queda é pior do que aceitar o risco de valores pequenos. O corte é configurável (`fail-open-below`) e a decisão vem marcada
como degradada para reprocessar. ([0002](../decisions/0002-contrato-fraudservice-e-fallback.md))

**Como sei que o Java dá o mesmo resultado do Python?** O treino exporta vetores com a probabilidade esperada (`parity.csv`);
o Java reproduz com diferença máxima de ~2e-16. ([03](03-modelo-e-treino.md))

**Como um fraudador não descobre as regras?** O cliente só recebe `DECLINED`. Os motivos ficam internos (logs e auditoria).
([0001](../decisions/0001-fraude-antes-de-qualquer-efeito.md))

**Por que a blocklist é um arquivo?** É dado, não código: dá para mudar sem deploy, com validação e sem perder a lista
anterior se o arquivo vier quebrado. O banco entra depois, se precisar. ([0009](../decisions/0009-blocklist-como-dado.md))

**Como o modelo é atualizado?** Treino offline, conferência de paridade, deploy. Não há recarga a quente (simples e
auditável), e o `sha256` do modelo aparece no log. ([03](03-modelo-e-treino.md))

**Como isso escala?** A avaliação é barata (um modelo de 281 mil nós gastou ~190 µs por chamada). O limite virá do estado
(Redis) e, mais longe, de contas muito quentes. Ver [05](05-logs-metricas-e-carga.md).

**O que ainda não está pronto, honestamente?** Modelo treinado com dados reais, estado em Redis, outros canais, a revisão
manual integrada e o serviço HTTP/eventos. Ver "Limites atuais" em [00](00-visao-geral.md).

## Como responder "por que foi decidido assim?"

Aponte a decisão em [decisions/](../decisions/README.md). Toda decisão tem **contexto**, **o que foi decidido**,
**consequências** e **alternativas descartadas**, e diz quando vale revisitar.