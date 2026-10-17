#!/bin/sh
# Run inside the candidate app container, with its real user, env and model mount.
set -eu

case "${HAIR_ANALYSIS_ENABLED:-false}" in
  true|TRUE|True) ;;
  false|FALSE|False|'')
    echo 'Hair analysis is disabled; analysis requests will return 503.'
    exit 0
    ;;
  *)
    echo 'Invalid HAIR_ANALYSIS_ENABLED; expected true or false.' >&2
    exit 1
    ;;
esac

model_path=${HAIR_ANALYSIS_MODEL_PATH:-}
if [ -z "$model_path" ] || [ ! -f "$model_path" ] || [ ! -r "$model_path" ] || [ ! -s "$model_path" ]; then
  echo 'Hair analysis is enabled, but its model is missing, unreadable or empty. Deployment stopped before restarting app.' >&2
  exit 1
fi

echo 'Hair analysis model file is readable. ONNX compatibility must be validated before activation.'
