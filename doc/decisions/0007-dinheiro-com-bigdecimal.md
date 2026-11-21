# 0007. Dinheiro como `BigDecimal` de escala 2

- **Status:** Aceita
- **Data:** 2026-11-21

## Contexto
O módulo começou com valores em centavos (`long`). O domínio de pagamentos usa `BigDecimal` (`Money`), e o banco guarda
`NUMERIC(19,2)`.

## Decisão
- O valor é `BigDecimal`, normalizado para **escala 2**; o `Attempt` **recusa** o que perderia precisão (mais de 2 casas),
  zero e negativos.
- Comparações com `compareTo`, **nunca** `equals` (que considera a escala: 5000 ≠ 5000.00).
- `double` **só** dentro do `FeatureExtractor`, porque feature de modelo não é dinheiro.
- Banco: `NUMERIC(19,2)`.

## Consequências
**Boas:** nada de arredondamento silencioso; coerente com o resto do domínio.
**Custos e riscos:** o `compareTo` exige disciplina; a conversão para `double` nas features perde precisão (irrelevante ali).

## Alternativas descartadas
- **`long` em centavos:** preciso, mas diferente do resto do domínio e exige conversões nas bordas.
- **`double` para dinheiro:** nunca.

## Quando revisitar
Se uma moeda com outra escala for suportada.