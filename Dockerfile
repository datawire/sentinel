FROM java:openjdk-8-jre-alpine
MAINTAINER Datawire Inc, <dev@datawire.io>
LABEL PROJECT_REPO_URL = "git@github.com:datawire/sentinel.git" \
      PROJECT_REPO_BROWSER_URL = "https://github.com/datawire/sentinel" \
      DESCRIPTION = "Datawire sentinel" \
      VENDOR = "Datawire" \
      VENDOR_URL = "https://datawire.io/"

WORKDIR /opt/sentinel/

COPY sentinel-web/src/docker/entrypoint.sh \
     sentinel-web/build/libs/sentinel-web-*-fat.jar \
     ./sentinel.jar

EXPOSE 5000
ENTRYPOINT ["./entrypoint.sh"]
