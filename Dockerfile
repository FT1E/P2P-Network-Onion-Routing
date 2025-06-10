FROM openjdk:21
COPY target /app
WORKDIR /app/classes
CMD java Main