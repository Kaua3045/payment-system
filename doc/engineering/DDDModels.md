# Account
```
id
user_id
balance
status
version
created_at
updated_at
```
##### Decisões:
- `user_id` É o identificador do usuário dono da conta que vai referenciar a tabela de usuários e deve ser único.
- `balance` É um estado derivado (cache) das transações associadas a essa conta.
- `status` Pode ser `ACTIVE`, `BLOCKED` ou `CLOSED`.
- A fonte de verdade do balance são as transações associadas a essa conta.
- `version` Usado para controle de concorrência otimista.

# PixKey
```
id
type
value
account_id
status
created_at
updated_at
```
##### Decisões:
- `type` Pode ser `CPF`, `CNPJ`, `EMAIL`, `PHONE` ou `RANDOM`.
- `value` É o valor da chave pix, deve ser único.
- `account_id` Referencia a conta associada a essa chave pix.
- `status` Pode ser `ACTIVE`, `INACTIVE` ou `DELETED`.
- Uma conta pode ter múltiplas chaves pix associadas.
- Chaves pix são usadas para identificar contas em transações.
- Chaves pix podem ser ativadas ou desativadas sem serem deletadas.
- Garantir que o `RANDOM` seja gerado de forma única e segura. No backend

# Transaction
```
id
from_account_id
to_account_id
amount
status
idempotency_key
created_at
```
- `from_account_id` Referencia a conta de origem da transação.
- `to_account_id` Referencia a conta de destino da transação.
- `amount` É o valor monetário da transação.
- `status` Pode ser `PENDING`, `COMPLETED` ou `FAILED`.
- `idempotency_key` Usado para garantir que transações duplicadas não sejam processadas mais de uma vez.
- Transações representam transferências de fundos entre contas.