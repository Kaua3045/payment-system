# Documentação do módulo de antifraude

Esta pasta explica **o que foi construído, por que foi construído assim e como operar**. Hoje cobre o módulo de
**antifraude**.

## Por onde começar

| Seu objetivo | Leia |
|---|---|
| Entender tudo em 10 minutos | [fraud/00-visao-geral.md](fraud/00-visao-geral.md) |
| Explicar para outra pessoa | [fraud/09-como-explicar.md](fraud/09-como-explicar.md) e o [índice de decisões](decisions/README.md) |
| Entender o que o modelo "vê" | [fraud/01-features.md](fraud/01-features.md) |
| Entender o score, os limiares e o boost | [fraud/02-regras-e-boost.md](fraud/02-regras-e-boost.md) |
| Entender a árvore e o treino | [fraud/03-modelo-e-treino.md](fraud/03-modelo-e-treino.md) |
| Mexer no código | [fraud/07-referencia-de-classes.md](fraud/07-referencia-de-classes.md) |
| Colocar em produção / investigar um problema | [fraud/08-operacao.md](fraud/08-operacao.md) |
| Saber **por que** algo é assim | [decisions/README.md](decisions/README.md) |

## Convenções

- **Decisões (ADRs)** ficam em `decisions/`, numeradas. **Nunca edite uma decisão antiga para mudar o que ela decidiu**:
  crie uma nova que a substitui e marque a antiga como "Substituída por 00XX".
- Os documentos de `fraud/` descrevem **como é hoje**. Se o código mudar, atualize o documento no mesmo commit.
- Cada documento tem uma seção **Estado** com o que foi verificado. Atualize-a quando rodar algo novo.

## Estado da verificação (do módulo de antifraude)

| Item | Estado |
|---|---|
| Compilação (JDK 21, sem avisos) | verificada |
| Suíte do módulo (valor, canais, regras, features, pipeline, use case, blocklist, módulo) | passou após a adição dos logs |
| Teste de carga | medido em 1 CPU, em memória, sem Redis nem rede |
| Spring (`FraudConfiguration`), Redis, Postgres, JUnit/Mockito reais, Logback/OTel reais | **não verificados** |
| Dados de treino reais | **não existem ainda**: o modelo de exemplo é feito à mão e o treinado usa dados sintéticos |