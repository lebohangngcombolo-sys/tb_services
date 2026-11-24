#!/usr/bin/env bash
# ============================================================================
# TradeBridge Africa — Kafka topic bootstrap
#
# Creates the four domain event topics + their DLQ counterparts.
# Idempotent: re-running is safe — existing topics are skipped.
#
# Usage:
#   ./create-topics.sh                         # defaults: localhost:9092, 3 partitions
#   KAFKA_BOOTSTRAP=kafka:29092 ./create-topics.sh
#   KAFKA_PARTITIONS=6 KAFKA_REPLICATION=3 ./create-topics.sh
# ============================================================================
set -euo pipefail

BOOTSTRAP="${KAFKA_BOOTSTRAP:-localhost:9092}"
PARTITIONS="${KAFKA_PARTITIONS:-3}"
REPLICATION="${KAFKA_REPLICATION:-1}"

# Docker exec mode — set KAFKA_IN_DOCKER=true if Kafka runs in the compose
# container (kafka client is inside the image, not on your host).
KAFKA_IN_DOCKER="${KAFKA_IN_DOCKER:-false}"
KAFKA_CONTAINER="${KAFKA_CONTAINER:-tradebridge-kafka}"

# Kafka CLI wrapper — detects whether to run locally or via docker exec.
kafka_cmd() {
  if [ "${KAFKA_IN_DOCKER}" = "true" ]; then
    docker exec -i "${KAFKA_CONTAINER}" "$@"
  else
    "$@"
  fi
}

# ------------------------------------------------------------------ topics
# The order matters only for readability. Each event topic is followed
# immediately by its DLQ so the pairing is visually obvious.
TOPICS=(
  "identity.events"
  "identity.events.dlq"
  "country.events"
  "country.events.dlq"
  "customer.events"
  "customer.events.dlq"
  "notification.events"
  "notification.events.dlq"
)

# ------------------------------------------------------------------ helpers
log() { printf '[kafka-init] %s\n' "$*"; }

wait_for_kafka() {
  local attempts=0
  local max_attempts=60
  log "Waiting for Kafka at ${BOOTSTRAP} (max ${max_attempts} attempts)..."
  until kafka_cmd kafka-topics --bootstrap-server "${BOOTSTRAP}" --list >/dev/null 2>&1; do
    attempts=$((attempts + 1))
    if [ "${attempts}" -ge "${max_attempts}" ]; then
      log "ERROR: Kafka not reachable after ${max_attempts} attempts"
      exit 1
    fi
    sleep 2
  done
  log "Kafka is reachable"
}

topic_exists() {
  local topic="$1"
  kafka_cmd kafka-topics --bootstrap-server "${BOOTSTRAP}" --list 2>/dev/null \
    | grep -qx "${topic}"
}

create_topic() {
  local topic="$1"
  if topic_exists "${topic}"; then
    log "  [skip] ${topic} already exists"
    return 0
  fi
  log "  [create] ${topic}"
  kafka_cmd kafka-topics --bootstrap-server "${BOOTSTRAP}" \
    --create \
    --topic "${topic}" \
    --partitions "${PARTITIONS}" \
    --replication-factor "${REPLICATION}" \
    --config cleanup.policy=delete \
    --config retention.ms=604800000 \
    --config segment.bytes=1073741824
}

configure_dlq() {
  local topic="$1"
  # DLQs retain for 30 days so operators have time to inspect/replay
  log "  [config] ${topic} retention=30d"
  kafka_cmd kafka-configs --bootstrap-server "${BOOTSTRAP}" \
    --entity-type topics --entity-name "${topic}" \
    --alter --add-config retention.ms=2592000000 >/dev/null
}

# ------------------------------------------------------------------ main
log "=========================================================="
log "TradeBridge Africa — Kafka topic initialization"
log "Bootstrap:   ${BOOTSTRAP}"
log "Partitions:  ${PARTITIONS}"
log "Replication: ${REPLICATION}"
log "In Docker:   ${KAFKA_IN_DOCKER} (container: ${KAFKA_CONTAINER})"
log "=========================================================="

wait_for_kafka

log "Creating ${#TOPICS[@]} topics..."
for topic in "${TOPICS[@]}"; do
  create_topic "${topic}"
done

log "Configuring DLQ retention..."
for topic in "${TOPICS[@]}"; do
  if [[ "${topic}" == *.dlq ]]; then
    configure_dlq "${topic}"
  fi
done

log "=========================================================="
log "Current topics:"
kafka_cmd kafka-topics --bootstrap-server "${BOOTSTRAP}" --list | sort | sed 's/^/  /'
log "=========================================================="
log "Done."
