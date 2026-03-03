import http from "k6/http";
import { sleep, check, group } from "k6";
import { Trend, Rate } from "k6/metrics";
import { uuidv4 } from "https://jslib.k6.io/k6-utils/1.2.0/index.js";

import { getBaseUrl } from "./environments.js";

const transferDuration = new Trend("transfer_duration", true);
const errorRate = new Rate("errors");

export const options = {
  scenarios: {
    capacity_rps: {
      executor: "ramping-arrival-rate",
      startRate: 10,
      timeUnit: "1s",
      preAllocatedVUs: 200,
      maxVUs: 800,
      stages: [
        { duration: "30s", target: 200 },
        { duration: "1m",  target: 200 },

        { duration: "30s", target: 350 },
        { duration: "1m",  target: 350 },

        { duration: "30s", target: 400 }, // TODO: 400 target db connections timeout
        { duration: "1m",  target: 400 },

        { duration: "30s", target: 500 },
        { duration: "1m",  target: 500 },
      ],
      gracefulStop: "30s",
    },
  },

  thresholds: {
    errors: ["rate<0.01"],
    dropped_iterations: ["count==0"],

    "http_req_duration{name:deposit}": ["p(95)<400"],
    "http_req_duration{name:transfer}": ["p(95)<500"],

    "http_req_failed{name:deposit}": ["rate<0.01"],
    "http_req_failed{name:transfer}": ["rate<0.01"],

    transfer_duration: ["p(95)<500"],
  },
};

const BASE_URL = getBaseUrl();

export function setup() {
  const accounts = [];

  for (let i = 0; i < 4000; i++) {
    const accountId = createAccount();
    const email = `user-${i}-${Date.now()}@mail.com`;
    createPixKey(accountId, email);
    accounts.push({ accountId, email });
  }

  return { accounts };
}

function randomAmount() {
  return (Math.random() * 1000 + 100).toFixed(2);
}

function createAccount() {
  const res = http.post(
    `${BASE_URL}/v1/accounts`,
    JSON.stringify({ user_id: uuidv4() }),
    {
      headers: {
        "Content-Type": "application/json",
        "x-idempotency-key": uuidv4(),
      },
      tags: { name: "create_account" },
    }
  );

  check(res, { "account created": (r) => r.status === 201 });
  if (res.status !== 201) return null;
  return res.json("id");
}

function createPixKey(accountId, email) {
  const res = http.post(
    `${BASE_URL}/v1/pix-keys`,
    JSON.stringify({
      type: "EMAIL",
      value: email,
      account_id: accountId,
    }),
    {
      headers: {
        "Content-Type": "application/json",
        "x-idempotency-key": uuidv4(),
      },
      tags: { name: "create_pix_key" },
    }
  );

  check(res, { "pix key created": (r) => r.status === 201 });
}

function getBalance(accountId) {
  const res = http.get(`${BASE_URL}/v1/accounts/${accountId}`, {
    tags: { name: "get_balance" },
  });

  if (res.status !== 200) return null;
  return parseFloat(res.json("balance"));
}

export default function (data) {
  group("Transfer Throughput", () => {
    const accounts = data.accounts;

    const fromIndex = (__VU + __ITER) % accounts.length;
    let toIndex =
      (fromIndex + 1 + Math.floor(Math.random() * (accounts.length - 1))) %
      accounts.length;

    const accountA = accounts[fromIndex];
    const accountB = accounts[toIndex];

    const depositAmount = parseFloat(randomAmount());

    const depositRes = http.post(
      `${BASE_URL}/v1/transactions/deposit`,
      JSON.stringify({
        pix_key: accountA.email,
        pix_key_type: "EMAIL",
        source: "external",
        amount: depositAmount,
      }),
      {
        headers: {
          "Content-Type": "application/json",
          "x-idempotency-key": uuidv4(),
        },
        tags: { name: "deposit" },
      }
    );

    const depositOk = check(depositRes, { "deposit ok": (r) => r.status === 201 });

    const transferAmount = depositAmount / 2;

    const transferRes = http.post(
      `${BASE_URL}/v1/transactions`,
      JSON.stringify({
        from_account_id: accountA.accountId,
        pix_key: accountB.email,
        pix_key_type: "EMAIL",
        amount: transferAmount.toFixed(2),
      }),
      {
        headers: {
          "Content-Type": "application/json",
          "x-idempotency-key": uuidv4(),
        },
        tags: { name: "transfer" },
      }
    );

    transferDuration.add(transferRes.timings.duration);

    const transferOk = check(transferRes, { "transfer ok": (r) => r.status === 201 });

    errorRate.add(!(depositOk && transferOk));

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