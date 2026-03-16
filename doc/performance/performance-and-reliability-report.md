# Performance & Reliability Report
Payment System – Pix Transfer Use Case

## 1. Contexto
Este documento descreve os testes de capacidade e confiabilidade realizados no fluxo de transferência Pix do sistema.
O objetivo foi identificar:
- capacidade sustentável de requisições por segundo
- comportamento de latência sob carga
- ponto de saturação da infraestrutura
- gargalo principal do sistema

O fluxo testado executa:
1. busca da conta de origem
2. busca da chave pix
3. busca da conta de destino
4. validações de regra de negócio
5. criação da transação
6. atualização de saldo das contas
7. conclusão ou falha da transação

Características do sistema:
- processamento 100% síncrono
- sem cache
- sem filas
- banco relacional como única fonte de verdade
- idempotência garantida por `UNIQUE (idempotency_key)`
- controle de concorrência via optimistic locking

## 2. Ambiente de Teste
Aplicação:
- Spring Boot
- JDBC repositories
- pool de conexões HikariCP
- Virtual Threads (Java)

Banco:
- PostgreSQL 17

Pool de conexões: `maximumPoolSize = 20`

Observabilidade:
- OpenTelemetry
- Grafana
- métricas de pool e latência

Ferramenta de carga:
- k6

Executor utilizado:
- `ramping-arrival-rate` (iterações por segundo)

Configuração do teste:
```javascript
export const options = {
  scenarios: {
    capacity_rps: {
      executor: "ramping-arrival-rate",
      startRate: 10,
      timeUnit: "1s",
      preAllocatedVUs: 200,
      maxVUs: 800,
      stages: [
        { duration: "30s", target: 200 },
        { duration: "1m",  target: 200 },

        { duration: "30s", target: 330 },
        { duration: "1m",  target: 330 },

        { duration: "30s", target: 400 },
        { duration: "1m",  target: 400 },

        { duration: "30s", target: 500 },
        { duration: "1m",  target: 500 },
      ],
      gracefulStop: "30s",
    },
  }
}
```

## 3. Resultados
Durante o teste foram executadas:
```text
151.598 requisições
≈ 530 req/s média durante o teste
```
Sem interrupções de iteração.
```text
31.571 iterações completas
0 iterações interrompidas
```

## 4. Latência
Métricas de latência observadas:
- avg ~77 ms
- median ~20 ms
- p90 ~247 ms
- p95 ~334 ms
- max ~1.84 s

Latência permaneceu estável mesmo sob carga crescente.

## 5. Taxa de erro
`http_req_failed: 2.50%`

Distribuição:
- deposit
- transfer

Esses erros são principalmente rejeições de negócio, incluindo:
- insufficient funds
- conflitos de concorrência (optimistic locking)

Não foram observados:
- timeouts de pool
- falhas sistêmicas críticas
- interrupções de execução

## 6. Comportamento do Pool de Conexões
Pool configurado com: `maximumPoolSize = 20`

Observação durante estágio 330 rps:
Inicial:
```text
idle = 15
active = 5
```

Após estabilização:
```text
active = 15
idle = 5
```

Depois voltou para:
```text
idle = 17
active = 3
```

O sistema permaneceu estável por aproximadamente 1m15s nesse patamar.

Durante o ramp para 350 rps, começou a surgir: `pending connections`

Esse comportamento indica formação de fila de acquire no pool, sinalizando proximidade da saturação.

## 7. Capacidade Sustentável
Baseado nos testes:

- 200 = estável, latência baixa, erro ~0.5%
- 330 = estável, latência média, erro ~1.5%
- 400 = início de instabilidade, latência sobe, erro ~4%

Capacidade operacional segura estimada:
> 330 – 400 requests por segundo

Nesse patamar o sistema apresenta:
- latência estável
- ausência de timeouts
- ausência de fila de conexões
- taxa de erro limitada a regras de negócio

## 8. Gargalo Identificado
O principal gargalo identificado foi:

**banco de dados / pool de conexões**

Sinais observados:
- aumento de conexões ativas
- redução de conexões idle
- surgimento de pending ao aproximar 350 rps

Conclusão:
> O sistema é DB-bound, não CPU-bound.

A aplicação continua estável até atingir o limite de concorrência permitido pelo banco e pool de conexões.

## 9. Confiabilidade
O sistema atualmente garante:
- consistência transacional
- atomicidade em débito/crédito
- idempotência via constraint única
- classificação de erros (negócio vs sistema)
- observabilidade via métricas e logs estruturados

## 10. Limitações atuais
Arquitetura atual apresenta algumas limitações naturais:
- processamento totalmente síncrono
- ausência de fila/event streaming
- ausência de ledger de auditoria
- escalabilidade limitada pela capacidade do banco

## 11. Testado em AWS ECS
Arquitetura
- RDS PostgreSQL com tipo de instância db.t4g.medium
- ElasticCache Redis com tipo de instância cache.t4g.small e 1 cluster
- ECS Fargate com tipo de instância `fargate`, 2 vCPU, 4gb de RAM e 2 tasks rodando a aplicação
- Teste rodando localmente na minha máquina (Ryzen 5 5600G, 32GB de RAM)
    ```javascript
    preAllocatedVUs: 200,
      maxVUs: 800,
      stages: [
        { duration: "1m", target: 200 },
        { duration: "1m", target: 300 },
        { duration: "1m", target: 400 },
        { duration: "1m", target: 500 },
        { duration: "1m", target: 600 },
        { duration: "3m", target: 600 },
      ],
  ```
- Faltando em torno de 1m e 15s para o fim dos 600 target, o pending subiu para 58 no banco de dados e a latência começou a subir, o teste foi interrompido nesse ponto.
- Suportou 470 ~ 500 rps
  - avg ~ 170ms
  - p95 ~ 200ms
  - p99 ~ 450ms

## 12. Conclusão
O sistema demonstrou capacidade de processar:
> 330 – 400 requisições por segundo de forma sustentável

Com:
- p95 ~334ms
- latência estável
- ausência de timeouts
- erros limitados a regras de negócio

O principal limite atual está no banco de dados e pool de conexões, indicando que futuras melhorias devem focar em reduzir round-trips ao banco e desacoplar processamento síncrono.
