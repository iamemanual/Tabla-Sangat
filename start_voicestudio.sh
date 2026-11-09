#!/data/data/com.termux/files/usr/bin/bash
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PID_FILE="$DIR/.voicestudio.pid"
LOG_FILE="$DIR/.voicestudio.log"
PORT=3900

case "$1" in
  start)
    if [ -f "$PID_FILE" ] && kill -0 "$(cat "$PID_FILE" 2>/dev/null)" 2>/dev/null; then
      echo "VoiceStudio server is already running (PID: $(cat "$PID_FILE"))."
      exit 0
    fi
    echo "Starting VoiceStudio speech backend on port $PORT..."
    cd "$DIR"
    setsid python3 "$DIR/voicestudio_server.py" > "$LOG_FILE" 2>&1 < /dev/null &
    PID=$!
    echo $PID > "$PID_FILE"
    sleep 1
    if kill -0 $PID 2>/dev/null; then
      echo "VoiceStudio started successfully (PID: $PID, URL: http://localhost:$PORT)."
    else
      echo "Failed to start VoiceStudio. Check $LOG_FILE."
      exit 1
    fi
    ;;
  stop)
    if [ -f "$PID_FILE" ]; then
      PID=$(cat "$PID_FILE" 2>/dev/null)
      echo "Stopping VoiceStudio (PID: $PID)..."
      kill $PID 2>/dev/null || true
      rm -f "$PID_FILE"
      echo "VoiceStudio stopped."
    else
      echo "VoiceStudio is not running."
    fi
    ;;
  status)
    if [ -f "$PID_FILE" ] && kill -0 "$(cat "$PID_FILE" 2>/dev/null)" 2>/dev/null; then
      echo "VoiceStudio is running (PID: $(cat "$PID_FILE"), URL: http://localhost:$PORT)."
      curl -s http://localhost:$PORT/health | head -n 5
    else
      echo "VoiceStudio is stopped."
    fi
    ;;
  restart)
    bash "$0" stop
    sleep 1
    bash "$0" start
    ;;
  *)
    echo "Usage: $0 {start|stop|status|restart}"
    exit 1
    ;;
esac
