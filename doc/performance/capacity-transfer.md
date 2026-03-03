# Capacity Test — limite por iterações/s

Este documento descreve o **capacity test** do sistema para encontrar o limite de throughput (iterações/segundo) antes de ocorrer saturação do pool de conexões (HikariCP) e degradação significativa de latência/erros.

> **Definição importante:** neste teste, **1 iteração = 2 requisições principais** (Deposit + Transfer).  
> Portanto: `400 it/s ≈ 800 RPS` (no mix deposit+transfer).

## Objetivo

- Encontrar o **“joelho da curva”** (início de saturação) do sistema em **it/s**.
- Identificar o ponto de **colapso** (fila explode, timeouts e erros sobem).
- Correlacionar capacidade com métricas do **HikariCP** e latência HTTP.
- Garantir que o limite observado é do sistema (app/DB) e **não** do k6.

## Ambiente
### Infraestrutura
Ambiente local usando Docker:
- Application: Spring Boot
- Database: PostgreSQL 17
- Pool: HikariCP
- Ryzen 5 5600G, 32GB RAM, GTX 1660 Super, SSD NVMe

### Configuração atual:
```yaml
hikari:
  maximum-pool-size: 20
  minimum-idle: 10
  connection-timeout: 2000ms (2s)
```
- Estratégia de concorrência: **Virtual Threads** (Java).

> Observação: Virtual threads permitem muita concorrência no app, mas **não aumentam** o número de conexões disponíveis no DB. O recurso escasso continua sendo o pool.

## Cenário do Teste
### Ferramenta
- **k6** com executor **`ramping-arrival-rate`** (it/s).

### Fluxo por iteração
1. `POST /v1/transactions/deposit`
2. `POST /v1/transactions` (transfer)

Validação leve:
- 1% das iterações faz `GET /v1/accounts/{id}` (somente amostragem, para evitar custo alto no caminho crítico).

## Script do Teste

Arquivo sugerido: `tests/capacity_test.js`

Características principais:
- `ramping-arrival-rate` para “forçar” taxa e medir capacidade.
- `preAllocatedVUs` e `maxVUs` configurados para reduzir chance de limite do k6.
- massa de dados criada no `setup()` (contas + pix keys).

> **Nota:** k6 foi rodado na mesma máquina da app/DB pode distorcer resultados (CPU/IO competindo). Ideal em ambiente real: k6 separado.

## Resultados e Leitura Correta

### 1) Limite do DB pool / fila de acquire

Observado no Grafana:

- Em ~**400 it/s**: `pending` ~ **80+** → já é **saturação**.
- Em **500 it/s**: `pending` ~ **510** + **timeouts** → **colapso**.

Esse comportamento é característico de:

- Pool encosta no teto (**20** conexões)
- Requisições começam a **esperar conexão** (`pending` sobe)
- Latência explode para **~1–3s**
- Erros sobem para **~4–8%**

Conclusão: com `pool=20` (no mix deposit+transfer), o limite “comportado” fica por volta de **300–350 it/s**. Claro com uma margem, já que próximo dos 400 pending já começou a subir.

## Capacidade estimada
Com base no comportamento observado:

- **Capacidade sustentável** (SLO tipo `p95 < 500ms` e `erro < 1%`): **~350–420 it/s**
- **Início de saturação / joelho da curva:** **~400–450 it/s**
- **Colapso:** **≥ 500 it/s** (fila explode, latência 1–3s, erro 4–8%)

> Lembrete: `400 it/s ≈ 800 RPS` (deposit+transfer).