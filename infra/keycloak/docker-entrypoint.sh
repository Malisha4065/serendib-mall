#!/bin/bash
# Substitute environment variables into the realm template
# then start Keycloak with the resolved config

TEMPLATE="/opt/keycloak/data/import-template/serendibmall-realm.json"
OUTPUT="/opt/keycloak/data/import/serendibmall-realm.json"

mkdir -p /opt/keycloak/data/import

# Replace ${VAR} placeholders with actual env var values
sed \
  -e "s|\${GOOGLE_CLIENT_ID}|${GOOGLE_CLIENT_ID:-not-configured}|g" \
  -e "s|\${GOOGLE_CLIENT_SECRET}|${GOOGLE_CLIENT_SECRET:-not-configured}|g" \
  "$TEMPLATE" > "$OUTPUT"

echo "Realm config prepared with Google OAuth credentials"

# Start Keycloak with all original arguments
exec /opt/keycloak/bin/kc.sh "$@"
