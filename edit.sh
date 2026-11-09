#!/data/data/com.termux/files/usr/bin/bash
# ==============================================================================
# 🎯 Tabla Sangat — Localhost Pinpoint & Live Edit Manager
# ==============================================================================
PORT=8080
SERVER_URL="http://localhost:${PORT}"
PROJECT_DIR="/storage/emulated/0/Workspace/Tabla App"

case "$1" in
  list|pending)
    echo ""
    echo "======================================================"
    echo "  🎯 TABLA SANGAT — PENDING LOCALHOST PINPOINT EDITS"
    echo "======================================================"
    node -e '
      const http = require("http");
      http.get("'"${SERVER_URL}"'/api/edits/pending", (res) => {
        let body = "";
        res.on("data", chunk => body += chunk);
        res.on("end", () => {
          try {
            const data = JSON.parse(body);
            const edits = data.edits || [];
            if (edits.length === 0) {
              console.log("\n  No pending edits. Use 🎯 Pinpoint & Edit on http://localhost:'"${PORT}"' to select elements.\n");
              return;
            }
            console.log("\n  Found " + edits.length + " pending edit(s):\n");
            edits.forEach((e, i) => {
              console.log(`[#${i + 1}] Target:    \x1b[33m${e.selector}\x1b[0m (<${e.tagName || "DIV"}>)`);
              if (e.innerText) console.log(`     Text:      "${e.innerText.trim().slice(0, 60)}"`);
              console.log(`     Instruction: \x1b[36m${e.instruction}\x1b[0m`);
              console.log("------------------------------------------------------");
            });
            console.log("");
          } catch (err) {
            console.error("Could not parse edits:", err.message);
          }
        });
      }).on("error", (e) => {
        console.error("Server not reachable on '"${SERVER_URL}"':", e.message);
      });
    '
    ;;

  clear)
    curl -s -X POST "${SERVER_URL}/api/edits/clear" > /dev/null
    echo "✅ Cleared all pending pinpoint edits."
    ;;

  status)
    curl -s "${SERVER_URL}/api/status" | grep -v '^[[:space:]]*$'
    ;;

  open)
    if command -v termux-open-url &> /dev/null; then
      termux-open-url "${SERVER_URL}"
    elif command -v am &> /dev/null; then
      am start -a android.intent.action.VIEW -d "${SERVER_URL}" > /dev/null 2>&1
    else
      echo "Open ${SERVER_URL} in your browser."
    fi
    ;;

  restart)
    pkill -f "node.*server.js" 2>/dev/null
    sleep 1
    cd "${PROJECT_DIR}" && nohup node server.js > /dev/null 2>&1 &
    sleep 1
    echo "✅ Restarted Tabla Sangat server on ${SERVER_URL}"
    ;;

  *)
    echo "Usage: tabla-edit {list|clear|status|open|restart}"
    echo "  list    - Show all elements pinpointed from localhost:8080"
    echo "  clear   - Clear pending edits queue"
    echo "  status  - Show server health and Wi-Fi URL"
    echo "  open    - Open localhost:8080 in browser"
    echo "  restart - Restart server process"
    ;;
esac
