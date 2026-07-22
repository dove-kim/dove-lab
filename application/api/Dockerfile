FROM amazoncorretto:21-al2023
WORKDIR /app

ENV APP_NAME="api"
# 컨테이너 메모리 한계 1GB 기준:
#   힙 700m + Metaspace 200m + 스레드·코드캐시 등 ≈ 1GB
# G1GC: REST API처럼 짧은 요청이 많은 워크로드에 적합
ENV JAVA_TOOL_OPTIONS="-Xmx700m -XX:MaxMetaspaceSize=200m -Xss256k -XX:+UseG1GC -Duser.timezone=Asia/Seoul"

# score.py(활성화 드라이런) 실행용 Python. scheduler와 동일 버전 고정(requirements.txt). numpy 2.5는 Python 3.12+ 필요 → 3.13.
COPY build/scoring/requirements.txt /app/scoring/requirements.txt
RUN dnf install -y python3.13 python3.13-pip libgomp && \
    python3.13 -m pip install --no-cache-dir --upgrade pip && \
    python3.13 -m pip install --no-cache-dir --prefer-binary -r /app/scoring/requirements.txt && \
    dnf clean all
ENV MODEL_SCORER_PYTHON="python3.13" \
    MODEL_SCORER_SCRIPT="/app/scoring/score.py"
COPY build/scoring/score.py /app/scoring/score.py

COPY build/libs/${APP_NAME}.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
