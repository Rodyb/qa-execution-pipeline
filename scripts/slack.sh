#!/bin/bash
set -e

SLACK_WEBHOOK_URL="${SLACK_WEBHOOK_URL:?SLACK_WEBHOOK_URL is not set}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

SUMMARY_JSON="${ALLURE_REPORT_DIR}/widgets/summary.json"
if [[ -z "$ALLURE_REPORT_DIR" ]]; then
  echo "ALLURE_REPORT_DIR is not set"
  exit 1
fi
JOB_NAME="${JOB_NAME:-local-run}"
BUILD_NUMBER="${BUILD_NUMBER:-local}"
BUILD_URL="${BUILD_URL:-http://localhost}"

# ==============================
# VALIDATION
# ==============================
if [[ ! -f "$SUMMARY_JSON" ]]; then
  echo "Allure summary not found at:"
  echo "   $SUMMARY_JSON"
  echo ""
  echo "Did you run:"
  echo "  allure generate rest-assured/allure-results -o rest-assured/allure-report --clean"
  exit 1
fi

# ==============================
# PARSE ALLURE SUMMARY
# ==============================
PASSED=$(jq '.statistic.passed' "$SUMMARY_JSON")
FAILED=$(jq '.statistic.failed' "$SUMMARY_JSON")
BROKEN=$(jq '.statistic.broken' "$SUMMARY_JSON")
SKIPPED=$(jq '.statistic.skipped' "$SUMMARY_JSON")
TOTAL=$(jq '.statistic.total' "$SUMMARY_JSON")

DURATION_MS=$(jq '.time.duration' "$SUMMARY_JSON")
DURATION_MIN=$((DURATION_MS / 60000))
DURATION_SEC=$(((DURATION_MS / 1000) % 60))
DURATION="${DURATION_MIN}m ${DURATION_SEC}s"

# ==============================
# RESULT LOGIC
# ==============================
STATUS="PASSED"
EMOJI=":white_check_mark:"

if [[ "$FAILED" -ne 0 || "$BROKEN" -ne 0 ]]; then
  STATUS="FAILED"
  EMOJI=":x:"
fi

SUCCESS_RATE="0%"
if [[ "$TOTAL" -gt 0 ]]; then
  RATE=$(echo "scale=2; $PASSED*100/$TOTAL" | bc)
  SUCCESS_RATE="$(printf "%.0f%%" "$RATE")"
fi

# ==============================
# SLACK PAYLOAD
# ==============================
PAYLOAD=$(jq -n \
  --arg emoji "$EMOJI" \
  --arg status "$STATUS" \
  --arg passed "$PASSED" \
  --arg total "$TOTAL" \
  --arg rate "$SUCCESS_RATE" \
  --arg duration "$DURATION" \
  --arg job "$JOB_NAME" \
  --arg build "$BUILD_NUMBER" \
  --arg url "$BUILD_URL" \
  '{
    text: "\($emoji) API Tests — \($status)",
    blocks: [
      {
        type: "header",
        text: {
          type: "plain_text",
          text: "\($emoji) API TESTS — \($status)",
          emoji: true
        }
      },
      {
        type: "section",
        text: {
          type: "mrkdwn",
          text: "*Results*\n• *Passed:* \($passed)/\($total) (\($rate))\n• *Duration:* \($duration)"
        }
      },
      {
        type: "divider"
      },
      {
        type: "section",
        fields: [
          { "type": "mrkdwn", "text": "*Job*\n\($job)" },
          { "type": "mrkdwn", "text": "*Build*\n\($build)" }
        ]
      },
      {
        type: "actions",
        elements: [
          {
            type: "button",
            text: { type: "plain_text", text: "🔍 View Build" },
            url: "\($url)"
          }
        ]
      }
    ]
  }'
)



# ==============================
# SEND TO SLACK
# ==============================
curl -s -X POST \
  -H "Content-Type: application/json" \
  -d "$PAYLOAD" \
  "$SLACK_WEBHOOK_URL"

echo "Slack notification sent"
