import http from 'k6/http';
import { sleep, check, fail } from 'k6';
import { uuidv4 } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';

import { getBaseUrl } from './environments.js';
import { getThresholds } from './thresholds.js';

const BASE_URL = getBaseUrl();

export const options = {
  scenarios: {
    deadlock: {
      executor: 'constant-arrival-rate',
      rate: 500,           // 500 transactions per second
      timeUnit: '1s',
      duration: '10m',
      preAllocatedVUs: 300,
      maxVUs: 800,
    },
  },
  thresholds: getThresholds(),
};

export function setup() {

  const accountA = createAccount();
  const pixA = `deadlock-A-${Date.now()}@mail.com`;
  createPixKey(accountA, pixA);

  const accountB = createAccount();
  const pixB = `deadlock-B-${Date.now()}@mail.com`;
  createPixKey(accountB, pixB);

  const depositPayload = JSON.stringify({
    pix_key: pixA,
    pix_key_type: 'EMAIL',
    source: 'external',
    amount: "100000.00",
  });

  const depositRes = http.post(
    `${BASE_URL}/v1/transactions/deposit`,
    depositPayload,
    {
      headers: {
        'Content-Type': 'application/json',
        'x-idempotency-key': uuidv4(),
      },
    }
  );

  if (depositRes.status !== 201) {
    fail(`Initial deposit failed: ${depositRes.status} - ${depositRes.body}`);
  }

  return {
    accountA,
    accountB,
    pixB,
  };
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
  });

  if (res.status !== 201) {
    fail(`Account creation failed: ${res.status} - ${res.body}`);
  }

  const id = res.json('id');
  if (!id) fail('Missing account id');

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
  });

  if (res.status !== 201) {
    fail(`Pix key creation failed: ${res.status} - ${res.body}`);
  }
}

export default function (data) {

  const payload = JSON.stringify({
    from_account_id: data.accountA,
    pix_key: data.pixB,
    pix_key_type: 'EMAIL',
    amount: "1.00",
  });

  http.post(`${BASE_URL}/v1/transactions`, payload, {
    headers: {
      'Content-Type': 'application/json',
      'x-idempotency-key': uuidv4(),
    },
    tags: { name: 'deadlock_transfer' },
  });

  sleep(0.05);
}