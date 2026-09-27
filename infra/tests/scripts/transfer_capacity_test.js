import http from "k6/http";
import { check } from "k6";
import { Trend, Rate, Counter } from "k6/metrics";
import { uuidv4 } from "https://jslib.k6.io/k6-utils/1.2.0/index.js";

import { getBaseUrl } from "./environments.js";
import { createFundedAccounts } from "./transfer_test_data.js";

const BASE_URL = getBaseUrl();

const transferDuration = new Trend("transfer_duration", true);

const transferErrorRate = new Rate("transfer_errors");

const transferOptimistic409Rate = new Rate(
    "transfer_optimistic_409_rate"
);

const transferOptimistic409Count = new Counter(
    "transfer_optimistic_409_count"
);

const transferInsufficient422Rate = new Rate(
    "transfer_insufficient_422_rate"
);

const transferInsufficient422Count = new Counter(
    "transfer_insufficient_422_count"
);

const transfer5xxRate = new Rate("transfer_5xx_rate");

export const options = {
    scenarios: {
        transfer_capacity: {
            executor: "ramping-arrival-rate",

            startRate: 10,

            timeUnit: "1s",

            preAllocatedVUs: 300,

            maxVUs: 800,

            stages: [
                // Warm-up
                { duration: "30s", target: 100 },
                { duration: "1m", target: 100 },

                // 200 req/s
                { duration: "30s", target: 200 },
                { duration: "1m", target: 200 },

                // 300 req/s
                { duration: "30s", target: 300 },
                { duration: "1m", target: 300 },

                // 400 req/s
                { duration: "30s", target: 400 },
                { duration: "1m", target: 400 },

                // // 500 req/s TODO voltar isso depois, mas meu pc nao ta aguentando tudo
                // { duration: "30s", target: 500 },
                // { duration: "1m", target: 500 },
            ],

            gracefulStop: "30s",
        },
    },

    thresholds: {
        transfer_errors: ["rate<0.01"],

        dropped_iterations: ["count==0"],

        "http_req_failed{name:transfer}": ["rate<0.01"],

        "http_req_duration{name:transfer}": [
            "p(95)<500",
        ],

        transfer_duration: [
            "p(95)<500",
        ],

        transfer_5xx_rate: [
            "rate<0.005",
        ],

        transfer_insufficient_422_rate: [
            "rate<0.01",
        ],

        transfer_optimistic_409_rate: [
            "rate<0.01",
        ],
    },
};

export function setup() {
    return {
        accounts: createFundedAccounts(4000),
    };
}

function selectAccounts(accounts) {
    const fromIndex =
        (__VU + __ITER) % accounts.length;

    let toIndex =
        Math.floor(Math.random() * accounts.length);

    while (toIndex === fromIndex) {
        toIndex =
            Math.floor(Math.random() * accounts.length);
    }

    return {
        from: accounts[fromIndex],
        to: accounts[toIndex],
    };
}

function recordMetrics(res) {
    const is409 = res.status === 409;
    const is422 = res.status === 422;
    const is5xx = res.status >= 500;

    transferOptimistic409Rate.add(is409);

    if (is409) {
        transferOptimistic409Count.add(1);
    }

    transferInsufficient422Rate.add(is422);

    if (is422) {
        transferInsufficient422Count.add(1);
    }

    transfer5xxRate.add(is5xx);
}

export default function (data) {
    const { accounts } = data;

    const { from, to } = selectAccounts(accounts);

    const amount = "10.00";

    const res = http.post(
        `${BASE_URL}/v1/transactions`,
        JSON.stringify({
            from_account_id: from.accountId,
            pix_key: to.email,
            pix_key_type: "EMAIL",
            amount,
        }),
        {
            headers: {
                "Content-Type": "application/json",
                "x-idempotency-key": uuidv4(),
            },
            tags: {
                name: "transfer",
            },
        }
    );

    transferDuration.add(res.timings.duration);

    recordMetrics(res);

    const ok = check(res, {
        "transfer ok": (r) => r.status === 201,
    });

    transferErrorRate.add(!ok);
}