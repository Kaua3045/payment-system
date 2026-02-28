import http from 'k6/http';
import { sleep, check, group, fail } from 'k6';
import { Trend } from 'k6/metrics';
import { uuidv4 } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';

import { getBaseUrl } from './environments.js';
import { getThresholds } from './thresholds.js';

const transferDuration = new Trend('transfer_duration', true);

export const options = {
    scenarios: {
        soak: {
            executor: 'constant-vus',
            vus: 100, // TODO estoura timeout do DB com mais de 100 VUs, investigar depois
            duration: '2h',
        },
    },

    thresholds: getThresholds(),
};

const BASE_URL = getBaseUrl();

export function setup() {
  const accounts = [];

  for (let i = 0; i < 300; i++) {
    const accountId = createAccount();
    const email = `user-${i}-${Date.now()}@mail.com`;
    createPixKey(accountId, email);

    accounts.push({
      accountId,
      email,
    });
  }

  return { accounts };
}

function randomAmount() {
  return (Math.random() * 1000 + 100).toFixed(2); // mínimo 100
}

function randomEmail() {
  return `user-${__VU}-${__ITER}-${Date.now()}@mail.com`
}

function createAccount() {
  const payload = JSON.stringify({
    user_id: uuidv4(),
  });

  const res = http.post(`${BASE_URL}/v1/accounts`, payload, {
    headers: {
      'Content-Type': 'application/json',
      'x-idempotency-key': uuidv4(),
    },
    tags: { name: 'create_account' },
  });

  if (res.status !== 201) {
    fail(`Account creation failed: ${res.status} - ${res.body}`);
  }

  const id = res.json('id');

  if (!id) {
    fail(`Missing account id in response: ${res.body}`);
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

  if (res.status !== 201) {
    fail(`Pix key creation failed: ${res.status} - ${res.body}`);
  }
}

function getBalance(accountId) {
  const res = http.get(`${BASE_URL}/v1/accounts/${accountId}`, {
    tags: { name: 'get_balance' },
  });

  if (res.status !== 200) {
    fail(`Balance fetch failed: ${res.status} - ${res.body}`);
  }

  return parseFloat(res.json('balance'));
}

export default function (data) {
  group('Soak Transfer Flow', () => {

    const accounts = data.accounts;
    const fromIndex = Math.floor(Math.random() * accounts.length);
    let toIndex = Math.floor(Math.random() * accounts.length);

    while (toIndex === fromIndex) {
      toIndex = Math.floor(Math.random() * accounts.length);
    }

    const accountA = accounts[fromIndex];
    const accountB = accounts[toIndex];

    const depositAmount = parseFloat(randomAmount());

    const depositPayload = JSON.stringify({
      pix_key: accountA.email,
      pix_key_type: 'EMAIL',
      source: 'external',
      amount: depositAmount,
    });

    const depositRes = http.post(
      `${BASE_URL}/v1/transactions/deposit`,
      depositPayload,
      {
        headers: {
          'Content-Type': 'application/json',
          'x-idempotency-key': uuidv4(),
        },
        tags: { name: 'deposit' },
      }
    );

    check(depositRes, {
      'deposit created': (r) => r.status === 201,
    });

    const initialBalanceA = getBalance(accountA.accountId);
    const transferAmount = depositAmount / 2;

    const transferPayload = JSON.stringify({
      from_account_id: accountA.accountId,
      pix_key: accountB.email,
      pix_key_type: 'EMAIL',
      amount: transferAmount.toFixed(2),
    });

    const transferRes = http.post(
      `${BASE_URL}/v1/transactions`,
      transferPayload,
      {
        headers: {
          'Content-Type': 'application/json',
          'x-idempotency-key': uuidv4(),
        },
        tags: { name: 'transfer' },
      }
    );

    transferDuration.add(transferRes.timings.duration);

    check(transferRes, {
      'transfer created': (r) => r.status === 201,
    });

    const finalBalanceA = getBalance(accountA.accountId);
    const finalBalanceB = getBalance(accountB.accountId);

    check(null, {
      'balance A decreased correctly': () =>
        Math.abs(finalBalanceA - (initialBalanceA - transferAmount)) < 0.01,

      'balance B increased correctly': () =>
        finalBalanceB >= transferAmount,
    });
  });

  sleep(1);
}