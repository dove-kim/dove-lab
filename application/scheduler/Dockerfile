FROM amazoncorretto:21
WORKDIR /app

ENV APP_NAME="scheduler"
# 컨테이너 메모리 한계 1.5GB 기준:
#   힙 1100m + Metaspace 250m + 가상스레드·코드캐시 등 ≈ 1.5GB
# ZGC: 대량 배치(4000종목 병렬 처리, 지표 계산) 시 GC pause 최소화
ENV JAVA_TOOL_OPTIONS="-Xmx1100m -XX:MaxMetaspaceSize=250m -Xss256k -XX:+UseZGC -Duser.timezone=Asia/Seoul"

# 모델 채점(ModelScoreSweep)이 score.py를 ProcessBuilder로 호출 → python + 모델 런타임 의존성 필요.
RUN yum install -y python3 python3-pip libgomp && \
    pip3 install --no-cache-dir joblib numpy pandas lightgbm scikit-learn && \
    yum clean all
ENV MODEL_SCORER_PYTHON="python3" \
    MODEL_SCORER_SCRIPT="/app/scoring/score.py"
COPY build/scoring/score.py /app/scoring/score.py

COPY build/libs/${APP_NAME}.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]