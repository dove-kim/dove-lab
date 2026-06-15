FROM amazoncorretto:21
WORKDIR /app

ENV APP_NAME="api"
# 컨테이너 메모리 한계 1GB 기준:
#   힙 700m + Metaspace 200m + 스레드·코드캐시 등 ≈ 1GB
# G1GC: REST API처럼 짧은 요청이 많은 워크로드에 적합
ENV JAVA_TOOL_OPTIONS="-Xmx700m -XX:MaxMetaspaceSize=200m -Xss256k -XX:+UseG1GC -Duser.timezone=Asia/Seoul"

COPY build/libs/${APP_NAME}.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
