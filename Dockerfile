FROM java:8-alpine
MAINTAINER Your Name <you@example.com>

ADD target/uberjar/toscoding.jar /toscoding/app.jar

EXPOSE 3000

CMD ["java", "-jar", "/toscoding/app.jar"]
