import http from 'k6/http';
import { sleep, check, group } from 'k6';
import { uuidv4 } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';

export const options = {
  vus: 50,
  duration: '1m',
  thresholds: {
    http_req_duration: ['p(95)<700'],
    http_req_failed: ['rate<0.01'],
  },
};

const BASE_URL = 'http://app:8081/api';

function randomAmount() {
  return (Math.random() * 1000 + 100).toFixed(2); // mínimo 100
}

function randomEmail() {
  return `user${Math.floor(Math.random() * 1000000)}@mail.com`;
}

function createAccount() {
  const userId = uuidv4();
  const payload = JSON.stringify({ user_id: userId });

  const res = http.post(`${BASE_URL}/v1/accounts`, payload, {
    headers: {
      'Content-Type': 'application/json',
      'x-idempotency-key': uuidv4(),
    },
  });

  check(res, { 'account created': (r) => r.status === 201 });

  return res.json('id');
}

function createPixKey(accountId, email) {
  const payload = JSON.stringify({
    type: 'EMAIL',
    value: email,
    account_id: accountId,
  });

  const res = http.post(`${BASE_URL}/v1/pix-keys`, payload, {
    headers: {
      'Content-Type': 'application/json',
      'x-idempotency-key': uuidv4(),
    },
  });

  check(res, { 'pix key created': (r) => r.status === 201 });

  return email;
}

export default function () {
  group('Full Transfer Flow', () => {

    const accountA = createAccount();
    const emailA = randomEmail();
    const pixA = createPixKey(accountA, emailA);

    const accountB = createAccount();
    const emailB = randomEmail();
    const pixB = createPixKey(accountB, emailB);

    const depositAmount = randomAmount();

    const depositPayload = JSON.stringify({
      pix_key: pixA,
      pix_key_type: 'EMAIL',
      source: 'external',
      amount: depositAmount,
    });

    const depositRes = http.post(`${BASE_URL}/v1/transactions/deposit`, depositPayload, {
      headers: {
        'Content-Type': 'application/json',
        'x-idempotency-key': uuidv4(),
      },
    });

    check(depositRes, { 'deposit created': (r) => r.status === 201 });

    const transferPayload = JSON.stringify({
      from_account_id: accountA,
      pix_key: pixB,
      pix_key_type: 'EMAIL',
      amount: (depositAmount / 2).toFixed(2),
    });

    const transferRes = http.post(`${BASE_URL}/v1/transactions`, transferPayload, {
      headers: {
        'Content-Type': 'application/json',
        'x-idempotency-key': uuidv4(),
      },
    });

    check(transferRes, { 'transfer created': (r) => r.status === 201 });

  });

  sleep(1);
}