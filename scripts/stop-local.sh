#!/usr/bin/env bash
set -Eeuo pipefail

PROJECT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
PID_FILE="$PROJECT_DIR/.local-run/tomcat.pid"

fail() {
    printf 'Error: %s\n' "$*" >&2
    exit 1
}

if [[ ! -f "$PID_FILE" ]]; then
    printf 'MediSys Tomcat is not running (no project PID file).\n'
    exit 0
fi

[[ -x "$TOMCAT_HOME/bin/catalina.sh" ]] \
    || fail "Tomcat 11 was not found at '$TOMCAT_HOME'. Set TOMCAT_HOME to its installation folder."
TOMCAT_HOME="$(cd -- "$TOMCAT_HOME" && pwd)"

pid="$(cat "$PID_FILE")"
if [[ ! "$pid" =~ ^[0-9]+$ ]]; then
    fail "The project PID file is invalid: $PID_FILE. Remove it only after verifying Tomcat is stopped."
fi

if ! kill -0 "$pid" 2>/dev/null; then
    rm -f -- "$PID_FILE"
    printf 'Removed stale Tomcat PID file; the server was already stopped.\n'
    exit 0
fi

process_args="$(ps -p "$pid" -o args= || true)"
if [[ "$process_args" != *org.apache.catalina.startup.Bootstrap* \
    || "$process_args" != *"-Dcatalina.home=$TOMCAT_HOME"* ]]; then
    fail "PID $pid is not a Tomcat process. Refusing to stop an unrelated process; inspect $PID_FILE."
fi

export CATALINA_HOME="$TOMCAT_HOME"
export CATALINA_PID="$PID_FILE"
"$TOMCAT_HOME/bin/catalina.sh" stop 20 -force

for _ in {1..15}; do
    if ! kill -0 "$pid" 2>/dev/null; then
        rm -f -- "$PID_FILE"
        printf 'MediSys Tomcat stopped. The SQL Server Docker container was left running.\n'
        exit 0
    fi
    sleep 1
done

fail "Tomcat did not stop. Check $TOMCAT_HOME/logs/catalina.out and stop it manually if needed."
