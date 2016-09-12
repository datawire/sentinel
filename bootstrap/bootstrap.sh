#!/usr/bin/env bash
set -e

if [ ! -n "$DATAWIRE_TOKEN" ]; then
    printf "Environment variable DATAWIRE_TOKEN is not set or empty! Use 'export DATAWIRE_TOKEN=<value>' to configure before running this script.\n"
    exit 1
fi

printf "Create Datawire Namespace\n"
kubectl apply -f namespace.yml
sleep 5s

kubectl create secret generic datawire \
    --from-literal="token=${DATAWIRE_TOKEN}" \
    --namespace datawire

printf "Deploy Datawire Gateway\n"
kubectl apply -f gateway.yml
sleep 5s

printf "Deploy Datawire Sentinel\n"
kubectl apply -f sentinel.yml
sleep 5s

printf "Done!\n"