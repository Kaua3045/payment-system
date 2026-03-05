#!/bin/bash

SCRIPT_NAME=$1
ENVIRONMENT=${2:-dev}

if [ -z "$SCRIPT_NAME" ]; then
  echo "Use: $0 <script_name.js> [env]"
  exit 1
fi

docker-compose -f infra/tests/docker/docker-compose.yml run --rm \
  -e ENV=$ENVIRONMENT \
  k6 \
  /tests/$SCRIPT_NAME

docker-compose -f infra/tests/docker/docker-compose.yml down -v