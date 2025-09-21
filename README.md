# 주식 데이터 시스템
소소하게 매일 정리하는 주가 정보를 손으로 하기 너무 귀찮다.
시스템이 알아서 해주면 좋겠다.

## 시스템 구조
![img.png](./doc/system.png)
1. stock-batch: 일일 주가 업데이트 요청을 메시지큐에 요청한다.
2. stock-consumer: 주식 데이터 업데이트 요청을 처리한다.
3. stock-api: 주식 데이터 조회/업데이트를 위한 API 서버
4. stock-app: 주식데이터를 보여줄 수 있는 화면

## 프로젝트 구성
1. Spring boot3(Java21)

## 인프라
1. mysql, kafka
>[docker-compose.local.yml](./docker-compose.local.yml)
2. 카프카 토픽 
```shell
docker exec kafka-broker /opt/kafka/bin/kafka-topics.sh \
    --bootstrap-server kafka:9092     \
    --create \
    --topic KRX_DATA_REQUEST \
    --partitions 3 \
    --replication-factor 1 \
    --config retention.ms=604800000 \
    --config compression.type=snappy

```
3. (운영) docker compose yml 생성

```yaml
version: '3.8'

services:
  stock-batch:
    image: ghcr.io/dove-kim/stock-ledger-system/app-stock-batch:latest
    container_name: stock-batch
    restart: unless-stopped
    environment:
      # Spring 프로파일
      SPRING_PROFILES_ACTIVE: prod

      # 데이터베이스 설정
      DB_HOST:
      DB_PORT:
      DB_USERNAME:
      DB_PASSWORD:

    volumes:
      - ./logs/app-batch:/app/logs

  stock-consumer:
    image: ghcr.io/dove-kim/stock-ledger-system/stock-consumer:latest
    container_name: stock-consumer
    restart: unless-stopped
    environment:
      # Spring 프로파일
      SPRING_PROFILES_ACTIVE: prod

      # 데이터베이스 설정
      DB_HOST:
      DB_PORT:
      DB_USERNAME: 
      DB_PASSWORD:
        
      # 카프카 설정
      KAFKA_HOST:
      KAFKA_PORT:

      # 한국거래소 키 설정
      KRX_API_AUTH_KEY: 
      
    volumes:
      - ./logs/app-consumer:/app/logs
```

## Kafka 메시지 발행 도구

테스트용 메시지를 쉽게 발행할 수 있는 스크립트를 제공합니다.

```bash
# 실행 권한 부여
chmod +x message_publish.sh

# 메시지 발행 도구 실행
./message_publish.sh
```
