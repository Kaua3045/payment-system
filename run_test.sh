#!/bin/bash

SCRIPT_NAME=$1

if [ -z "$SCRIPT_NAME" ]; then
  echo "Uso: $0 <nome_do_script.js>"
  exit 1
fi

docker-compose run --rm k6 /tests/$SCRIPT_NAME
docker-compose down -v