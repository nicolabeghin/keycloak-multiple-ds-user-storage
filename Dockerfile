FROM maven:3.8.6-openjdk-18-slim
WORKDIR /opt/app

# to cache dependencies
ADD pom*.xml .
RUN mvn verify --fail-never

# build final JAR
COPY . .
RUN mvn package