# Payment System

## Ferramentas utilizadas
![Java](https://img.shields.io/badge/java-%23ED8B00.svg?style=for-the-badge&logo=java&logoColor=white)
![Spring](https://img.shields.io/badge/spring-%236DB33F.svg?style=for-the-badge&logo=spring&logoColor=white)
![Gradle](https://img.shields.io/badge/Gradle-02303A.svg?style=for-the-badge&logo=Gradle&logoColor=white)
![Docker](https://img.shields.io/badge/docker-%230db7ed.svg?style=for-the-badge&logo=docker&logoColor=white)
![GitHub Actions](https://img.shields.io/badge/github%20actions-%232671E5.svg?style=for-the-badge&logo=githubactions&logoColor=white)

## Sobre

Um sistema estilo banco, para processamento de pagamentos, com criação de contas, depósitos, saques e transferências entre contas.

- Porquê decidiu fazer esse projeto?
  - Decidi fazer esse projeto para aprender mais sobre clean architecture, DDD e SOLID. Além de aprender mais sobre o Spring e o Java. Deploys com k8s, docker e argoCD.

- Quais foram os desafios de implementá-lo?
  - ...

- O que eu aprendi com ele?
  - ...

## Tabela de conteúdos

- [Arquitetura](#arquitetura)
- [Requsitos para rodar o projeto](#requisitos)
- [Performance](#performance)
- [Instruções para executar o projeto](#instruções-para-executar-o-projeto)
- [Contribua com o projeto](#contribuindo-com-o-projeto)
- [Changelog](#changelog)
- [Observabilidade](/doc/observability.md)
- [Idempotência](/doc/idempotency-key.md)

## Arquitetura

![Circulo da clean architecture](doc/imagens/clean-arch-circle)

**Camadas da aplicação**

*Domain, é a camada onde se encontra as regras de negócio, validações e as interfaces gateways (abstração dos métodos do banco dedados, são usadas para remover o acomplamento com o banco de dados)*

*Application, é a camada que contem todos os casos de uso (criar um usuário, pegar um usuário pelo id, atualizar um usuário, deletar um usuário, esse é famoso CRUD) e contem a integração com o gateway do banco de dados*

*Infrastructure, é a camada responsável por conectar tudo, o usuário com a application e domain layer, contem a conexão com o banco de dados, entidades do banco e as rotas*

## Performance
Foram realizados testes de carga para avaliar o comportamento do sistema sob alta concorrência utilizando **k6**.

### Performance & Reliability Report
- Cenário: Transferências Pix
- Ambiente: Local (Docker) com PostgreSQL e HikariCP
- Métricas: Grafana + OpenTelemetry
- Resultados:
- Capacidade sustentável: **330 it/s** (800 RPS no mix deposit+transfer)
- Ponto de saturação: **400 it/s** (pool de conexões)

Relatório completo de performance:
[Performance & Reliability Report](doc/performance/performance-and-reliability-report.md)

### Spike Test
- Até **500 usuários concorrentes**
- Aproximadamente **580 requisições por segundo**
- Métricas coletadas via **Grafana e OpenTelemetry**
- Monitoramento de **PostgreSQL e HikariCP**
- Validação de consistência com **optimistic locking**

Relatório completo de performance:
[Spike Test de Transferências](doc/performance/spike-transfer.md)

### Capacity Test
- Métricas coletadas via **Grafana e OpenTelemetry**
- Monitoramento de **PostgreSQL e HikariCP**
- Capacidade aproximada: **350-410 it/s** (800 RPS no mix deposit+transfer)

Relatório completo de performance:
[Capacity Test de Transferências](doc/performance/capacity-transfer.md)

## Requisitos para rodar o projeto

1. Docker e docker-compose
2. Java e JDK 21

## Instruções para executar o projeto

### 1. Rodando localmente (modo dev)

1. Baixe o projeto e instale as dependências:
```bash
git clone https://github.com/Kaua3045/payment-system.git
cd payment-system
./gradlew build
```

2. Configure o ambiente:
```bash
   cp .env.example .env
```

3. Inicie a aplicação:
```bash
./gradlew bootRun
```
- URL base: http://localhost:8081/api/

### Rodando com Docker (sandbox)
1. Configure o .env como no passo anterior.
2. Rode os containers:
```bash
  chmod -x sandbox-script.sh
  ./sandbox-script.sh up
```
- URL base: http://localhost:8081/api/
- `./sandbox-script.sh up` Inicializa as apps
- `./sandbox-script.sh down` Para as apps
- `./sandbox-script.sh restart` Reinicializa as apps
- `./sandbox-script.sh clean` Desliga as apps e limpa todos os volumes

## Contribuindo com o projeto

Para contribuir com o projeto, veja mais informações em [CONTRIBUTING](doc/CONTRIBUTING.md)

## Changelog

Para ver as últimas alterações do projeto, acesse [AQUI](doc/changelog.md)

## Configurações para dev
After cloning project add commit-msg hook in your git path
```shell
    git config core.hooksPath .githooks
```
