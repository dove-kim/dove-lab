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

## 환경변수

모든 설정은 환경변수로 주입하며, 기본값이 있어 로컬에서는 별도 설정 없이 실행 가능하다.

| 환경변수 | 설명 | 기본값 | 비고 |
|---|---|---|---|
| `DB_HOST` | MySQL 호스트 | `127.0.0.1` | |
| `DB_PORT` | MySQL 포트 | `13306` | |
| `DB_USERNAME` | DB 사용자명 | `dove_local_test` | |
| `DB_PASSWORD` | DB 비밀번호 | `dove1234` | 운영 시 반드시 변경 |
| `KAFKA_HOST` | Kafka 브로커 호스트 | `localhost` | |
| `KAFKA_PORT` | Kafka 브로커 포트 | `9092` | |
| `KRX_API_AUTH_KEY` | 한국거래소 API 인증키 | (없음) | stock-consumer에서 필수 |
| `LOG_LEVEL` | 로그 레벨 | `DEBUG` | DEBUG, INFO, WARN, ERROR |
| `LOG_PATH` | 로그 파일 경로 | `./logs` | 파일 로깅 활성화 시 사용 |
| `SPRING_PROFILES_INCLUDE` | 파일 로깅 활성화 | (없음) | `file` 설정 시 파일 로깅 추가 |

### 로컬 실행

[docker-compose.local.yml](./docker-compose.local.yml)로 인프라를 띄우면 기본값으로 바로 실행된다.

```bash
# 1. 인프라 실행 (Kafka 토픽은 kafka-init 컨테이너가 자동 생성)
docker compose -f docker-compose.local.yml up -d

# 2. stock-batch 실행 (콘솔 DEBUG)
DB_HOST=127.0.0.1 DB_PORT=13306 \
DB_USERNAME=dove_local_test DB_PASSWORD=dove1234 \
KAFKA_HOST=localhost KAFKA_PORT=9092 \
LOG_LEVEL=DEBUG \
./gradlew :app-stock-batch:bootRun

# 3. stock-consumer 실행 (콘솔 DEBUG)
DB_HOST=127.0.0.1 DB_PORT=13306 \
DB_USERNAME=dove_local_test DB_PASSWORD=dove1234 \
KAFKA_HOST=localhost KAFKA_PORT=9092 \
KRX_API_AUTH_KEY=<발급받은_인증키> \
LOG_LEVEL=DEBUG \
./gradlew :app-stock-consumer:bootRun

# 파일 로깅도 함께 사용하려면 SPRING_PROFILES_INCLUDE=file 추가
DB_HOST=127.0.0.1 DB_PORT=13306 \
DB_USERNAME=dove_local_test DB_PASSWORD=dove1234 \
KAFKA_HOST=localhost KAFKA_PORT=9092 \
KRX_API_AUTH_KEY=<발급받은_인증키> \
LOG_LEVEL=DEBUG LOG_PATH=./logs \
SPRING_PROFILES_INCLUDE=file \
./gradlew :app-stock-consumer:bootRun
```

### 운영 실행 (Docker Compose)

```yaml
version: '3.8'

services:
  stock-batch:
    image: ghcr.io/dove-kim/stock-ledger-system/app-stock-batch:latest
    container_name: stock-batch
    restart: unless-stopped
    environment:
      DB_HOST: <DB 호스트>
      DB_PORT: <DB 포트>
      DB_USERNAME: <DB 사용자명>
      DB_PASSWORD: <DB 비밀번호>
      KAFKA_HOST: <Kafka 호스트>
      KAFKA_PORT: <Kafka 포트>
      LOG_LEVEL: INFO
      SPRING_PROFILES_INCLUDE: file
    volumes:
      - ./logs/app-batch:/logs
    network_mode: "host"

  stock-consumer:
    image: ghcr.io/dove-kim/stock-ledger-system/app-stock-consumer:latest
    container_name: stock-consumer
    restart: unless-stopped
    environment:
      DB_HOST: <DB 호스트>
      DB_PORT: <DB 포트>
      DB_USERNAME: <DB 사용자명>
      DB_PASSWORD: <DB 비밀번호>
      KAFKA_HOST: <Kafka 호스트>
      KAFKA_PORT: <Kafka 포트>
      KRX_API_AUTH_KEY: <한국거래소 API 인증키>
      LOG_LEVEL: INFO
      SPRING_PROFILES_INCLUDE: file
    volumes:
      - ./logs/app-consumer:/logs
    network_mode: "host"
```

## 쉘 스크립트

### kafka-init.sh — Kafka 토픽 초기화

Docker 컨테이너 내부에서 실행되며, `docker-compose.local.yml`의 `kafka-init` 서비스가 자동 호출한다.

| 환경변수 | 기본값 | 설명 |
|---|---|---|
| `BOOTSTRAP_SERVER` | `kafka:29092` | Kafka 브로커 주소 |
| `TOPICS` | `KRX_STOCK_PRICE_QUERY TECHNICAL_INDICATOR_CALC` | 생성할 토픽 (공백 구분) |
| `PARTITIONS` | `4` | 파티션 수 |
| `REPLICATION_FACTOR` | `1` | 복제 팩터 |

운영 환경에서는 docker-compose의 `environment`로 오버라이드한다.

```yaml
kafka-init:
  environment:
    BOOTSTRAP_SERVER: "prod-kafka:29092"
    PARTITIONS: "12"
    REPLICATION_FACTOR: "3"
```

### message_publish.sh — Kafka 메시지 발행

호스트에서 직접 실행하는 대화형 도구로, 종료(`q`) 전까지 반복 사용 가능하다.

```bash
# 로컬 (기본값: kafka-broker / kafka:29092)
./message_publish.sh

# 운영 (컨테이너명과 브로커 주소 지정)
./message_publish.sh prod-broker prod-kafka:29092
```

| 인자 | 순서 | 기본값 | 설명 |
|---|---|---|---|
| `KAFKA_CONTAINER` | 1번째 | `kafka-broker` | Docker 컨테이너명 |
| `BOOTSTRAP_SERVER` | 2번째 | `kafka:29092` | 컨테이너 내부 브로커 주소 |
