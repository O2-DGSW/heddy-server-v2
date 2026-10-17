#!/bin/sh
# Runs inside the candidate/running app container; never print the key.
set -eu
test -n "${OPENAI_API_KEY:-}" || {
  echo 'OPENAI_API_KEY is missing from app container' >&2
  exit 1
}
echo 'Recommendation API key is present in app container.'
