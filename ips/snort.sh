#!/usr/bin/env bash

COMMAND=(
  "snort"
  "-c" "/etc/snort/snort.lua"
  "-k" "none"
  "-A" "alert_full"
)

echo "Executing: ${COMMAND[*]}"
exec "${COMMAND[@]}"
