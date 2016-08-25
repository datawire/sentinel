FROM java:openjdk-8-jre-alpine
MAINTAINER Datawire Inc, <dev@datawire.io>

# Exposed Ports
# ----------------------------------------------------------
# 5000  : sentinel
# 5701  : Hazelcast Clustering
EXPOSE 5000 5701

LABEL PROJECT_REPO_URL = "git@github.com:datawire/sentinel.git" \
      PROJECT_REPO_BROWSER_URL = "https://github.com/datawire/sentinel" \
      PROJECT_LICENSE = "https://github.com/datawire/sentinel/LICENSE" \
      DESCRIPTION = "Datawire sentinel" \
      VENDOR = "Datawire" \
      VENDOR_URL = "https://datawire.io/"

RUN apk --no-cache add \
    bash \
  && ln -snf /bin/bash /bin/sh

RUN mkdir /var/log/datawire

WORKDIR /opt/sentinel/

COPY sentinel/build/libs/sentinel-web-*-fat.jar ./sentinel.jar

COPY sentinel/src/docker/entrypoint.sh ./entrypoint.sh

ENTRYPOINT ["./entrypoint.sh"]
