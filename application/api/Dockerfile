FROM amazoncorretto:21
WORKDIR /app

ENV APP_NAME="api"
# 컨테이너 메모리 한계 1GB 기준:
#   힙 700m + Metaspace 200m + 스레드·코드캐시 등 ≈ 1GB
# G1GC: REST API처럼 짧은 요청이 많은 워크로드에 적합
ENV JAVA_TOOL_OPTIONS="-Xmx700m -XX:MaxMetaspaceSize=200m -Xss256k -XX:+UseG1GC -Duser.timezone=Asia/Seoul"

# 모델 활성화 드라이런이 score.py를 ProcessBuilder로 호출 → python + 모델 런타임 의존성 필요.
RUN yum install -y python3 python3-pip libgomp && \
    pip3 install --no-cache-dir --upgrade pip && \
    pip3 install --no-cache-dir --prefer-binary joblib numpy pandas lightgbm scikit-learn && \
    yum clean all
ENV MODEL_SCORER_PYTHON="python3" \
    MODEL_SCORER_SCRIPT="/app/scoring/score.py"
COPY build/scoring/score.py /app/scoring/score.py

COPY build/libs/${APP_NAME}.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
