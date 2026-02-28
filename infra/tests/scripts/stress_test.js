import http from 'k6/http';
import { sleep, check, group, fail } from 'k6';
import { Trend } from 'k6/metrics';
import { uuidv4 } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';

import { getBaseUrl } from './environments.js';
import { getThresholds } from './thresholds.js';

const transferDuration = new Trend('transfer_duration', true);

export const options = {
    scenarios: {
        stress_test: {
            executor: 'ramping-vus',
            startVUs: 1,
            stages: [
                { duration: '1m', target: 50 },
                { duration: '1m', target: 100 },
                { duration: '1m', target: 200 },
                { duration: '1m', target: 400 },
                { duration: '1m', target: 800 }, // TODO derrubou o DB connections
                { duration: '2m', target: 0 },
            ],
        },
    },

    thresholds: getThresholds(),
};

const BASE_URL = getBaseUrl();

function randomAmount() {
  return (Math.random() * 1000 + 100).toFixed(2); // mínimo 100
}

function randomEmail() {
  return `user-${__VU}-${__ITER}-${Date.now()}@mail.com`
}

function createAccount() {
  const userId = uuidv4();
  const payload = JSON.stringify({ user_id: userId });

  const res = http.post(`${BASE_URL}/v1/accounts`, payload, {
    headers: {
      'Content-Type': 'application/json',
      'x-idempotency-key': uuidv4(),
    },
    tags: { name: 'create_account' },
  });

  const is201 = res.status === 201;
    const is422 = res.status === 422;

    check(res, {
      'account created (201)': () => is201,
      'idempotency hit (422)': () =>
        is422 && res.body.includes('idempotency key already exists'),
    });

    if (!is201) {
      console.error(`Unexpected status: ${res.status} | body: ${res.body}`);
      return null;
    }

    const id = res.json('id');

    if (!id) {
      console.error(`Missing id in response: ${res.body}`);
      return null;
    }

    return id;
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
    tags: { name: 'create_pix_key' },
  });

  check(res, {
      'pix key created (201)': (r) => r.status === 201,
      'idempotency hit (422)': (r) =>
          r.status === 422 &&
          r.body.includes('idempotency key already exists'),
  });

    return email;
}

function getBalance(accountId) {
  const res = http.get(`${BASE_URL}/v1/accounts/${accountId}`, {
    tags: { name: 'get_balance' },
  });

  check(res, { 'balance fetched': r => r.status === 200 });

  return parseFloat(res.json('balance'));
}

export default function () {
  group('Full Transfer Flow', () => {

    const accountA = createAccount();

    if (!accountA) return; // Skip if account creation failed

    const emailA = randomEmail();
    const pixA = createPixKey(accountA, emailA);

    const accountB = createAccount();

    if (!accountB) return; // Skip if account creation failed

    const emailB = randomEmail();
    const pixB = createPixKey(accountB, emailB);

    const depositAmount = parseFloat(randomAmount());

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
      tags: { name: 'deposit' },
    });

    check(depositRes, { 'deposit created': (r) => r.status === 201, 'idempotency hit (422)': (r) =>
                                                                        r.status === 422 &&
                                                                        r.body.includes('idempotency key already exists') });

    const initialBalanceA = getBalance(accountA);

    const transferAmount = parseFloat(depositAmount) / 2;

    const transferPayload = JSON.stringify({
      from_account_id: accountA,
      pix_key: pixB,
      pix_key_type: 'EMAIL',
      amount: transferAmount.toFixed(2),
    });

    const transferRes = http.post(`${BASE_URL}/v1/transactions`, transferPayload, {
      headers: {
        'Content-Type': 'application/json',
        'x-idempotency-key': uuidv4(),
      },
      tags: { name: 'transfer' },
    });

    transferDuration.add(transferRes.timings.duration);

    check(transferRes, { 'transfer created': (r) => r.status === 201, 'idempotency hit (422)': (r) =>
                                                                          r.status === 422 &&
                                                                          r.body.includes('idempotency key already exists') });

     const finalBalanceA = getBalance(accountA);
     const finalBalanceB = getBalance(accountB);

    check(null, {
       'balance A decreased correctly': () =>
         Math.abs(finalBalanceA - (initialBalanceA - transferAmount)) < 0.01,

       'balance B increased correctly': () =>
         Math.abs(finalBalanceB - transferAmount) < 0.01,
    });
  });

  sleep(1);
}