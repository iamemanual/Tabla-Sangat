const http = require('http');
const fs = require('fs');
const path = require('path');
const os = require('os');

const PORT = parseInt(process.env.PORT || '8080', 10);
const HOST = '0.0.0.0';
const ROOT_DIR = path.resolve(__dirname);
const PID_FILE = path.join(ROOT_DIR, '.server.pid');
const PENDING_EDITS_FILE = path.join(ROOT_DIR, 'pending_edits.json');

const MIME_TYPES = {
  '.html': 'text/html; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.js': 'application/javascript; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.jpeg': 'image/jpeg',
  '.gif': 'image/gif',
  '.webp': 'image/webp',
  '.svg': 'image/svg+xml',
  '.wav': 'audio/wav',
  '.mp3': 'audio/mpeg',
  '.ogg': 'audio/ogg',
  '.ico': 'image/x-icon',
  '.txt': 'text/plain; charset=utf-8',
  '.woff': 'font/woff',
  '.woff2': 'font/woff2',
  '.ttf': 'font/ttf'
};

const ALLOWED_EXTENSIONS = new Set(Object.keys(MIME_TYPES));
const BLOCKED_DIRECTORIES = new Set(['app', 'backups', 'gradle', '.gradle', 'node_modules', '.git']);

function readPendingEdits() {
  try {
    if (fs.existsSync(PENDING_EDITS_FILE)) {
      return JSON.parse(fs.readFileSync(PENDING_EDITS_FILE, 'utf8'));
    }
  } catch (e) {}
  return [];
}

function savePendingEdits(edits) {
  try {
    fs.writeFileSync(PENDING_EDITS_FILE, JSON.stringify(edits, null, 2), 'utf8');
  } catch (e) {
    console.error('Error saving pending edits:', e.message);
  }
}

function parseSafeJsonBody(req, res, maxBytes = 2 * 1024 * 1024, callback) {
  let body = '';
  let received = 0;
  let aborted = false;

  req.on('data', chunk => {
    if (aborted) return;
    received += chunk.length;
    if (received > maxBytes) {
      aborted = true;
      req.destroy();
      res.writeHead(413, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ error: 'Payload too large' }));
      return;
    }
    body += chunk;
  });

  req.on('end', () => {
    if (aborted) return;
    try {
      const parsed = body ? JSON.parse(body) : {};
      callback(null, parsed);
    } catch (err) {
      res.writeHead(400, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ error: 'Invalid JSON payload: ' + err.message }));
    }
  });
}

function getWifiIp() {
  try {
    const interfaces = os.networkInterfaces();
    for (const name of Object.keys(interfaces)) {
      if (/wlan|wifi|ap|rndis|eth/i.test(name)) {
        for (const iface of interfaces[name]) {
          if (iface.family === 'IPv4' && !iface.internal && iface.address !== '127.0.0.1') {
            return iface.address;
          }
        }
      }
    }
    for (const name of Object.keys(interfaces)) {
      if (!/dummy|rmnet|ccmni|pdp/i.test(name)) {
        for (const iface of interfaces[name]) {
          if (iface.family === 'IPv4' && !iface.internal && /^(192\.168\.|10\.|172\.(1[6-9]|2[0-9]|3[0-1])\.)/.test(iface.address)) {
            return iface.address;
          }
        }
      }
    }
  } catch (_) {}
  return 'localhost';
}

const wifiIp = getWifiIp();

function applySecurityHeaders(res) {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, HEAD, OPTIONS, DELETE');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization');
  res.setHeader('X-Content-Type-Options', 'nosniff');
  res.setHeader('X-Frame-Options', 'SAMEORIGIN');
  res.setHeader('X-XSS-Protection', '1; mode=block');
  res.setHeader('Referrer-Policy', 'strict-origin-when-cross-origin');
  res.setHeader('Content-Security-Policy', "default-src 'self' 'unsafe-inline' 'unsafe-eval' data: blob: https://eqwgrxcitwoyickthfpd.supabase.co https://fonts.googleapis.com https://fonts.gstatic.com;");
}

const server = http.createServer((req, res) => {
  applySecurityHeaders(res);

  if (req.method === 'OPTIONS') {
    res.writeHead(204);
    res.end();
    return;
  }

  // Safe URI parsing with malformed URI protection
  let parsedUrl;
  let pathname;
  try {
    parsedUrl = new URL(req.url, 'http://localhost');
    pathname = decodeURIComponent(parsedUrl.pathname);
  } catch (err) {
    res.writeHead(400, { 'Content-Type': 'text/plain' });
    res.end('400 Bad Request: Malformed URI');
    return;
  }

  // Null byte injection check
  if (pathname.includes('\0')) {
    res.writeHead(400, { 'Content-Type': 'text/plain' });
    res.end('400 Bad Request');
    return;
  }

  // Status API
  if (pathname === '/api/status' || pathname === '/api/health') {
    const payload = JSON.stringify({
      status: 'online',
      app: 'Tabla Sangat',
      port: PORT,
      localUrl: `http://localhost:${PORT}`,
      wifiUrl: `http://${wifiIp}:${PORT}`,
      uptimeSeconds: Math.floor(process.uptime()),
      timestamp: new Date().toISOString()
    }, null, 2);

    res.writeHead(200, { 'Content-Type': 'application/json' });
    if (req.method === 'HEAD') {
      res.end();
    } else {
      res.end(payload);
    }
    return;
  }

  // 1. Pinpoint API: Submit Edit from In-Page Inspector
  if (pathname === '/api/edits/submit' && req.method === 'POST') {
    parseSafeJsonBody(req, res, 2 * 1024 * 1024, (err, item) => {
      if (err || !item) return;
      try {
        item.id = Date.now();
        item.status = 'pending';

        const edits = readPendingEdits();
        edits.push(item);
        savePendingEdits(edits);

        console.log('\n======================================================');
        console.log('  🎯 [PINPOINT RECEIVED FROM LOCALHOST / TABLA]');
        console.log(`  Target:      ${item.selector} (<${item.tagName}>)`);
        if (item.pageUrl) console.log(`  Page:        \x1b[33m${item.pageUrl}\x1b[0m`);
        if (item.isHtmlEdit || item.html) {
          console.log(`  Mode:        \x1b[35mHTML Edit (${item.htmlScope || 'outer'})\x1b[0m`);
          if (item.html) console.log(`  New HTML:    \x1b[32m"${item.html.slice(0, 80).replace(/\n/g, ' ')}..."\x1b[0m`);
        }
        if (item.innerText && !item.isHtmlEdit) console.log(`  Content:     "${item.innerText.slice(0, 60)}"`);
        console.log(`  Instruction: \x1b[36m${(item.instruction || '').length > 80 ? item.instruction.slice(0, 80) + '...' : item.instruction}\x1b[0m`);
        console.log(`  Timestamp:   ${item.timestamp || new Date().toISOString()}`);
        console.log('======================================================\n');

        res.writeHead(200, { 'Content-Type': 'application/json' });
        return res.end(JSON.stringify({ success: true, item, total: edits.length }));
      } catch (e) {
        res.writeHead(400, { 'Content-Type': 'application/json' });
        return res.end(JSON.stringify({ error: e.message }));
      }
    });
    return;
  }

  // 2. Pinpoint API: Get Pending Edits
  if (pathname === '/api/edits/pending' && req.method === 'GET') {
    const edits = readPendingEdits();
    res.writeHead(200, { 'Content-Type': 'application/json' });
    return res.end(JSON.stringify({ count: edits.length, edits }));
  }

  // 3. Pinpoint API: Clear Pending Edits
  if (pathname === '/api/edits/clear' && req.method === 'POST') {
    savePendingEdits([]);
    res.writeHead(200, { 'Content-Type': 'application/json' });
    return res.end(JSON.stringify({ success: true, message: 'Cleared' }));
  }

  // 4. Pinpoint API: Delete Specific Pending Edit
  if (pathname === '/api/edits/delete' && req.method === 'POST') {
    parseSafeJsonBody(req, res, 512 * 1024, (err, payload) => {
      if (err || !payload) return;
      try {
        let edits = readPendingEdits();
        edits = edits.filter(e => e.id !== payload.id);
        savePendingEdits(edits);
        res.writeHead(200, { 'Content-Type': 'application/json' });
        return res.end(JSON.stringify({ success: true, count: edits.length }));
      } catch (e) {
        res.writeHead(400, { 'Content-Type': 'application/json' });
        return res.end(JSON.stringify({ error: e.message }));
      }
    });
    return;
  }

  // 5. VoiceStudio Speech Proxy API (proxies to http://127.0.0.1:3900)
  if (pathname.startsWith('/api/voice/')) {
    const subpath = pathname.replace(/^\/api\/voice/, '') || '/';
    const vsTarget = `http://127.0.0.1:3900${subpath}${parsedUrl.search || ''}`;
    const vsReq = http.request(vsTarget, {
      method: req.method,
      headers: {
        ...req.headers,
        host: '127.0.0.1:3900'
      }
    }, (vsRes) => {
      res.writeHead(vsRes.statusCode, vsRes.headers);
      vsRes.pipe(res);
    });

    vsReq.on('error', (err) => {
      res.writeHead(503, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({
        error: 'VoiceStudio backend is offline on port 3900',
        message: 'Start with bash start_voicestudio.sh start',
        details: err.message
      }));
    });

    req.pipe(vsReq);
    return;
  }

  // Static files only allow GET and HEAD
  if (req.method !== 'GET' && req.method !== 'HEAD') {
    res.writeHead(405, { 'Content-Type': 'text/plain' });
    res.end('405 Method Not Allowed');
    return;
  }

  // Normalize root path to index.html
  if (pathname === '/' || pathname === '') {
    pathname = '/index.html';
  }

  // Secure path normalization & traversal resolution
  const normalizedRel = path.normalize(pathname).replace(/^(\.\.[\/\\])+/, '');
  let filePath = path.resolve(ROOT_DIR, '.' + path.sep + normalizedRel);

  // Prefix verification: prevent directory breakout and prefix confusion
  if (filePath !== ROOT_DIR && !filePath.startsWith(ROOT_DIR + path.sep)) {
    res.writeHead(403, { 'Content-Type': 'text/plain' });
    res.end('403 Forbidden');
    return;
  }

  // Disallow hidden files/directories (starting with dot)
  const relativeFromRoot = path.relative(ROOT_DIR, filePath);
  const segments = relativeFromRoot.split(path.sep);
  if (segments.some(seg => seg.startsWith('.'))) {
    res.writeHead(403, { 'Content-Type': 'text/plain' });
    res.end('403 Forbidden: Hidden files cannot be accessed');
    return;
  }

  // Block access to project source code and sensitive directories
  const firstDir = segments[0];
  if (BLOCKED_DIRECTORIES.has(firstDir)) {
    res.writeHead(403, { 'Content-Type': 'text/plain' });
    res.end('403 Forbidden: Project source and configuration directories are protected');
    return;
  }

  fs.stat(filePath, (err, stats) => {
    if (err) {
      // SPA Fallback: ONLY serve index.html for clean extensionless route paths (e.g., /tuner, /tanpura)
      const hasExt = Boolean(path.extname(pathname));
      const acceptsHtml = (req.headers.accept || '').includes('text/html') || !req.headers.accept;
      if (!hasExt && acceptsHtml) {
        const indexPath = path.join(ROOT_DIR, 'index.html');
        fs.stat(indexPath, (indexErr, indexStats) => {
          if (indexErr || !indexStats.isFile()) {
            res.writeHead(404, { 'Content-Type': 'text/plain' });
            res.end('404 Not Found');
            return;
          }

          if (req.method === 'HEAD') {
            res.writeHead(200, {
              'Content-Type': 'text/html; charset=utf-8',
              'Content-Length': indexStats.size
            });
            res.end();
            return;
          }

          fs.readFile(indexPath, (readErr, data) => {
            if (readErr) {
              res.writeHead(500, { 'Content-Type': 'text/plain' });
              res.end('500 Internal Server Error');
            } else {
              res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
              res.end(data);
            }
          });
        });
        return;
      }

      res.writeHead(404, { 'Content-Type': 'text/plain' });
      res.end('404 Not Found');
      return;
    }

    if (stats.isDirectory()) {
      filePath = path.join(filePath, 'index.html');
    }

    // Extension whitelist enforcement
    const ext = path.extname(filePath).toLowerCase();
    if (!ALLOWED_EXTENSIONS.has(ext)) {
      res.writeHead(403, { 'Content-Type': 'text/plain' });
      res.end('403 Forbidden: Requested file type is not allowed');
      return;
    }

    const contentType = MIME_TYPES[ext] || 'application/octet-stream';

    if (req.method === 'HEAD') {
      res.writeHead(200, {
        'Content-Type': contentType,
        'Content-Length': stats.size
      });
      res.end();
      return;
    }

    fs.readFile(filePath, (readErr, data) => {
      if (readErr) {
        res.writeHead(500, { 'Content-Type': 'text/plain' });
        res.end('500 Internal Server Error');
        return;
      }
      res.writeHead(200, { 'Content-Type': contentType });
      res.end(data);
    });
  });
});

server.on('clientError', (err, socket) => {
  if (err.code === 'ECONNRESET' || !socket.writable) return;
  socket.end('HTTP/1.1 400 Bad Request\r\n\r\n');
});

process.on('uncaughtException', (err) => {
  console.error('[server error] Uncaught exception:', err.message);
});

server.listen(PORT, HOST, () => {
  try {
    fs.writeFileSync(PID_FILE, process.pid.toString(), 'utf8');
  } catch (_) {}
  console.log(`\n======================================================`);
  console.log(`🥁 Tabla Sangat — Localhost Server Active (Pinpoint Live)`);
  console.log(`📱 Local:   http://localhost:${PORT}`);
  console.log(`📶 Wi-Fi:   http://${wifiIp}:${PORT}`);
  console.log(`🎯 Pinpoint: In-page visual inspector active`);
  console.log(`======================================================\n`);
});

process.on('SIGTERM', () => {
  try { fs.unlinkSync(PID_FILE); } catch (_) {}
  process.exit(0);
});
process.on('SIGINT', () => {
  try { fs.unlinkSync(PID_FILE); } catch (_) {}
  process.exit(0);
});
