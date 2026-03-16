import http from "k6/http";
import { sleep, check, group } from "k6";
import { Trend, Rate, Counter } from "k6/metrics";
import { uuidv4 } from "https://jslib.k6.io/k6-utils/1.2.0/index.js";

import { getBaseUrl } from "./environments.js";

const transferDuration = new Trend("transfer_duration", true);
const errorRate = new Rate("errors");

const transferOptimistic409Rate = new Rate("transfer_optimistic_409_rate");
const transferOptimistic409Count = new Counter("transfer_optimistic_409_count");

const transferInsufficient422Rate = new Rate("transfer_insufficient_422_rate");
const transferInsufficient422Count = new Counter("transfer_insufficient_422_count");

const transfer5xxRate = new Rate("transfer_5xx_rate");

export const options = {
  setupTimeout: "15m",
  scenarios: {
    capacity_rps: {
      executor: "ramping-arrival-rate",
      startRate: 10,
      timeUnit: "1s",
      preAllocatedVUs: 200,
      maxVUs: 800,
      stages: [
        { duration: "1m", target: 200 },
        { duration: "1m", target: 300 },
        { duration: "1m", target: 400 },
        { duration: "1m", target: 500 },
        { duration: "1m", target: 600 },
        { duration: "3m", target: 600 },
      ],
//      stages: [
//        { duration: "30s", target: 200 },
//        { duration: "30s", target: 300 },
//        { duration: "30s", target: 400 },
//        { duration: "2m", target: 400 },
//      ],
      gracefulStop: "30s",
    },
  },

  thresholds: {
    errors: ["rate<0.01"],
    dropped_iterations: ["count==0"],

    "http_req_duration{name:transfer}": ["p(95)<500"],
    "http_req_failed{name:transfer}": ["rate<0.01"],

    transfer_duration: ["p(95)<500"],
    transfer_optimistic_409_rate: ["rate<0.01"],
    transfer_insufficient_422_rate: ["rate<0.01"],
    transfer_5xx_rate: ["rate<0.005"],
  },
};

const BASE_URL = getBaseUrl();
const INITIAL_BALANCE = "100000.00";
const TOTAL_ACCOUNTS = 5000;
const BATCH_SIZE = 200; // teste 50, 100, 200
//const BATCH_SIZE = 100; // teste 50, 100, 200

function chunkArray(array, size) {
  const chunks = [];
  for (let i = 0; i < array.length; i += size) {
    chunks.push(array.slice(i, i + size));
  }
  return chunks;
}

function buildCreateAccountRequest(userId) {
  return {
    method: "POST",
    url: `${BASE_URL}/v1/accounts`,
    body: JSON.stringify({ user_id: userId }),
    params: {
      headers: {
        "Content-Type": "application/json",
        "x-idempotency-key": uuidv4(),
      },
      tags: { name: "create_account" },
    },
  };
}

function buildCreatePixKeyRequest(accountId, email) {
  return {
    method: "POST",
    url: `${BASE_URL}/v1/pix-keys`,
    body: JSON.stringify({
      type: "EMAIL",
      value: email,
      account_id: accountId,
    }),
    params: {
      headers: {
        "Content-Type": "application/json",
        "x-idempotency-key": uuidv4(),
      },
      tags: { name: "create_pix_key" },
    },
  };
}

function buildPrefundRequest(email, amount) {
  return {
    method: "POST",
    url: `${BASE_URL}/v1/transactions/deposit`,
    body: JSON.stringify({
      pix_key: email,
      pix_key_type: "EMAIL",
      source: "external",
      amount,
    }),
    params: {
      headers: {
        "Content-Type": "application/json",
        "x-idempotency-key": uuidv4(),
      },
      tags: { name: "prefund_deposit" },
    },
  };
}

export function setup() {
  const runId = Date.now();
  const seedAccounts = Array.from({ length: TOTAL_ACCOUNTS }, (_, i) => ({
    idx: i,
    userId: uuidv4(),
    email: `user-${i}-${runId}@mail.com`,
    accountId: null,
  }));

  const chunks = chunkArray(seedAccounts, BATCH_SIZE);

  for (const batch of chunks) {
    const requests = batch.map((item) => buildCreateAccountRequest(item.userId));
    const responses = http.batch(requests);

    responses.forEach((res, index) => {
      const ok = check(res, {
        "account created": (r) => r.status === 201,
      });

      if (!ok) {
//        throw new Error(
//          `failed to create account: status=${res.status} body=${res.body}`
//        );
      }

      batch[index].accountId = res.json("id");
    });
  }

  for (const batch of chunks) {
    const requests = batch.map((item) =>
      buildCreatePixKeyRequest(item.accountId, item.email)
    );

    const responses = http.batch(requests);

    responses.forEach((res) => {
      const ok = check(res, {
        "pix key created": (r) => r.status === 201,
      });

      if (!ok) {
//        throw new Error(
//          `failed to create pix key: status=${res.status} body=${res.body}`
//        );
      }
    });
  }

  for (const batch of chunks) {
    const requests = batch.map((item) =>
      buildPrefundRequest(item.email, INITIAL_BALANCE)
    );

    const responses = http.batch(requests);

    responses.forEach((res) => {
      const ok = check(res, {
        "prefund deposit ok": (r) => r.status === 201,
      });

      if (!ok) {
//        throw new Error(
//          `failed to prefund account: status=${res.status} body=${res.body}`
//        );
      }
    });
  }

  return {
    accounts: seedAccounts.map(({ accountId, email }) => ({ accountId, email })),
  };
}

function randomAmount() {
  return (Math.random() * 100 + 1).toFixed(2);
}

function getBalance(accountId) {
  const res = http.get(`${BASE_URL}/v1/accounts/${accountId}`, {
    tags: { name: "get_balance" },
  });

  if (res.status !== 200) return null;
  return parseFloat(res.json("balance"));
}

function recordTransferMetrics(res) {
  const is409 = res.status === 409;
  const is422 = res.status === 422;
  const is5xx = res.status >= 500;

  transferOptimistic409Rate.add(is409);
  if (is409) transferOptimistic409Count.add(1);

  transferInsufficient422Rate.add(is422);
  if (is422) transferInsufficient422Count.add(1);

  transfer5xxRate.add(is5xx);
}

export default function (data) {
  group("Transfer Throughput", () => {
    const accounts = data.accounts;

    const fromIndex = Math.floor(Math.random() * accounts.length);
    let toIndex = Math.floor(Math.random() * accounts.length);

    while (toIndex === fromIndex) {
      toIndex = Math.floor(Math.random() * accounts.length);
    }

    const accountA = accounts[fromIndex];
    const accountB = accounts[toIndex];

    const transferAmount = randomAmount();

    const transferRes = http.post(
      `${BASE_URL}/v1/transactions`,
      JSON.stringify({
        from_account_id: accountA.accountId,
        pix_key: accountB.email,
        pix_key_type: "EMAIL",
        amount: transferAmount,
      }),
      {
        headers: {
          "Content-Type": "application/json",
          "x-idempotency-key": uuidv4(),
        },
        tags: { name: "transfer" },
      }
    );

    recordTransferMetrics(transferRes);
    transferDuration.add(transferRes.timings.duration);

    const transferOk = check(transferRes, {
      "transfer ok": (r) => r.status === 201,
    });

    errorRate.add(!transferOk);

    if (Math.random() < 0.01) {
      const balanceA = getBalance(accountA.accountId);
      const balanceB = getBalance(accountB.accountId);

      check(null, {
        "sample balance A valid": () => balanceA === null || balanceA >= 0,
        "sample balance B valid": () => balanceB === null || balanceB >= 0,
      });
    }
  });

  sleep(0.001);
}