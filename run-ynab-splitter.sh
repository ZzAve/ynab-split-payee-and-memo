#!/bin/bash

calculate_last_successful_run () {
    local LAST_SUCCESS="$1"
    local TIME_AGO="unknown time ago"
    LAST_SUCCESS_SECONDS=$(date -j -u -f '%Y-%m-%dT%H:%M:%SZ' "${LAST_SUCCESS}" '+%s')
    if [ -z "${LAST_SUCCESS_SECONDS}" ]; then
      echo "${TIME_AGO}"
      return 1
    fi
    CURRENT_SECONDS=$(date -u +"%s")
    DIFF_SECONDS=$((CURRENT_SECONDS - LAST_SUCCESS_SECONDS))
    if [ $DIFF_SECONDS -lt 60 ]; then
      TIME_AGO="$DIFF_SECONDS seconds ago"
    elif [ $DIFF_SECONDS -lt 3600 ]; then
      MINUTES=$((DIFF_SECONDS / 60))
      TIME_AGO="$MINUTES minutes ago"
    elif [ $DIFF_SECONDS -lt 86400 ]; then
      HOURS=$((DIFF_SECONDS / 3600))
      TIME_AGO="$HOURS hours ago"
    else
      DAYS=$((DIFF_SECONDS / 86400))
      TIME_AGO="$DAYS days ago"
    fi

    echo "${TIME_AGO}"
}

LOG_DIR="$(pwd)/logs"
 Run the Docker container
docker run \
  -v "${LOG_DIR}:/app/logs" \
  --env-file .env \
  --rm --name ynab-updater \
  zzave/ynab-split-payee:0.0.1

RESULT="$?"
if [ "$RESULT" -eq 0 ]; then
  echo "YNAB Split Payee and Memo completed successfully"
  date -u +"%Y-%m-%dT%H:%M:%SZ" > .last_successful_run
  echo "0" > .failure_count
  echo "Consecutive failures reset to 0"

else
  LAST_SUCCESS=$(cat .last_successful_run)
  TIME_AGO="$(calculate_last_successful_run "${LAST_SUCCESS}")"

  # Update failure count
  if [ -f .failure_count ]; then
    COUNT=$(cat .failure_count)
    # Increment the count
    COUNT=$((COUNT + 1))
  else
    # Initialize count if file doesn't exist
    COUNT=1
  fi
  echo "$COUNT" > .failure_count
  echo "Consecutive failures: $COUNT"

  echo "RESULT: '$TIME_AGO'"
  script="display notification \"Check the logs for details: ${LOG_DIR}\" with title \"Updating YNAB failed (${COUNT})\" subtitle \"Last success: ${TIME_AGO} (${LAST_SUCCESS})\""
    echo "Script: ${script}"
    echo "YNAB Split Payee and Memo failed with exit code $RESULT"
    osascript -e "${script}"
fi
