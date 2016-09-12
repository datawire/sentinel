FROM openjdk:8-jdk-alpine
MAINTAINER Datawire Inc, <dev@datawire.io>

LABEL PROJECT_REPO_URL         = "git@github.com:datawire/sentinel.git" \
      PROJECT_REPO_BROWSER_URL = "https://github.com/datawire/sentinel" \
      DESCRIPTION              = "Datawire Sentinel" \
      VENDOR                   = "Datawire" \
      VENDOR_URL               = "https://datawire.io/"

WORKDIR /opt/sentinel
COPY    . ./

RUN apk --no-cache add --virtual .build-deps \
        bash \
        libstdc++ \
    && ./gradlew shadowJar \
    && apk del .build-deps \
    && mv build/libs/sentinel*.jar ./sentinel.jar \
    && chmod +x entrypoint.sh \
    && rm -rf src build gradle .vertx .gradle .git .gitignore .dockerignore Dockerfile

EXPOSE 5000
ENTRYPOINT ["./entrypoint.sh"]
