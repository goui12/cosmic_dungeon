#!/bin/bash
set -e

# ---- Config ----
OWNER="goui12"
REPO="cosmic_dungeon"
PATH_IN_REPO="build/libs"

JAR_PREFIX="cosmicdungeon"
LOG_FILE="Cosmic Dungeon Update History"

HERE="$(cd "$(dirname "$0")" && pwd)"
LOG_PATH="$HERE/$LOG_FILE"

# ---- Helpers ----
parse_version() {
  # cosmicdungeon-1.3.3.jar -> 1.3.3
  echo "$1" | sed -E 's/.*-([0-9]+\.[0-9]+\.[0-9]+)\.jar/\1/'
}

version_gt() {
  # returns true if $1 > $2
  [ "$(printf '%s\n' "$1" "$2" | sort -V | tail -n1)" = "$1" ] && [ "$1" != "$2" ]
}

show_popup() {
  local old="$1"
  local new="$2"

  osascript <<EOF
display dialog "Cosmic Dungeon updated!

Previous: $old
New:      $new" buttons {"OK"} default button "OK" with title "Cosmic Dungeon Updated"
EOF
}

log_update() {
  local old="$1"
  local new="$2"
  echo "$(date '+%Y-%m-%d %H:%M:%S')  |  $old -> $new" >> "$LOG_PATH"
}

# ---- Local version ----
LOCAL_JAR="$(ls "$HERE"/$JAR_PREFIX-*.jar 2>/dev/null | sort -V | tail -n1 || true)"
LOCAL_VERSION="0.0.0"

if [[ -n "$LOCAL_JAR" ]]; then
  LOCAL_VERSION="$(parse_version "$LOCAL_JAR")"
fi

# ---- Remote versions ----
API_URL="https://api.github.com/repos/$OWNER/$REPO/contents/$PATH_IN_REPO"

REMOTE_JSON="$(curl -fsSL "$API_URL")"

REMOTE_JAR="$(echo "$REMOTE_JSON" \
  | grep -o "\"name\": *\"$JAR_PREFIX-[0-9]\+\.[0-9]\+\.[0-9]\+\.jar\"" \
  | sed -E 's/.*"([^"]+)".*/\1/' \
  | sort -V \
  | tail -n1)"

if [[ -z "$REMOTE_JAR" ]]; then
  exit 0
fi

REMOTE_VERSION="$(parse_version "$REMOTE_JAR")"

# ---- Compare ----
if ! version_gt "$REMOTE_VERSION" "$LOCAL_VERSION"; then
  exit 0
fi

# ---- Download ----
DOWNLOAD_URL="https://raw.githubusercontent.com/$OWNER/$REPO/main/$PATH_IN_REPO/$REMOTE_JAR"
TMP="$HERE/$REMOTE_JAR.download"

curl -fsSL "$DOWNLOAD_URL" -o "$TMP"

# Sanity check
if [[ ! -s "$TMP" ]]; then
  rm -f "$TMP"
  exit 0
fi

# Backup old
if [[ -n "$LOCAL_JAR" ]]; then
  mv "$LOCAL_JAR" "$LOCAL_JAR.bak"
fi

mv "$TMP" "$HERE/$REMOTE_JAR"

# Cleanup older jars
ls "$HERE"/$JAR_PREFIX-*.jar 2>/dev/null | grep -v "$REMOTE_JAR" | xargs rm -f 2>/dev/null || true

# Log + popup
log_update "$LOCAL_VERSION" "$REMOTE_VERSION"
show_popup "$LOCAL_VERSION" "$REMOTE_VERSION"
