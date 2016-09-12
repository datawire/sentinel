#!/usr/bin/env bash
set -euo pipefail

# ngrok.sh
#
# Start an ngrok tunnel which is useful for sharing a local service among developers on a team. This script assumes
# that you have an authorized paid ngrok account that can assign a subdomain.

SERVICE_NAME="$(cat Datawirefile | python -c 'import sys, json; print json.load(sys.stdin)["service"]["name"]')"
GIT_BRANCH=$(git rev-parse --abbrev-ref HEAD)
SUBDOMAIN="${SERVICE_NAME}-$(whoami)-${GIT_BRANCH}"

printf "Starting ngrok.io tunnel... (subdomain: ${SUBDOMAIN})\n"
ngrok http -subdomain "$SUBDOMAIN" 5000