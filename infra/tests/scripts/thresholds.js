import { ENV } from './environments.js';

export function getThresholds() {
  const baseThresholds = {
    http_req_failed: ['rate<0.02'],
    transfer_duration: ['p(95)<1000'], // custom metric
  };

  const envThresholds = {
    dev: {
      http_req_duration: ['p(95)<2000'],
    },
    staging: {
      http_req_duration: ['p(95)<1500'],
    },
    prod: {
      http_req_duration: ['p(95)<800', 'p(99)<1200'],
      transfer_duration: ['p(95)<500'],
    },
  };

  return {
    ...baseThresholds,
    ...envThresholds[ENV],
  };
}