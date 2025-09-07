FROM amazoncorretto:21
WORKDIR /app

ENV APP_NAME="app-stock-consumer"

COPY build/libs/${APP_NAME}.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]