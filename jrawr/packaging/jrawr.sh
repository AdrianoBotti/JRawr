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
if [ -n "${JRAWR_JAVA:-}" ]; then
    JAVA_BIN="$JRAWR_JAVA"
elif [ -x "$APP_DIR/runtime/bin/java" ]; then
    JAVA_BIN="$APP_DIR/runtime/bin/java"
elif [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/java" ]; then
    JAVA_BIN="$JAVA_HOME/bin/java"
else
    JAVA_BIN="java"
fi

if ! command -v "$JAVA_BIN" >/dev/null 2>&1; then
    echo "Java 25 or newer is required, but no java executable was found." >&2
    echo "Set JRAWR_JAVA to a Java 25 executable, or install Java 25." >&2
    exit 1
fi

JAVA_VERSION=$("$JAVA_BIN" -version 2>&1 | awk -F '"' '/version/ { print $2; exit }')
JAVA_MAJOR=${JAVA_VERSION%%.*}
case "$JAVA_MAJOR" in
    25|2[6-9]|[3-9][0-9]) ;;
    *)
        echo "Java 25 or newer is required (found ${JAVA_VERSION:-unknown})." >&2
        echo "Set JRAWR_JAVA to a Java 25 executable, or install Java 25." >&2
        exit 1
        ;;
esac

mkdir -p "$USER_DATA_DIR"

exec "$JAVA_BIN" ${JAVA_OPTS:-} -jar "$APP_DIR/jrawr.jar" \
    --server.address="$ADDRESS" \
    --server.port="$PORT" \
    --jrawr.data-dir="$DATA_DIR" \
    --jrawr.characters-dir="$USER_DATA_DIR" \
    --spring.thymeleaf.cache=true \
    --spring.web.resources.cache.period=365d
