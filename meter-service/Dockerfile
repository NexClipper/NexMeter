FROM openjdk:8u111-jdk-alpine
VOLUME /tmp
ADD target/*.jar app.jar
EXPOSE 8080 8001
ENTRYPOINT ["java","-Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=8001","-Xmx400m","-Xms256m","-XX:+CMSClassUnloadingEnabled","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]