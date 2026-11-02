#!/usr/bin/env sh
# JRawr launcher. Runs the bundled application with the bundled game data and
# keeps characters, saved specs and other user files in a writable per-user
# directory.
set -eu

APP_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
DATA_DIR="$APP_DIR/data"
USER_DATA_DIR="${XDG_DATA_HOME:-$HOME/.local/share}/jrawr"
ADDRESS="${JRAWR_ADDRESS:-127.0.0.1}"
PORT="${JRAWR_PORT:-8080}"

if [ ! -f "$APP_DIR/jrawr.jar" ]; then
    echo "jrawr.jar was not found next to this launcher." >&2
    exit 1
fi
if [ ! -f "$DATA_DIR/ItemCache.xml" ]; then
    echo "Game data was not found in $DATA_DIR." >&2
    exit 1
fi
if ! command -v java >/dev/null 2>&1; then
    echo "Java 25 or newer is required, but 'java' was not found on PATH." >&2
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ { print $2; exit }')
JAVA_MAJOR=${JAVA_VERSION%%.*}
case "$JAVA_MAJOR" in
    25|2[6-9]|[3-9][0-9]) ;;
    *)
        echo "Java 25 or newer is required (found ${JAVA_VERSION:-unknown})." >&2
        exit 1
        ;;
esac

mkdir -p "$USER_DATA_DIR"

exec java ${JAVA_OPTS:-} -jar "$APP_DIR/jrawr.jar" \
    --server.address="$ADDRESS" \
    --server.port="$PORT" \
    --jrawr.data-dir="$DATA_DIR" \
    --jrawr.characters-dir="$USER_DATA_DIR" \
    --spring.thymeleaf.cache=true \
    --spring.web.resources.cache.period=365d
