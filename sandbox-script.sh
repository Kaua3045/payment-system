#!/usr/bin/env bash
set -e

COMMAND=${1:-up}

OBS_COMPOSE="docker/sandbox/observability/docker-compose.yml"
REDIS_COMPOSE="docker/sandbox/redis/docker-compose.yml"
APP_COMPOSE="docker/sandbox/local/docker-compose.yml"

up() {
  echo "Creating network observability"
  docker network create observability

  echo "📊 Starting Observability stack..."
  docker compose -f $OBS_COMPOSE up -d

  echo "🧠 Starting Redis..."
  docker compose -f $REDIS_COMPOSE up -d

  echo "⏳ Waiting for services..."
  sleep 5

  echo "🧩 Starting application..."
  docker compose -f $APP_COMPOSE up -d
}

down() {
  echo "🧩 Stopping application..."
  docker compose -f $APP_COMPOSE down

  echo "🧠 Stopping Redis..."
  docker compose -f $REDIS_COMPOSE down

  echo "📊 Stopping Observability stack..."
  docker compose -f $OBS_COMPOSE down
}

restart() {
  down
  up
}

clean() {
  echo "🧹 Cleaning all sandbox containers and volumes..."

  docker compose -f $APP_COMPOSE down -v
  docker compose -f $REDIS_COMPOSE down -v
  docker compose -f $OBS_COMPOSE down -v
  docker network rm observability
}

case "$COMMAND" in
  up)
    up
    ;;
  down)
    down
    ;;
  restart)
    restart
    ;;
  clean)
    clean
    ;;
  *)
    echo "❌ Unknown command: $COMMAND"
    echo "Usage: ./sandbox.sh [up|down|restart|clean]"
    exit 1
    ;;
esac