#!/bin/sh
BOOTSTRAP_SERVER="${BOOTSTRAP_SERVER:-kafka:29092}"
TOPICS="${TOPICS:-KRX_STOCK_PRICE_QUERY TECHNICAL_INDICATOR_CALC}"
PARTITIONS="${PARTITIONS:-4}"
REPLICATION_FACTOR="${REPLICATION_FACTOR:-1}"

echo "Waiting for Kafka to be ready..."
until /opt/kafka/bin/kafka-topics.sh --bootstrap-server "$BOOTSTRAP_SERVER" --list > /dev/null 2>&1; do
  sleep 2
done

for TOPIC in $TOPICS; do
  echo "Creating topic: $TOPIC"
  /opt/kafka/bin/kafka-topics.sh --bootstrap-server "$BOOTSTRAP_SERVER" --create --if-not-exists \
    --topic "$TOPIC" --partitions "$PARTITIONS" --replication-factor "$REPLICATION_FACTOR" \
    --config retention.ms=604800000 --config compression.type=snappy
done

echo "All topics created successfully."
