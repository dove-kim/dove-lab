#!/bin/bash

# Kafka 메시지 발행 스크립트
KAFKA_CONTAINER="kafka-broker"
BOOTSTRAP_SERVER="kafka:29092"

echo "=== Kafka 메시지 발행 도구 ==="
echo "1. 토픽 목록 조회"
echo "2. KRX 데이터 요청 메시지 발행"

read -p "원하는 작업 번호를 선택하세요: " choice

case $choice in
    1)
        echo "=== 토픽 목록 조회 ==="
        docker exec $KAFKA_CONTAINER /opt/kafka/bin/kafka-topics.sh --bootstrap-server $BOOTSTRAP_SERVER --list
        ;;

    2)
        echo "=== KRX 데이터 요청 메시지 발행 ==="
        echo "시장 타입을 선택하세요:"
        echo "1) KOSPI"
        echo "2) KOSDAQ"
        echo "3) KONEX"
        read -p "시장 타입 번호: " market_choice

        case $market_choice in
            1) market_type="KOSPI" ;;
            2) market_type="KOSDAQ" ;;
            3) market_type="KONEX" ;;
            *) echo "잘못된 선택입니다."; exit 1 ;;
        esac

        # 기본값: 2일 전 (배치 서버 로직과 동일)
        default_date=$(date -d '2 days ago' '+%Y%m%d')
        read -p "기준 날짜 (YYYYMMDD, 기본값: $default_date): " input_date
        base_date=${input_date:-$default_date}

        # 날짜 형식 검증
        if [[ ! $base_date =~ ^[0-9]{8}$ ]]; then
            echo "날짜 형식이 올바르지 않습니다. (YYYYMMDD)"
            exit 1
        fi

        # JSON 메시지 생성 (실제 KrxDailyStockDataRequest 형태)
        message="{\"eventVersion\":1,\"marketType\":\"$market_type\",\"baseDate\":\"$base_date\"}"

        echo "발행할 메시지: $message"
        echo "토픽: KRX_DATA_REQUEST"
        echo "키: date-key"

        read -p "메시지를 발행하시겠습니까? (y/n): " confirm
        if [[ $confirm == "y" || $confirm == "Y" ]]; then
            echo "date-key:$message" | docker exec -i $KAFKA_CONTAINER /opt/kafka/bin/kafka-console-producer.sh \
                --bootstrap-server $BOOTSTRAP_SERVER \
                --topic KRX_DATA_REQUEST \
                --property "parse.key=true" \
                --property "key.separator=:"
            echo "✅ 메시지 발행 완료!"
        else
            echo "메시지 발행을 취소했습니다."
        fi
        ;;

    *)
        echo "잘못된 선택입니다."
        exit 1
        ;;
esac