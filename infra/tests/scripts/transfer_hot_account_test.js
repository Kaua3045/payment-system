import http from "k6/http";
import { check } from "k6";
import { Trend, Rate, Counter } from "k6/metrics";
import { uuidv4 } from "https://jslib.k6.io/k6-utils/1.2.0/index.js";

import { getBaseUrl } from "./environments.js";

const BASE_URL = getBaseUrl();

const transferDuration = new Trend("transfer_duration", true);

const transferErrors = new Rate("transfer_errors");

const optimistic409Rate = new Rate(
    "transfer_optimistic_409_rate"
);

const optimistic409Count = new Counter(
    "transfer_optimistic_409_count"
);

const transfer5xxRate = new Rate(
    "transfer_5xx_rate"
);

export const options = {
    scenarios: {
        hot_account_contention: {
            executor: "constant-arrival-rate",

            rate: 100,

            timeUnit: "1s",

            duration: "1m",

            preAllocatedVUs: 100,

            maxVUs: 500,

            gracefulStop: "30s",
        },
    },

    thresholds: {
        transfer_5xx_rate: [
            "rate<0.005",
        ],

        "http_req_failed{name:transfer}": [
            "rate<0.5",
        ],
    },
};

export function setup() {
    const source = createAccount(
        "hot-source"
    );

    const destination = createAccount(
        "hot-destination"
    );

    createPixKey(
        destination.accountId,
        destination.email
    );

    deposit(
        source.email,
        "1000000.00"
    );

    return {
        source,
        destination,
    };
}

function createAccount(label) {
    const email =
        `${label}-${Date.now()}-${uuidv4()}@mail.com`;

    const res = http.post(
        `${BASE_URL}/v1/accounts`,
        JSON.stringify({
            user_id: uuidv4(),
        }),
        {
            headers: {
                "Content-Type": "application/json",
                "x-idempotency-key": uuidv4(),
            },
            tags: {
                name: "setup_create_account",
            },
        }
    );

    check(res, {
        "setup account created": (r) =>
            r.status === 201,
    });

    return {
        accountId: res.json("id"),
        email,
    };
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
            tags: {
                name: "setup_create_pix_key",
            },
        }
    );

    check(res, {
        "setup pix key created": (r) =>
            r.status === 201,
    });
}

function deposit(email, amount) {
    const res = http.post(
        `${BASE_URL}/v1/transactions/deposit`,
        JSON.stringify({
            pix_key: email,
            pix_key_type: "EMAIL",
            source: "external",
            amount,
        }),
        {
            headers: {
                "Content-Type": "application/json",
                "x-idempotency-key": uuidv4(),
            },
            tags: {
                name: "setup_deposit",
            },
        }
    );

    check(res, {
        "setup deposit created": (r) =>
            r.status === 201,
    });
}

export default function (data) {
    const res = http.post(
        `${BASE_URL}/v1/transactions`,
        JSON.stringify({
            from_account_id:
            data.source.accountId,

            pix_key:
            data.destination.email,

            pix_key_type: "EMAIL",

            amount: "10.00",
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

    const is409 = res.status === 409;
    const is5xx = res.status >= 500;

    transferDuration.add(
        res.timings.duration
    );

    optimistic409Rate.add(is409);

    if (is409) {
        optimistic409Count.add(1);
    }

    transfer5xxRate.add(is5xx);

    const ok = check(res, {
        "transfer created": (r) =>
            r.status === 201 || r.status === 409,
    });

    transferErrors.add(!ok);
}