FROM amazoncorretto:21
WORKDIR /app

ENV APP_NAME="scheduler"
# 컨테이너 메모리 한계 1.5GB 기준:
#   힙 1100m + Metaspace 250m + 가상스레드·코드캐시 등 ≈ 1.5GB
# ZGC: 대량 배치(4000종목 병렬 처리, 지표 계산) 시 GC pause 최소화
ENV JAVA_TOOL_OPTIONS="-Xmx1100m -XX:MaxMetaspaceSize=250m -Xss256k -XX:+UseZGC -Duser.timezone=Asia/Seoul"

# score.py(모델 채점) 실행용 Python. pickle 호환 위해 버전 고정(requirements.txt). numpy 2.x는 Python 3.11 필요.
COPY build/scoring/requirements.txt /app/scoring/requirements.txt
RUN yum install -y python3.11 libgomp && \
    python3.11 -m ensurepip --upgrade && \
    python3.11 -m pip install --no-cache-dir --upgrade pip && \
    python3.11 -m pip install --no-cache-dir --prefer-binary -r /app/scoring/requirements.txt && \
    yum clean all
ENV MODEL_SCORER_PYTHON="python3.11" \
    MODEL_SCORER_SCRIPT="/app/scoring/score.py"
COPY build/scoring/score.py /app/scoring/score.py

COPY build/libs/${APP_NAME}.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]