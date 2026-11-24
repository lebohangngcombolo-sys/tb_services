#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# TradeBridge Africa — Kafka topic bootstrap
# Creates the four domain topics + their DLQ counterparts (per spec §9).
# Idempotent: re-running is safe (existing topics are skipped).
# ---------------------------------------------------------------------------
set -euo pipefail

BOOTSTRAP="${KAFKA_BOOTSTRAP:-kafka:9092}"
PARTITIONS="${KAFKA_PARTITIONS:-3}"
REPLICATION="${KAFKA_REPLICATION:-1}"

TOPICS=(
  identity.events
  identity.events.dlq
  country.events
  country.events.dlq
  customer.events
  customer.events.dlq
  notification.events
  notification.events.dlq
)

echo "==> Waiting for Kafka at ${BOOTSTRAP} ..."
until kafka-topics --bootstrap-server "${BOOTSTRAP}" --list >/dev/null 2>&1; do
  echo "    ... still waiting"
  sleep 3
done

echo "==> Creating ${#TOPICS[@]} topics (partitions=${PARTITIONS}, replication=${REPLICATION})"
for topic in "${TOPICS[@]}"; do
  if kafka-topics --bootstrap-server "${BOOTSTRAP}" --list | grep -qx "${topic}"; then
    echo "    [skip] ${topic} already exists"
  else
    kafka-topics --bootstrap-server "${BOOTSTRAP}" \
      --create \
      --topic "${topic}" \
      --partitions "${PARTITIONS}" \
      --replication-factor "${REPLICATION}" \
      --config cleanup.policy=delete \
      --config retention.ms=604800000 \
      --config segment.bytes=1073741824
    echo "    [ ok ] ${topic}"
  fi
done

# DLQs use longer retention (30 days) — they hold failed events for replay
for dlq in "${TOPICS[@]}"; do
  if [[ "${dlq}" == *.dlq ]]; then
    kafka-configs --bootstrap-server "${BOOTSTRAP}" \
      --entity-type topics --entity-name "${dlq}" \
      --alter --add-config retention.ms=2592000000
    echo "    [cfg ] ${dlq} retention=30d"
  fi
done

echo "==> Done. Current topics:"
kafka-topics --bootstrap-server "${BOOTSTRAP}" --list