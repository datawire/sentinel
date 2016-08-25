#!/usr/bin/env bash
set -euo pipefail

/usr/sbin/nginx && uwsgi --ini /opt/datawire/hello-mobius/config/uwsgi.ini
