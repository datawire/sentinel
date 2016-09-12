#!/bin/sh
set -x

APP_NAME="sentinel"
APP_JAR="${APP_NAME}.jar"
APP_CONFIG="config/${APP_NAME}.json"

JAVA_EXECUTABLE=java
JAVA_ARGS="-jar ${APP_JAR} \
-conf ${APP_CONFIG} \
-Dmdk.service.name=${MDK_SERVICE_NAME} \
-Dmdk.service.version=${MDK_SERVICE_VERSION} \
-Dmdk.service.host=${DATAWIRE_ROUTABLE_HOST} \
-Dmdk.service.port=${DATAWIRE_ROUTABLE_PORT} \
-Dvertx.logger-delegate-factory-class-name=io.vertx.core.logging.SLF4JLogDelegateFactory \
-Dlogback.configurationFile=config/logback.xml"

warn() {
  echo "$*"
}

# ------------------------------------------------------------------------------
# Increase the maximum file descriptors.
#
MAX_FD_LIMIT=`ulimit -H -n`
if [ $? -eq 0 ] ; then
  if [ "$MAX_FD" = "maximum" -o "$MAX_FD" = "max" ] ; then
    MAX_FD="$MAX_FD_LIMIT"
  fi

  ulimit -n $MAX_FD
  if [ $? -ne 0 ] ; then
    warn "Could not set maximum file descriptor limit: $MAX_FD"
  fi
else
  warn "Could not query maximum file descriptor limit: $MAX_FD_LIMIT"
fi

# ------------------------------------------------------------------------------
# Trap signals and propagate to the child process
#
trap 'kill -TERM $PID' TERM INT
$JAVA_EXECUTABLE $JAVA_ARGS &
PID=$!
wait $PID
trap - TERM INT
wait $PID
EXIT_STATUS=$?