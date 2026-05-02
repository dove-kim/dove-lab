#!/bin/sh
# Kafka 토픽 초기화. docker-compose의 kafka-init 서비스가 자동 호출.
#
# 환경변수 (기본값):
#   BOOTSTRAP_SERVER    (kafka:29092)                                              Kafka 브로커 주소
#   TOPICS              (STOCK_PRICE_QUERY STOCK_LISTING_QUERY INDICATOR_CALC_REQUESTED)  생성할 토픽 (공백 구분)
#   PARTITIONS          (4)                                                        파티션 수
#   REPLICATION_FACTOR  (1)                                                        복제 팩터
#
# 운영 환경: docker-compose의 environment로 오버라이드.

BOOTSTRAP_SERVER="${BOOTSTRAP_SERVER:-kafka:29092}"
TOPICS="${TOPICS:-STOCK_PRICE_QUERY STOCK_LISTING_QUERY INDICATOR_CALC_REQUESTED}"
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
