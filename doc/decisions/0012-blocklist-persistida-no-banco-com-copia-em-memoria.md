# 0012. Blocklist persistida no banco com snapshot em memória

- **Status:** Aceita
- **Data:** 2026-11-24
- **Substitui:** 0009. Blocklist como dado (arquivo ou memória)

## Contexto

Bloquear uma conta, chave Pix, cartão ou dispositivo deve ser possível sem deploy.

A consulta à blocklist acontece durante a avaliação antifraude, portanto precisa ter baixo custo e não adicionar I/O ao caminho crítico das transações.

A abordagem anterior utilizava um arquivo de texto como fonte de dados. Entretanto, com a adoção de JDBC e PostgreSQL como infraestrutura de persistência, manter uma fonte externa baseada em arquivos adicionaria complexidade operacional e dificultaria a centralização dos bloqueios.

## Decisão

- O PostgreSQL é a fonte de verdade da blocklist.
- O acesso e a persistência dos registros são realizados via JDBC.
- A consulta durante a avaliação antifraude é feita exclusivamente em memória, utilizando `Set.contains()`.
- Um snapshot da blocklist é carregado do banco e mantido em memória por meio de uma implementação `RefreshingBlockList`.
- O snapshot é atualizado periodicamente, com intervalo padrão de 60 segundos.
- Cada atualização carrega os registros do banco e constrói um novo `Set`.
- A substituição do snapshot acontece atomicamente, garantindo que as consultas observem sempre uma versão completa da lista.
- Em caso de falha durante a atualização, o snapshot anterior é preservado e a falha é registrada.
- Na inicialização, a aplicação precisa carregar a blocklist com sucesso antes de disponibilizar o serviço.
- Cada instância da aplicação mantém seu próprio snapshot e realiza sua própria sincronização com o banco.

### Modelo dos registros

Cada registro representa um identificador bloqueado, composto por:

- `type`: tipo do identificador (`acc`, `key`, `card`, `dev`).
- `value`: valor do identificador bloqueado.
- `created_at`: data de criação do bloqueio.
- `expires_at`: data de expiração caso exista. Caso seja um bloqueio temporário até garantir que aquela conta ou chave é segura

A combinação de `type` e `value` deve possuir uma restrição de unicidade no banco.

### Atualização dos dados

Novos bloqueios e desbloqueios são persistidos diretamente no PostgreSQL.
No momento não foi implementado a inserção de novos bloqueio no PostgreSQL, mas no futuro vai ser via kafka consumindo eventos de fraud.

As alterações passam a ser refletidas nas instâncias após a próxima atualização do snapshot, respeitando o intervalo de sincronização.

Não há consultas ao banco durante a avaliação individual de uma transação.

## Consequências

**Vantagens:**

- Consultas O(1) em memória, sem I/O no caminho crítico.
- Persistência centralizada e durável.
- Inclusão e remoção de bloqueios sem necessidade de deploy.
- Consistência eventual entre as instâncias, com atraso limitado pelo intervalo de atualização em condições normais.
- Não depende de arquivos externos ou de compartilhamento de filesystem entre instâncias.

**Custos e riscos:**

- Cada instância mantém uma cópia da blocklist em memória.
- Alterações não são refletidas imediatamente em todas as instâncias.
- O mecanismo de atualização periódica adiciona consultas de leitura ao banco.
- Falhas de sincronização podem deixar uma instância utilizando um snapshot desatualizado.
- A inicialização depende da disponibilidade do banco para carregar o snapshot inicial.

## Alternativas descartadas

**Consulta JDBC a cada avaliação antifraude:**

Descartada por adicionar latência e I/O ao caminho crítico de cada transação.

**Arquivo de texto:**

Substituído por persistência no PostgreSQL, evitando manter duas fontes de dados e simplificando a operação.

**Lista diretamente no código ou configuração:**

Descartada por exigir deploy para inclusão ou remoção de bloqueios.

**Eventos para atualização imediata:**

Adiados. Podem ser considerados futuramente caso o intervalo de sincronização se torne insuficiente.

## Quando revisitar

- Quando for necessário refletir bloqueios imediatamente em todas as instâncias.
- Quando o volume de registros tornar snapshots completos custosos.
- Quando houver necessidade de auditoria, histórico de bloqueios ou identificação do operador responsável.
- Quando a frequência de atualização precisar ser reduzida por meio de eventos ou mecanismos de invalidação.