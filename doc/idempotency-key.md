# 📌 Idempotency Key indices
- [📌 O que é Idempotency Key?](#-o-que-é-idempotency-key)
- [📌 Como Funciona?](#-como-funciona)
- [📌 Anotação `@IdempotencyKey`](#-anotação-idempotencykey)
  - [📌 Parâmetros da Anotação](#-parâmetros-da-anotação)
  - [📌 Constantes Padrão](#-constantes-padrão)
- [📌 Benefícios da Idempotência](#-benefícios-da-idempotência)
- [📌 Conclusão](#-conclusão)

## 📌 O que é Idempotency Key?

A Idempotency Key é um mecanismo utilizado para evitar 
que requisições duplicadas causem efeitos colaterais 
indesejados. Ele garante que operações sejam executadas 
apenas uma vez, mesmo que a mesma requisição seja enviada 
múltiplas vezes.

## 📌 Como Funciona?

- Antes de processar uma requisição, o sistema verifica se uma chave de idempotência (x-idempotency-key) já foi utilizada.
- Se a chave ainda não existir, a requisição é processada e o resultado é armazenado.
- Se a mesma chave for recebida novamente dentro do período definido (TTL - Time To Live), a resposta previamente armazenada é retornada sem reprocessar a operação.

## 📌 Anotação `@IdempotencyKey`

A anotação `@IdempotencyKey` permite definir que um método deve ser tratado como idempotente.
Essa anotação só pode ser utilizada em métodos de classes anotadas com `@RestController`.
Para usar em outros lugares é preciso fazer isso de forma programática.

```java
@RestController
@RequestMapping("/api")
public class PaymentService {
    
    @IdempotencyKey(ttl = 5, timeUnit = TimeUnit.MINUTES)
    @PostMapping("/payments")
    public PaymentResponse processPayment(PaymentRequest request) {
        // Processa o pagamento
        return new PaymentResponse("success");
    }
}
```

### 📌 Parâmetros da Anotação

| Parâmetro | Descrição                                                      |
|-----------|----------------------------------------------------------------|
| ttl       | Tempo de expiração da chave de idempotência. Valor padrão: `1` |
| timeUnit  | Unidade de tempo para o TTL. Valor padrão: `TimeUnit.HOURS`    |

### 📌 Constantes Padrão

| Constante                     | Valor                    |
|-------------------------------|--------------------------|
| `IDEMPOTENCY_KEY_HEADER`      | `x-idempotency-key`      |
| `IDEMPOTENCY_RESPONSE_HEADER` | `x-idempotency-response` |

## 📌 Benefícios da Idempotência

✅ Evita duplicidade em requisições concorrentes.

✅ Melhora a confiabilidade de APIs e serviços distribuídos.

✅ Garante consistência em sistemas assíncronos.

## 📌 Conclusão

A implementação de Idempotency Keys reduz falhas em operações sensíveis e melhora a confiabilidade do sistema, garantindo que cada requisição seja processada apenas uma vez dentro do período definido.
