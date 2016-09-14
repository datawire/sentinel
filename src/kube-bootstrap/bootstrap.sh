#!/usr/bin/env sh

if [ ! -n "$DATAWIRE_TOKEN" ]; then
    printf "Environment variable DATAWIRE_TOKEN is not set or empty! Use 'export DATAWIRE_TOKEN=<value>' to configure before running this script.\n"
    exit 1
fi

NAMESPACE=datawireio
FORCE=no

for i in "$@"; do
  case "$i" in
    -f|--force)
      FORCE=yes
    shift
    ;;
    *)
      echo "unknown option (option: $i)"
      exit 1
    ;;
  esac
done

if [[ "$FORCE" = "yes" ]]; then
    kubectl delete namespace "$NAMESPACE"
    sleep 10s
fi

kubectl create namespace "$NAMESPACE"

kubectl create secret generic datawire \
    --from-literal="token=${DATAWIRE_TOKEN}" \
    --namespace "$NAMESPACE"

kubectl apply -f sentinel.yml

printf "Done!"