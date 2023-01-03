FROM maven:3.8.7-amazoncorretto-11

RUN mkdir /app
WORKDIR /app

# in order to cache maven dependencies
ADD pom.xml .
RUN mvn verify --fail-never

# actual build
COPY . .
RUN mvn package