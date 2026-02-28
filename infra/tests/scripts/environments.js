export const ENV = __ENV.ENV || 'dev';

export const ENVIRONMENTS = {
  dev: {
    baseUrl: 'http://app:8081/api',
  },
  staging: {
    baseUrl: 'https://change.me',
  },
  prod: {
    baseUrl: 'https://change.me.prod',
  },
};

export function getBaseUrl() {
  const envConfig = ENVIRONMENTS[ENV];

  if (!envConfig) {
    throw new Error(`Environment invalid: ${ENV}`);
  }

  return __ENV.BASE_URL || envConfig.baseUrl;
}