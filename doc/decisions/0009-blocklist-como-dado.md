# 0009. Blocklist como dado (arquivo ou memória)

- **Status:** Aceita
- **Data:** 2026-11-21
- **Status:** Substituída por 0012

## Contexto
Bloquear uma conta, chave Pix, cartão ou aparelho deve ser possível sem deploy, e a consulta roda no caminho crítico.

## Decisão
- A consulta é só um `Set.contains` em memória (`BlockList`).
- A fonte é um **arquivo de texto**: uma chave `tipo:valor` por linha (`acc`, `key`, `card`, `dev`); `#` e linhas vazias são ignorados.
- O `RefreshingBlockList` recarrega de tempos em tempos (padrão 30 s) e troca o `Set` **atomicamente**.
- **Qualquer linha inválida recusa o arquivo todo**, com o número da linha.
- **Carga inicial inválida: o serviço não sobe** (sem blocklist é risco). **Recarga inválida: mantém a lista anterior** e avisa.
- Sem `fraud.blocklist.path`, usa-se uma `InMemoryBlockList` vazia (desenvolvimento).

## Consequências
**Boas:** muda sem deploy; a consulta é O(1) e sem I/O; um erro de digitação (`acc-123`) não vira uma regra que nunca dispara.
**Custos e riscos:** o arquivo precisa ser escrito de forma atômica (temporário + renomear); em várias instâncias, todas
precisam ler o mesmo arquivo.

## Alternativas descartadas
- **Tabela no banco (JDBC):** adiada; o arquivo cobre o começo sem I/O no caminho crítico.
- **Lista no código ou na configuração:** exige deploy.

## Quando revisitar
Quando houver painel de operação ou várias instâncias que precisem de uma fonte única (aí, banco ou evento).