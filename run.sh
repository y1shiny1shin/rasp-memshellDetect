#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

MODE="${1:-startup}"
case "$MODE" in
  startup|attach) ;;
  *) echo "用法: ./run.sh [startup|attach]"; exit 1 ;;
esac

if [[ "$MODE" == "attach" ]]; then
  PORT="${PORT:-18081}"
else
  PORT="${PORT:-18080}"
fi

APP_JAR="$(pwd)/target/rasp-memshellDetect.jar"
AGENT_JAR="$(pwd)/target/rasp-memshellDetect-agent.jar"
PAYLOAD_DIR="$(pwd)/target/test-classes"
WORK_DIR="$(pwd)/target/run-$MODE"
APP_LOG="$WORK_DIR/app.log"
BASE="http://127.0.0.1:$PORT"

wait_for_app() {
  local pid="$1"
  for _ in $(seq 1 240); do
    if ! kill -0 "$pid" 2>/dev/null; then
      echo "[error] 应用进程退出"
      cat "$APP_LOG"
      exit 1
    fi
    if curl -fsS "$BASE/" >/dev/null 2>&1; then
      return 0
    fi
    sleep 0.5
  done
  echo "[error] 应用启动超时"
  cat "$APP_LOG"
  exit 1
}

run_scenarios() {
  echo
  echo "=== 1. 白名单业务类执行命令（ALLOW） ==="
  curl -fsS "$BASE/api/orders/export"; echo

  echo
  echo "=== 2. 可信但不在白名单（WARN） ==="
  curl -fsS "$BASE/api/diagnostics/ping"; echo

  echo
  echo "=== 3. lambda 线程执行命令（ALLOW） ==="
  curl -fsS "$BASE/api/report/async"; echo

  echo
  echo "=== 4. JDK 动态代理执行命令（ALLOW） ==="
  curl -fsS "$BASE/api/plugin/run"; echo

  echo
  echo "=== 5. 注入 servlet 内存马 ==="
  curl -fsS "$BASE/test/inject"; echo

  echo
  echo "=== 6. 通过内存马执行命令（BLOCK，预期 HTTP 500） ==="
  SHELL_STATUS="$(curl -s -o /dev/null -w '%{http_code}' \
    "$BASE/shell?cmd=echo%20PWNED-BY-SERVLET-MEMSHELL")"
  echo "[shell] HTTP $SHELL_STATUS"
}

assert_scenarios() {
  local fail=0

  grep -q 'cmd=echo legit: order export completed -> ALLOW' "$APP_LOG" \
    || { echo "FAIL: 白名单业务场景不是 ALLOW"; fail=1; }
  grep -q 'cmd=echo diagnostics: ping -> WARN' "$APP_LOG" \
    || { echo "FAIL: 非白名单业务场景不是 WARN"; fail=1; }
  grep -q 'PWNED-BY-SERVLET-MEMSHELL -> BLOCK' "$APP_LOG" \
    || { echo "FAIL: servlet 内存马没有被 BLOCK"; fail=1; }
  [[ "$SHELL_STATUS" == "500" ]] \
    || { echo "FAIL: 内存马请求预期 HTTP 500，实际 $SHELL_STATUS"; fail=1; }

  if [[ "$fail" == 0 ]]; then
    echo "全部通过"
  fi
  return "$fail"
}

echo "[build] mvn clean -DskipTests package"
mvn -q -DskipTests clean package

rm -rf "$WORK_DIR"
mkdir -p "$WORK_DIR"

if [[ "$MODE" == "attach" ]]; then
  if [[ -z "${JAVA_HOME:-}" || ! -f "$JAVA_HOME/lib/tools.jar" ]]; then
    echo "attach 模式需要 JAVA_HOME 指向 JDK 8（tools.jar）"
    exit 1
  fi
  JAVA_BIN="$JAVA_HOME/bin/java"
  TOOLS_JAR="$JAVA_HOME/lib/tools.jar"

  echo "[run] 启动目标 JVM（$("$JAVA_BIN" -version 2>&1 | head -n 1)）"
  "$JAVA_BIN" -Dpayload.dir="$PAYLOAD_DIR" \
    -jar "$APP_JAR" --server.port="$PORT" \
    >"$APP_LOG" 2>&1 &
  APP_PID=$!
  trap 'kill "$APP_PID" 2>/dev/null || true' EXIT

  echo "[wait] Spring Boot 启动中..."
  wait_for_app "$APP_PID"

  echo "[attach] loadAgent -> pid=$APP_PID"
  "$JAVA_BIN" -cp "$(pwd)/target/classes:$TOOLS_JAR" \
    com.execguard.app.tooling.AttachMain "$APP_PID" "$AGENT_JAR"
else
  JAVA_BIN="${RUN_JAVA:-java}"

  echo "[run] $("$JAVA_BIN" -version 2>&1 | head -n 1)"
  echo "[run] app=$APP_JAR"
  echo "[run] agent=$AGENT_JAR"
  echo "[run] port=$PORT"

  "$JAVA_BIN" -javaagent:"$AGENT_JAR" -Dpayload.dir="$PAYLOAD_DIR" \
    -jar "$APP_JAR" --server.port="$PORT" \
    >"$APP_LOG" 2>&1 &
  APP_PID=$!
  trap 'kill "$APP_PID" 2>/dev/null || true' EXIT

  echo "[wait] Spring Boot 启动中..."
  wait_for_app "$APP_PID"
fi

run_scenarios

kill "$APP_PID" 2>/dev/null || true
wait "$APP_PID" 2>/dev/null || true
trap - EXIT

echo
echo "=== agent 判定结果 ==="
grep -E '\[rasp-memshell\].*(cmd=|reason:|blocked:)' "$APP_LOG" || true

echo
echo "=== 断言 ==="
if assert_scenarios; then
  exit 0
else
  echo
  echo "完整应用日志: $APP_LOG"
  exit 1
fi
