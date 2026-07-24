#!/usr/bin/env sh
set -eu

API_URL="${PMTOOL_API_URL:-http://127.0.0.1:5959/actuator/health}"
WEB_URL="${PMTOOL_WEB_URL:-http://127.0.0.1:8989}"

echo "Checking systemd service..."
systemctl is-active --quiet pmtool

echo "Checking API health..."
curl --fail --silent --show-error "$API_URL" | grep -q 'UP'

echo "Checking frontend response..."
curl --fail --silent --show-error --output /dev/null "$WEB_URL"

echo "Checking listening ports..."
ss -ltn | grep -q ':5959'
ss -ltn | grep -q ':8989'

echo "PMTool deployment verification passed."
