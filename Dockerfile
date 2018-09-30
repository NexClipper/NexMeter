FROM openjdk:8u111-jdk-alpine
VOLUME /tmp
RUN apk add --no-cache curl
RUN mkdir -p /var/h2
ADD target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-Xmx256m","-Xms128m","-XX:+CMSClassUnloadingEnabled","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]