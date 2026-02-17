
#!/bin/bash

# Kafka 메시지 발행 스크립트
KAFKA_CONTAINER="kafka-broker"
BOOTSTRAP_SERVER="kafka:9092"

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

        echo ""
        echo "날짜 입력 방식을 선택하세요:"
        echo "1) 단일 날짜"
        echo "2) 날짜 범위"
        read -p "선택: " date_choice

        case $date_choice in
            1)
                # 기존 단일 날짜 로직
                default_date=$(date -d '2 days ago' '+%Y%m%d')
                read -p "기준 날짜 (YYYYMMDD, 기본값: $default_date): " input_date
                base_date=${input_date:-$default_date}

                # 날짜 형식 검증
                if [[ ! $base_date =~ ^[0-9]{8}$ ]]; then
                    echo "날짜 형식이 올바르지 않습니다. (YYYYMMDD)"
                    exit 1
                fi

                # JSON 메시지 생성
                message="{\"eventVersion\":1,\"marketType\":\"$market_type\",\"baseDate\":\"$base_date\"}"

                echo ""
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

            2)
                # 날짜 범위 로직
                echo ""
                echo "=== 날짜 범위 설정 ==="

                # 시작일 입력
                default_start_date=$(date -d '7 days ago' '+%Y%m%d')
                read -p "시작일 (YYYYMMDD, 기본값: $default_start_date): " input_start_date
                start_date=${input_start_date:-$default_start_date}

                # 종료일 입력
                default_end_date=$(date -d '2 days ago' '+%Y%m%d')
                read -p "종료일 (YYYYMMDD, 기본값: $default_end_date): " input_end_date
                end_date=${input_end_date:-$default_end_date}

                # 날짜 형식 검증
                if [[ ! $start_date =~ ^[0-9]{8}$ ]] || [[ ! $end_date =~ ^[0-9]{8}$ ]]; then
                    echo "날짜 형식이 올바르지 않습니다. (YYYYMMDD)"
                    exit 1
                fi

                # 날짜 순서 검증
                if [[ $start_date > $end_date ]]; then
                    echo "시작일이 종료일보다 늦습니다."
                    exit 1
                fi

                # 날짜 범위 계산
                start_timestamp=$(date -d "${start_date:0:4}-${start_date:4:2}-${start_date:6:2}" +%s)
                end_timestamp=$(date -d "${end_date:0:4}-${end_date:4:2}-${end_date:6:2}" +%s)

                # 총 일수 계산
                total_days=$(( (end_timestamp - start_timestamp) / 86400 + 1 ))

                echo ""
                echo "=== 발행 예정 메시지 정보 ==="
                echo "시장 타입: $market_type"
                echo "기간: $start_date ~ $end_date"
                echo "총 ${total_days}개의 메시지가 발행됩니다."
                echo ""

                # 미리보기 (처음 3개만)
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
                read -p "모든 메시지를 발행하시겠습니까? (y/n): " confirm
                if [[ $confirm == "y" || $confirm == "Y" ]]; then
                    echo ""
                    echo "=== 메시지 발행 중... ==="

                    current_timestamp=$start_timestamp
                    sent_count=0

                    while [[ $current_timestamp -le $end_timestamp ]]; do
                        current_date=$(date -d "@$current_timestamp" '+%Y%m%d')
                        message="{\"eventVersion\":1,\"marketType\":\"$market_type\",\"baseDate\":\"$current_date\"}"

                        # 메시지 발행
                        echo "date-key:$message" | docker exec -i $KAFKA_CONTAINER /opt/kafka/bin/kafka-console-producer.sh \
                            --bootstrap-server $BOOTSTRAP_SERVER \
                            --topic KRX_DATA_REQUEST \
                            --property "parse.key=true" \
                            --property "key.separator=:" > /dev/null 2>&1

                        sent_count=$(( sent_count + 1 ))
                        echo "✅ [$sent_count/$total_days] $current_date 메시지 발행 완료"

                        # 다음 날로 이동
                        current_timestamp=$(( current_timestamp + 86400 ))

                        # API 호출 부하를 줄이기 위한 짧은 대기
                        sleep 0.1
                    done

                    echo ""
                    echo "🎉 총 ${sent_count}개의 메시지 발행이 완료되었습니다!"
                else
                    echo "메시지 발행을 취소했습니다."
                fi
                ;;

            *)
                echo "잘못된 선택입니다."
                exit 1
                ;;
        esac
        ;;

    *)
        echo "잘못된 선택입니다."
        exit 1
        ;;
esac