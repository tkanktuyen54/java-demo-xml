#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
if [[ -z "${JAVA_HOME:-}" && -x /usr/libexec/java_home ]]; then
  export JAVA_HOME="$(/usr/libexec/java_home -v 21)"
fi
exec ./mvnw spring-boot:run "$@"
