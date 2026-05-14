#!/bin/sh
set -e
. ./.env
scp build/libs/mc-server-otel-*.jar "$DEPLOY_TARGET"
