FROM maven:3.9.9-eclipse-temurin-17

WORKDIR /tests

RUN apt-get update && \
    apt-get install -y curl unzip jq bc && \
    rm -rf /var/lib/apt/lists/*

ARG ALLURE_VERSION=2.27.0
RUN curl -sLo /tmp/allure.zip \
      https://github.com/allure-framework/allure2/releases/download/${ALLURE_VERSION}/allure-${ALLURE_VERSION}.zip && \
    unzip /tmp/allure.zip -d /opt && \
    ln -s /opt/allure-${ALLURE_VERSION}/bin/allure /usr/bin/allure && \
    rm /tmp/allure.zip

COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src

ENV BASE_URL=http://messageboard-app:5000 \
    DB_URL=jdbc:postgresql://messageboard-db:5432/messageboard \
    DB_USER=messageboard \
    DB_PASS=messageboard_password

CMD ["mvn", "-B", "test"]
