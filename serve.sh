#!/data/data/com.termux/files/usr/bin/bash
# ==============================================================================
# 🥁 Tabla Sangat — Localhost Server CLI (Termux)
# ==============================================================================

PORT=8080
SERVER_URL="http://localhost:${PORT}"
PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
SERVER_SCRIPT="${PROJECT_DIR}/server.js"
PID_FILE="${PROJECT_DIR}/.server.pid"
LOG_FILE="${PROJECT_DIR}/.server.log"

get_wifi_ip() {
  local ip=""
  if command -v node >/dev/null 2>&1; then
    ip=$(node -e "const os = require('os'); const ifaces = os.networkInterfaces(); for (let k of Object.keys(ifaces)) { if (/wlan|wifi|eth|ap|softap|rndis/i.test(k)) { for (let f of ifaces[k]) { if (f.family === 'IPv4' && !f.internal && f.address !== '127.0.0.1') { console.log(f.address); process.exit(0); } } } } for (let k of Object.keys(ifaces)) { if (!/ccmni|dummy|rmnet|pdp/i.test(k)) { for (let f of ifaces[k]) { if (f.family === 'IPv4' && !f.internal && /^(192\.168\.|10\.|172\.(1[6-9]|2[0-9]|3[0-1])\.)/.test(f.address)) { console.log(f.address); process.exit(0); } } } } console.log('localhost');" 2>/dev/null)
  fi
  if [ -z "$ip" ] || [ "$ip" = "localhost" ]; then
    ip=$(ifconfig wlan0 2>/dev/null | grep 'inet ' | awk '{print $2}' | sed 's/addr://')
  fi
  if [ -z "$ip" ]; then
    ip="localhost"
  fi
  echo "$ip"
}

WIFI_IP=$(get_wifi_ip)
WIFI_URL="http://${WIFI_IP}:${PORT}"

is_running() {
  if [ -f "$PID_FILE" ]; then
    local pid
    pid=$(cat "$PID_FILE" 2>/dev/null)
    if [ -n "$pid" ] && kill -0 "$pid" 2>/dev/null; then
      return 0
    fi
  fi
  if curl -s --max-time 1 "${SERVER_URL}/api/status" >/dev/null 2>&1; then
    return 0
  fi
  return 1
}

show_status() {
  echo ""
  echo "======================================================"
  echo "🥁 Tabla Sangat — Localhost Service Status"
  echo "======================================================"
  if is_running; then
    echo "  Status:        🟢 ONLINE"
    echo "  📱 Localhost:  ${SERVER_URL}"
    echo "  📶 Local Wi-Fi:${WIFI_URL}"
    echo "  📄 Serving:    ${PROJECT_DIR}/index.html"
    if curl -s --max-time 1 "http://localhost:3900/health" >/dev/null 2>&1; then
      echo "  🎙️ VoiceStudio: 🟢 ONLINE (http://localhost:3900)"
    else
      echo "  🎙️ VoiceStudio: ⚪ STANDBY (assets preloaded)"
    fi
    echo "======================================================"
  else
    echo "  Status:        🔴 OFFLINE"
    echo "  Command to start: tabla-serve start"
    echo "======================================================"
  fi
  echo ""
}

start_server() {
  if is_running; then
    echo "⚠️  Tabla Sangat localhost server is ALREADY RUNNING!"
    show_status
    return 0
  fi

  echo "🚀 Starting Tabla Sangat localhost server on port ${PORT}..."
  cd "${PROJECT_DIR}"
  setsid node "${SERVER_SCRIPT}" > "${LOG_FILE}" 2>&1 < /dev/null &
  local new_pid=$!
  echo "$new_pid" > "$PID_FILE"

  # Also start VoiceStudio speech engine
  bash "${PROJECT_DIR}/start_voicestudio.sh" start >/dev/null 2>&1 || true

  local attempts=0
  while [ $attempts -lt 6 ]; do
    if is_running; then
      break
    fi
    sleep 0.5
    attempts=$((attempts + 1))
  done

  if is_running; then
    echo "✅ Tabla Sangat server started successfully (PID: $new_pid)!"
    show_status
  else
    echo "❌ Failed to start server. Logs:"
    cat "${LOG_FILE}" 2>/dev/null | tail -n 15
  fi
}

stop_server() {
  echo "🛑 Stopping Tabla Sangat server..."
  bash "${PROJECT_DIR}/start_voicestudio.sh" stop >/dev/null 2>&1 || true
  if [ -f "$PID_FILE" ]; then
    local pid
    pid=$(cat "$PID_FILE" 2>/dev/null)
    if [ -n "$pid" ]; then
      kill "$pid" 2>/dev/null || true
      sleep 0.5
    fi
    rm -f "$PID_FILE"
  fi
  pkill -f "node.*Tabla App/server.js" 2>/dev/null || true
  echo "✅ Server stopped."
}

open_browser() {
  if ! is_running; then
    start_server
  fi
  echo "🌐 Opening ${SERVER_URL} in browser..."
  if command -v termux-open-url >/dev/null 2>&1; then
    termux-open-url "$SERVER_URL"
  elif command -v xdg-open >/dev/null 2>&1; then
    xdg-open "$SERVER_URL"
  else
    am start -a android.intent.action.VIEW -d "$SERVER_URL" >/dev/null 2>&1
  fi
  echo "📱 Browser launched!"
}

case "${1:-start}" in
  start)
    start_server
    ;;
  stop)
    stop_server
    ;;
  restart)
    stop_server
    sleep 0.5
    start_server
    ;;
  status)
    show_status
    ;;
  open)
    open_browser
    ;;
  logs)
    tail -n 40 -f "${LOG_FILE}"
    ;;
  *)
    echo "Usage: tabla-serve {start|stop|restart|status|open|logs}"
    ;;
esac
