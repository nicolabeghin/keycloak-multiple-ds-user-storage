FROM maven:3.8.7-amazoncorretto-11

RUN mkdir /app
WORKDIR /app
COPY . .
RUN mvn package