FROM eclipse-temurin:11-jre-alpine
WORKDIR /app
RUN apk add --no-cache bash
COPY target/*.jar ./app.jar
ENTRYPOINT ["java","-jar","app.jar"]