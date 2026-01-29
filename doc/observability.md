# 📌 Observabilidade indices
- [📌 Introdução](#-introdução)
- [📌 Padrão de Nomeação dos Spans](#-padrão-de-nomeação-dos-spans)
- [📌 Como Implementar Spans](#-como-implementar-spans)
- [📌 Documentação de Spans](#-documentação-de-spans)
  - [📄 Exemplo de Documentação para `idempotency.save`](#-exemplo-de-documentação-para-idempotencysave)
- [📌 Onde Documentar](#-onde-documentar)

## 📌 Introdução

A observabilidade é essencial para entender o comportamento 
dos microserviços, detectar falhas rapidamente e 
garantir um desempenho adequado. Neste documento, 
estão definidos os padrões de rastreamento de spans, 
logs e métricas, garantindo padronização e clareza no 
monitoramento do sistema.

## 📌 Padrão de Nomeação dos Spans

Os nomes dos spans devem seguir um padrão consistente para 
facilitar a análise e depuração.

| Componente         | Padrão do Nome do Span           | Exemplo                          | Observação                              |
|--------------------|----------------------------------|----------------------------------|-----------------------------------------|
| HTTP Request       | http.\<método\>.\<recurso\>      | http.get.customer                | Requisições externas                    |
| Banco de Dados     | db.\<tipo\>.\<ação\>             | db.sql.insert.customer           | Suporte a SQL e NoSQL                   |
| Cache              | cache.\<ação\>.\<recurso\>       | cache.get.customer               | Memcached, Redis, etc.                  |
| Mensageria         | queue.\<ação\>.\<fila\>          | queue.consume.order_created      | Kafka, RabbitMQ, etc.                   |
| Idempotência       | idempotency.\<ação\>             | idempotency.save                 | Para salvar, buscar ou remover         |
| Observabilidade    | observation.\<ação\>.\<recurso\> | observation.trace.user_action    | Logs, métricas, tracing                |
| Autenticação       | auth.\<ação\>                    | auth.validate_token              | Login, validação, permissões           |
| Tarefas Assíncronas| async.\<ação\>.\<recurso\>       | async.process.notification       | Processos em background                |
| Serviço Externo    | external.\<sistema\>.\<ação\>    | external.payment.create_tx       | APIs externas                          |
| Filters/Middleware | filter.\<ação\>                  | filter.authentication            | Para spans em filtros e middlewares    |

## 📌 Como Implementar Spans

Exemplo de criação de spans utilizando `ObservationHelper` com opentelemetry:

```java
public class Saves {
    
    private final ObservationHelper observationHelper;

    public Saves(final ObservationHelper observationHelper) {
        this.observationHelper = Objects.requireNonNull(observationHelper);
    }
    
    public void saveWithoutReturn(final String id, final String name) {
        this.observationHelper.observation(
            "span-name",
            (span) -> {
                span.setAttribute("id", id);
                span.setAttribute("name", name);
                
                // Do something
            }
        );
    }
    
    public String saveWithReturn(final String id, final String name) {
        return this.observationHelper.observationWithReturn(
            "span-name",
            (span) -> {
                span.setAttribute("id", id);
                span.setAttribute("name", name);
                
                // Do something
                
                return "return";
            }
        );
    }
    
    public String saveWithValuesAndReturn(final String id, final String name) {
        return this.observationHelper.observationWithValuesAndReturn(
            "span-name",
            (span) -> {
                span.setAttribute("id", id);
                span.setAttribute("name", name);
                
                // Do something
                
                return "return";
            },
            "attribute1", "value1",
            "attribute2", "value2"
        );
    }
}
```

## 📌 Documentação de Spans

Cada span deve ser documentado com:

- Nome: Seguir o padrão de nomeação.

- Descrição: O que ele representa.

- Atributos: Chaves e valores importantes.

#### 📄 Exemplo de Documentação para `idempotency.save`

```text
span: idempotency.save
description: Salva uma chave de idempotência
attributes:
  idempotency_key: "Chave de idempotência usada para evitar duplicidades."
  ttl: "Tempo de vida da chave em segundos."
  storage_type: "Onde a chave está armazenada (e.g., Redis, In-Memory)."
```
A descrição normalmente pode ser adicionada como um atributo, 
ou então não ser adicionada

## 📌 Onde Documentar

- No código: Comentários Javadoc.
- Nos arquivos Markdown: Adicionar tabelas e exemplos.