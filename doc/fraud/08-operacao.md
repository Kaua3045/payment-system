# Operação

## Propriedades

```properties
fraud.model.path=/etc/app/fraud/model.tree
fraud.blocklist.path=/etc/app/fraud/blocklist.txt     # sem esta propriedade a blocklist fica vazia, em memória
fraud.blocklist.refresh-seconds=30
fraud.threshold.review=0.30
fraud.threshold.deny=0.80
fraud.limit.amount=5000.00
fraud.limit.night=1000.00
fraud.velocity.max-per-minute=3
fraud.velocity.boost=2.0
fraud.travel.max-speed-kmh=900
fraud.travel.boost=3.0
fraud.fail-open-below=200.00
```
(No IDE, o caminho do modelo pode ser relativo à pasta de trabalho, ex.: `./src/main/resources/fraud/dev-model.tree`.)

## Códigos de motivo (`reasons`)

| Código | Significado | Origem |
|---|---|---|
| `BLOCKLIST_ORIGIN` / `_DESTINATION` / `_INSTRUMENT` / `_DEVICE` | a conta, o instrumento (chave Pix, cartão) ou o aparelho está na blocklist | regra dura |
| `AMOUNT_LIMIT` | valor acima do limite | regra dura |
| `NIGHT_LIMIT` | acima do limite noturno | regra dura |
| `VELOCITY_1M` | rajada de tentativas no último minuto | boost |
| `IMPOSSIBLE_TRAVEL` | deslocamento rápido demais | boost |
| `FALLBACK:<etapa>:<Exceção>` | a etapa falhou e aplicou a política de falha | falha |
| `UNSUPPORTED_CHANNEL:<canal>` | o canal não tem pipeline | configuração |

Os motivos são **internos**: nunca saem na API ([decisão 0001](../decisions/0001-fraude-antes-de-qualquer-efeito.md)).

## Runbook

### Bloquear uma conta, chave Pix, cartão ou aparelho (Não implementado o arquivo ainda)
Edite o arquivo da blocklist (uma chave `tipo:valor` por linha; `#` comenta). Vale em até `refresh-seconds`. **Grave em um
arquivo temporário e renomeie por cima**, para a leitura nunca pegar o arquivo pela metade. Se o arquivo ficar inválido, a
lista **anterior é mantida** e o erro aparece no log (com o número da linha). Um `acc-123` (sem os dois-pontos) recusa o
arquivo inteiro, de propósito.
```
# contas, chaves Pix, cartões e aparelhos bloqueados
acc:conta-laranja-123
key:golpista@email.com
card:tok_roubado_9
dev:aparelho-clonado-7
```

### Trocar o modelo
1. Treinar (`train_and_export.py`) e conferir o relatório e a **paridade** (`parity.csv`).
2. Colocar o `model.tree` no caminho de `fraud.model.path`.
3. **Fazer o deploy** (não há recarga a quente). Um modelo inválido impede o serviço de subir: o deploy falha, e o tráfego
   continua no modelo anterior.
4. Confirmar no log de inicialização o `sha256`, o número de árvores e de nós.

### Ajustar os limiares
Use o relatório do treino (precisão × recall por limiar) e o custo de cada erro para o negócio. Mude `fraud.threshold.*` e
faça deploy. Acompanhe a taxa de REVIEW e DENY (métrica `fraud.decisions`).

### Investigar "meu pagamento foi negado"
1. Pegue a chave de idempotência (`attempt`) da requisição.
2. Busque `fraud.decision attempt=<chave>` nos logs: veja `verdict`, `score`, `reasons`, `path`.
3. Traduza o código pelo quadro acima. Se `degraded=true`, o antifraude falhou (procure `fraud.stage_failure`).
4. **Não revele** o motivo ao cliente: a API devolve só o código genérico `DECLINED`.

## Alertas sugeridos
Falhas de etapa ou fallback acima de 1% do tráfego; qualquer `unsupported_channel`; `sha256` do modelo mudando sem deploy;
p99 de latência acima de 40 ms; salto na taxa de DENY ou REVIEW; blocklist vazia por engano (`fraud.blocklist.size`).

## Antes de ir para produção

- [ ] `InMemoryStatsStore` trocado por Redis (um script Lua atômico, TTL) se houver mais de uma instância.
- [ ] Modelo **treinado com dados reais** e limiares calibrados (o modelo de exemplo é só para desenvolvimento).
- [ ] Limites e janela noturna confirmados com compliance.
- [ ] Níveis de log em `INFO` (aprovações em `DEBUG` desligadas) e métricas ligadas.
- [ ] Teste de carga repetido com o Redis real e com o nível de log de produção.
- [ ] Decidir se o REVIEW será integrado (06) ou tratado como DENY enquanto isso.
- [X] Implementar o jdbc da blocklist e a recarga a quente.
- [ ] Implementar a inserção de keys na blocklist
- [X] Mudar a interface `FraudGatewayImpl` para receber um record `AttemptRequest` e devolver um `AssessmentResult`
- [ ] Implementar o step de geo location na pipeline
