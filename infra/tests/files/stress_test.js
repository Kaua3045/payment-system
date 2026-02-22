import http from 'k6/http';
import { sleep, check, group } from 'k6';
import { uuidv4 } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js'; // biblioteca para UUID

export const options = {
  vus: 50,           // 50 usuários virtuais
  duration: '1m',    // rodando por 1 minuto
  thresholds: {
    http_req_duration: ['p(95)<700'], // 95% requisições < 700ms
    'http_req_failed': ['rate<0.01'], // <1% de erro
  },
};

const BASE_URL = 'http://app:8081/api'; // Ajuste para sua app

function randomAmount() {
  return (Math.random() * 1000).toFixed(2);
}

function randomEmail() {
  return `user${Math.floor(Math.random() * 100000)}@mail.com`;
}

export default function () {

  group('Account + PIX + Deposit + Transaction Flow', () => {
    // Gerar user_id externo
    const userId = uuidv4();

    // Criar conta usando user_id externo
    const accountPayload = JSON.stringify({ user_id: userId });
    const accountRes = http.post(`${BASE_URL}/v1/accounts`, accountPayload, { headers: { 'Content-Type': 'application/json', 'x-idempotency-key': uuidv4() } });
    check(accountRes, { 'account created': (r) => r.status === 201 });
    const accountId = accountRes.json('id');

    const email = randomEmail();
    console.log(`Generated email: ${email}`); // Para debug

    // Criar PIX key
    const pixPayload = JSON.stringify({
      type: 'EMAIL',
      value: email,
      account_id: accountId
    });
    const pixRes = http.post(`${BASE_URL}/v1/pix-keys`, pixPayload, { headers: { 'Content-Type': 'application/json', 'x-idempotency-key': uuidv4() } });
    check(pixRes, { 'pix key created': (r) => r.status === 201 });
    const pixKey = pixRes.json('value');

    // Criar depósito
    const depositPayload = JSON.stringify({
      pix_key: email,
      pix_key_type: 'EMAIL',
      source: 'external',
      amount: randomAmount()
    });
    const depositRes = http.post(`${BASE_URL}/v1/transactions/deposit`, depositPayload, { headers: { 'Content-Type': 'application/json', 'x-idempotency-key': uuidv4() } });
    check(depositRes, { 'deposit created': (r) => r.status === 201 });
  });

  sleep(1);
}