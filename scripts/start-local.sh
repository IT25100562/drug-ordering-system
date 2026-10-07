#!/usr/bin/env bash
set -Eeuo pipefail

PROJECT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
RUN_DIR="$PROJECT_DIR/.local-run"
PID_FILE="$RUN_DIR/tomcat.pid"
APP_URL="http://localhost:8080/medisys/"

fail() {
    printf 'Error: %s\n' "$*" >&2
    exit 1
}

if ! command -v java >/dev/null 2>&1; then
    fail "Java is not installed or is not on PATH. Install a JDK 17 or newer."
fi
if ! command -v mvn >/dev/null 2>&1; then
    fail "Maven is not installed or is not on PATH."
fi
if ! command -v curl >/dev/null 2>&1; then
    fail "curl is required for the local server checks. Install it with: sudo apt install curl"
fi

JAVA_VERSION="$(java -version 2>&1 | awk -F '[\".]' '/version/ { print $2; exit }')"
if [[ ! "$JAVA_VERSION" =~ ^[0-9]+$ ]] || (( JAVA_VERSION < 17 )); then
    fail "Java 17 or newer is required."
fi

TOMCAT_HOME="${TOMCAT_HOME:-${CATALINA_HOME:-$HOME/apache-tomcat-11}}"
[[ -x "$TOMCAT_HOME/bin/catalina.sh" ]] \
    || fail "Tomcat 11 was not found at '$TOMCAT_HOME'. Set TOMCAT_HOME to its installation folder."
TOMCAT_HOME="$(cd -- "$TOMCAT_HOME" && pwd)"

[[ -f "$PROJECT_DIR/src/main/resources/db.properties" ]] \
    || fail "Database settings are missing. Copy src/main/resources/db.properties.example to src/main/resources/db.properties and set your credentials."

if ! timeout 3 bash -c ':</dev/tcp/127.0.0.1/1433' 2>/dev/null; then
    fail "SQL Server is not reachable at localhost:1433. Start your Docker container manually and confirm it publishes port 1433."
fi

mkdir -p "$RUN_DIR"
if [[ -f "$PID_FILE" ]]; then
    old_pid="$(cat "$PID_FILE")"
    if [[ "$old_pid" =~ ^[0-9]+$ ]] && kill -0 "$old_pid" 2>/dev/null; then
        fail "This project's Tomcat already appears to be running (PID $old_pid). Use scripts/stop-local.sh first."
    fi
    rm -f -- "$PID_FILE"
fi

if timeout 1 bash -c ':</dev/tcp/127.0.0.1/8080' 2>/dev/null; then
    fail "Port 8080 is already in use. Stop the other service or free the port before starting MediSys."
fi

printf 'Building MediSys...\n'
(
    cd -- "$PROJECT_DIR"
    mvn package
)

[[ -s "$PROJECT_DIR/target/medisys.war" ]] || fail "Maven did not create target/medisys.war."
[[ -d "$TOMCAT_HOME/webapps" && -w "$TOMCAT_HOME/webapps" ]] \
    || fail "Tomcat webapps folder is missing or not writable: $TOMCAT_HOME/webapps"
install -m 0644 "$PROJECT_DIR/target/medisys.war" "$TOMCAT_HOME/webapps/medisys.war"

export CATALINA_HOME="$TOMCAT_HOME"
export CATALINA_PID="$PID_FILE"
"$TOMCAT_HOME/bin/catalina.sh" start

for _ in {1..30}; do
    if [[ -f "$PID_FILE" ]] && ! kill -0 "$(cat "$PID_FILE")" 2>/dev/null; then
        fail "Tomcat stopped during startup. Check $TOMCAT_HOME/logs/catalina.out"
    fi
    status="$(curl --silent --output /dev/null --write-out '%{http_code}' --connect-timeout 1 --max-time 2 "$APP_URL" || true)"
    if [[ "$status" =~ ^[23][0-9][0-9]$ ]]; then
        printf 'MediSys is live: %s\n' "$APP_URL"
        printf 'Tomcat log: %s/logs/catalina.out\n' "$TOMCAT_HOME"
        printf 'To stop MediSys: %s/scripts/stop-local.sh\n' "$PROJECT_DIR"
        exit 0
    fi
    sleep 2
done

printf 'Error: MediSys did not respond at %s within 120 seconds.\n' "$APP_URL" >&2
printf 'Check the Tomcat log: %s/logs/catalina.out\n' "$TOMCAT_HOME" >&2
exit 1
