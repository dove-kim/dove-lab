FROM amazoncorretto:21
WORKDIR /app

ENV APP_NAME="scheduler"
# 컨테이너 메모리 한계 1.5GB 기준:
#   힙 1100m + Metaspace 250m + 가상스레드·코드캐시 등 ≈ 1.5GB
# ZGC: 대량 배치(4000종목 병렬 처리, 지표 계산) 시 GC pause 최소화
ENV JAVA_TOOL_OPTIONS="-Xmx1100m -XX:MaxMetaspaceSize=250m -Xss256k -XX:+UseZGC -Duser.timezone=Asia/Seoul"

COPY build/libs/${APP_NAME}.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]