FROM amazoncorretto:21
WORKDIR /app

ENV APP_NAME="scheduler"

COPY build/libs/${APP_NAME}.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]