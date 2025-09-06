#!/bin/bash
FROM amazoncorretto:21
WORKDIR /app

ENV APP_NAME="app-stock-batch"
ENV PROFILE="prod"

COPY build/libs/${APP_NAME}.jar .

CMD java -jar \
    -Dspring.profiles.active=${PROFILE} \
    ${APP_NAME}.jar;
