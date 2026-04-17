#!/usr/bin/env bash
set -euo pipefail
cd "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

java_home() {
	[[ -n "${JAVA_HOME:-}" && -x "${JAVA_HOME}/bin/java" ]] && echo "$JAVA_HOME" && return
	local h
	h="$(/usr/libexec/java_home -v 21 2>/dev/null || true)"
	[[ -n "$h" && -x "$h/bin/java" ]] && echo "$h" && return
	for p in /usr/local/opt/openjdk@21 /opt/homebrew/opt/openjdk@21; do
		[[ -x "$p/bin/java" ]] && echo "$p" && return
	done
	return 1
}

export JAVA_HOME="$(java_home)" || {
	echo "Chưa có JDK 21. Cài: brew install openjdk@21" >&2
	exit 1
}
export PATH="$JAVA_HOME/bin:$PATH"

echo "Biên dịch lần đầu…"
./mvnw -q compile -DskipTests

mkdir -p target
stamp="target/.run-sh-stamp"
touch "$stamp"

echo "Chạy Spring Boot (sửa file trong src/main rồi Save → tự build; F5 trình duyệt). Ctrl+C: thoát."
./mvnw spring-boot:run &
mvn_pid=$!
trap 'kill "$mvn_pid" 2>/dev/null; wait "$mvn_pid" 2>/dev/null; exit 0' INT TERM

while sleep 1; do
	find src/main -type f -newer "$stamp" 2>/dev/null | grep -q . || continue
	echo "[$(date +%H:%M:%S)] Đang build…"
	if ./mvnw -q compile -DskipTests; then
		touch "$stamp"
	else
		echo "Build lỗi — sửa code rồi save lại."
	fi
done
