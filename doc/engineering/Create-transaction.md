# Fluxo de Transferência entre Contas
1. Request entra via API
2. Validação a conta de origem e destino existem
3. Validação do status das contas (devem estar ACTIVE)
4. Verificação de idempotência
5. Validar saldo suficiente na conta de origem
6. Criação da transação com status PENDING
7. Débito e crédito nas contas envolvidas atômicamente
8. Atualização do status da transação para COMPLETED
##### Se ocorrer algum erro durante o processo:
- A transação é marcada como FAILED
- A transação é revertida
- nenhum saldo é alterado nas contas
- Uma mensagem de erro é retornada ao solicitante