import http from "k6/http";
import { check } from "k6";
import { uuidv4 } from "https://jslib.k6.io/k6-utils/1.2.0/index.js";

import { getBaseUrl } from "./environments.js";

const BASE_URL = getBaseUrl();

export function createFundedAccounts(count = 4000) {
    const accounts = [];

    console.log(`Creating ${count} accounts...`);

    for (let i = 0; i < count; i++) {
        const email = `capacity-${i}-${Date.now()}@mail.com`;

        const accountRes = http.post(
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

        const accountCreated = check(accountRes, {
            "setup account created": (r) => r.status === 201,
        });

        if (!accountCreated) {
            console.error(
                `Failed to create account ${i}: status=${accountRes.status} body=${accountRes.body}`
            );

            continue;
        }

        const accountId = accountRes.json("id");

        const pixRes = http.post(
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

        const pixCreated = check(pixRes, {
            "setup pix key created": (r) => r.status === 201,
        });

        if (!pixCreated) {
            console.error(
                `Failed to create PIX key for account ${accountId}: status=${pixRes.status}`
            );

            continue;
        }

        accounts.push({
            accountId,
            email,
        });
    }

    console.log(`Created ${accounts.length} accounts.`);

    console.log(`Funding ${accounts.length} accounts...`);

    for (let i = 0; i < accounts.length; i++) {
        const account = accounts[i];

        const depositRes = http.post(
            `${BASE_URL}/v1/transactions/deposit`,
            JSON.stringify({
                pix_key: account.email,
                pix_key_type: "EMAIL",
                source: "external",
                amount: 100000.0,
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

        const depositCreated = check(depositRes, {
            "setup deposit created": (r) => r.status === 201,
        });

        if (!depositCreated) {
            console.error(
                `Failed to fund account ${account.accountId}: status=${depositRes.status} body=${depositRes.body}`
            );
        }
    }

    console.log(`Dataset ready with ${accounts.length} funded accounts.`);

    return accounts;
}