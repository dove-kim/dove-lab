#!/bin/bash

# Kafka 메시지 발행 (대화형). 종료는 q.
#
# 사용법:
#   ./message_publish.sh                            # 로컬 기본
#   ./message_publish.sh <컨테이너명> <브로커주소> # 운영
#
# 인자 (기본값):
#   $1 KAFKA_CONTAINER   (kafka-broker)   Docker 컨테이너명
#   $2 BOOTSTRAP_SERVER  (kafka:29092)    컨테이너 내부 브로커 주소

# Windows Git Bash(MSYS2) 경로 자동 변환 방지 (리눅스에서는 무시됨)
export MSYS_NO_PATHCONV=1

KAFKA_CONTAINER="${1:-kafka-broker}"
BOOTSTRAP_SERVER="${2:-kafka:29092}"

publish_message() {
    local topic="$1"
    local key="$2"
    local message="$3"

    echo "$key:$message" | docker exec -i "$KAFKA_CONTAINER" /opt/kafka/bin/kafka-console-producer.sh \
        --bootstrap-server "$BOOTSTRAP_SERVER" \
        --topic "$topic" \
        --property "parse.key=true" \
        --property "key.separator=:" > /dev/null 2>&1
}

publish_single_date() {
    local market_type="$1"

    default_date=$(date -d '2 days ago' '+%Y%m%d')
    read -p "기준 날짜 (YYYYMMDD, 기본값: $default_date): " input_date
    base_date=${input_date:-$default_date}

    if [[ ! $base_date =~ ^[0-9]{8}$ ]]; then
        echo "날짜 형식이 올바르지 않습니다. (YYYYMMDD)"
        return
    fi

    message="{\"eventVersion\":1,\"marketType\":\"$market_type\",\"baseDate\":\"$base_date\"}"

    echo "발행: $message"
    publish_message "STOCK_PRICE_QUERY" "${base_date}-${market_type}" "$message"
    echo "완료"
}

publish_date_range() {
    local market_type="$1"

    echo ""
    echo "=== 날짜 범위 설정 ==="

    default_start_date=$(date -d '7 days ago' '+%Y%m%d')
    read -p "시작일 (YYYYMMDD, 기본값: $default_start_date): " input_start_date
    start_date=${input_start_date:-$default_start_date}

    default_end_date=$(date -d '2 days ago' '+%Y%m%d')
    read -p "종료일 (YYYYMMDD, 기본값: $default_end_date): " input_end_date
    end_date=${input_end_date:-$default_end_date}

    if [[ ! $start_date =~ ^[0-9]{8}$ ]] || [[ ! $end_date =~ ^[0-9]{8}$ ]]; then
        echo "날짜 형식이 올바르지 않습니다. (YYYYMMDD)"
        return
    fi

    if [[ $start_date > $end_date ]]; then
        echo "시작일이 종료일보다 늦습니다."
        return
    fi

    start_timestamp=$(date -d "${start_date:0:4}-${start_date:4:2}-${start_date:6:2}" +%s)
    end_timestamp=$(date -d "${end_date:0:4}-${end_date:4:2}-${end_date:6:2}" +%s)
    total_days=$(( (end_timestamp - start_timestamp) / 86400 + 1 ))

    echo ""
    echo "=== 발행 예정 메시지 정보 ==="
    echo "시장 타입: $market_type"
    echo "기간: $start_date ~ $end_date"
    echo "총 ${total_days}개의 메시지가 발행됩니다."
    echo ""

    echo "미리보기 (최대 3개):"
    current_timestamp=$start_timestamp
    preview_count=0
    while [[ $current_timestamp -le $end_timestamp ]] && [[ $preview_count -lt 3 ]]; do
        current_date=$(date -d "@$current_timestamp" '+%Y%m%d')
        message="{\"eventVersion\":1,\"marketType\":\"$market_type\",\"baseDate\":\"$current_date\"}"
        echo "  - $message"
        current_timestamp=$(( current_timestamp + 86400 ))
        preview_count=$(( preview_count + 1 ))
    done

    if [[ $total_days -gt 3 ]]; then
        echo "  ... 그 외 $(( total_days - 3 ))개"
    fi

    echo ""
    echo "=== 메시지 발행 중... ==="

    current_timestamp=$start_timestamp
    sent_count=0

    while [[ $current_timestamp -le $end_timestamp ]]; do
        current_date=$(date -d "@$current_timestamp" '+%Y%m%d')
        message="{\"eventVersion\":1,\"marketType\":\"$market_type\",\"baseDate\":\"$current_date\"}"

        publish_message "STOCK_PRICE_QUERY" "${current_date}-${market_type}" "$message"

        sent_count=$(( sent_count + 1 ))
        echo "[$sent_count/$total_days] $current_date 발행 완료"

        current_timestamp=$(( current_timestamp + 86400 ))
    done

    echo ""
    echo "총 ${sent_count}개의 메시지 발행이 완료되었습니다!"
}

publish_indicator_advance() {
    echo "시장 타입을 선택하세요:"
    echo "1) KOSPI"
    echo "2) KOSDAQ"
    echo "3) KONEX"
    read -p "시장 타입 번호: " market_choice

    case $market_choice in
        1) market_type="KOSPI" ;;
        2) market_type="KOSDAQ" ;;
        3) market_type="KONEX" ;;
        *) echo "잘못된 선택입니다."; return ;;
    esac

    read -p "종목 코드 (예: 005930): " stock_code
    if [[ -z "$stock_code" ]]; then
        echo "종목 코드를 입력해야 합니다."
        return
    fi

    message="{\"eventVersion\":1,\"marketType\":\"$market_type\",\"stockCode\":\"$stock_code\"}"
    echo "발행: $message"
    publish_message "INDICATOR_CALC_REQUESTED" "$stock_code" "$message"
    echo "완료"
}

handle_krx_publish() {
    echo "시장 타입을 선택하세요:"
    echo "1) KOSPI"
    echo "2) KOSDAQ"
    echo "3) KONEX"
    read -p "시장 타입 번호: " market_choice

    case $market_choice in
        1) market_type="KOSPI" ;;
        2) market_type="KOSDAQ" ;;
        3) market_type="KONEX" ;;
        *) echo "잘못된 선택입니다."; return ;;
    esac

    echo ""
    echo "날짜 입력 방식을 선택하세요:"
    echo "1) 단일 날짜"
    echo "2) 날짜 범위"
    read -p "선택: " date_choice

    case $date_choice in
        1) publish_single_date "$market_type" ;;
        2) publish_date_range "$market_type" ;;
        *) echo "잘못된 선택입니다." ;;
    esac
}

# 메인 루프
echo "=== Kafka 메시지 발행 도구 ==="
echo "컨테이너: $KAFKA_CONTAINER / 서버: $BOOTSTRAP_SERVER"
echo ""

while true; do
    echo "----------------------------------------"
    echo "1. 토픽 목록 조회"
    echo "2. KRX 데이터 요청 메시지 발행"
    echo "3. INDICATOR_CALC_REQUESTED 수동 발행"
    echo "q. 종료"
    read -p "> " choice

    case $choice in
        1)
            echo "=== 토픽 목록 ==="
            docker exec "$KAFKA_CONTAINER" /opt/kafka/bin/kafka-topics.sh --bootstrap-server "$BOOTSTRAP_SERVER" --list
            ;;
        2)
            handle_krx_publish
            ;;
        3)
            publish_indicator_advance
            ;;
        q|Q)
            echo "종료합니다."
            exit 0
            ;;
        *)
            echo "잘못된 선택입니다."
            ;;
    esac
    echo ""
done
