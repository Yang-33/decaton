#!/usr/bin/env bash
# Validate and compile Decaton ProcessorProperties schemas
set -euo pipefail
shopt -s nullglob

AJV_CLI_VER="5.0.0"
SCHEMA_DIR="jsonschema/dist"
INSTANCE="${SCHEMA_DIR}/central-dogma-decaton-properties-example.json"

DRAFTS=(draft_7 draft_2019_09 draft_2020_12)
SUFFIXES=("" "-allow-additional-properties")

spec_opt() {
  case "$1" in
    draft_7)         echo "--spec=draft7"     ;;
    draft_2019_09)   echo "--spec=draft2019"  ;;
    draft_2020_12)   echo "--spec=draft2020"  ;;
    *)               echo ""                  ;;
  esac
}

# If npx is not installed, show an error message and exit
if ! command -v npx &> /dev/null; then
  echo "'npx' is not installed. Please install Node.js and npm to use npx." >&2
  exit 1
fi

echo "Start validation / compilation ..."
for draft in "${DRAFTS[@]}"; do
  for suffix in "${SUFFIXES[@]}"; do
    schema="${SCHEMA_DIR}/decaton-processor-properties-schema-${draft}${suffix}.json"

    [[ -f $schema ]] || { echo "Missing schema: $schema" >&2; exit 1; }

    spec=$(spec_opt "$draft")


    echo "Testing ${schema##*/} ..."
    # Check jsonschema itself
    npx -y ajv-cli@"$AJV_CLI_VER" compile  -s "$schema"                $spec
    # Check example json with jsonschema
    npx -y ajv-cli@"$AJV_CLI_VER" validate -s "$schema" -d "$INSTANCE" $spec
    echo
  done
done

echo "All done."
