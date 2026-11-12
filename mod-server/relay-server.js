// Ретранслятор фейк-подарков для Telegram-мода.
// Хранит ТОЛЬКО косметические фейк-подарки в памяти. Никаких данных Telegram.
//
// Запуск:
//   node relay-server.js            (порт 8787)
//   PORT=9000 SECRET=мойсекрет node relay-server.js
//
// Эндпоинты:
//   POST /send    {secret, from, to, gift, name}
//   GET  /pending?secret=...&user=...
//   GET  /health

const http = require('http');

const PORT = process.env.PORT ? parseInt(process.env.PORT, 10) : 8787;
const SECRET = process.env.SECRET || 'changeme';

// user(lowercase) -> [ {gift, name, from, ts} ]
const queues = new Map();

function normalize(u) {
  u = (u || '').trim().toLowerCase();
  return u.startsWith('@') ? u.slice(1) : u;
}

function send(res, code, obj) {
  const body = JSON.stringify(obj);
  res.writeHead(code, { 'Content-Type': 'application/json; charset=utf-8' });
  res.end(body);
}

function readBody(req) {
  return new Promise((resolve) => {
    let data = '';
    req.on('data', (c) => (data += c));
    req.on('end', () => resolve(data));
  });
}

const server = http.createServer(async (req, res) => {
  const url = new URL(req.url, `http://localhost:${PORT}`);

  if (req.method === 'GET' && url.pathname === '/health') {
    return send(res, 200, { ok: true });
  }

  if (req.method === 'POST' && url.pathname === '/send') {
    let body;
    try {
      body = JSON.parse(await readBody(req));
    } catch {
      return send(res, 400, { ok: false, error: 'bad json' });
    }
    if (body.secret !== SECRET) return send(res, 403, { ok: false, error: 'bad secret' });
    const to = normalize(body.to);
    const from = normalize(body.from);
    if (!to || !from) return send(res, 400, { ok: false, error: 'no user' });
    const list = queues.get(to) || [];
    list.push({ gift: body.gift || '', name: body.name || '', from, ts: Date.now() });
    queues.set(to, list);
    return send(res, 200, { ok: true });
  }

  if (req.method === 'GET' && url.pathname === '/pending') {
    if (url.searchParams.get('secret') !== SECRET) {
      return send(res, 403, { ok: false, error: 'bad secret' });
    }
    const user = normalize(url.searchParams.get('user'));
    const list = queues.get(user) || [];
    queues.set(user, []);
    return send(res, 200, list.map((g) => ({ gift: g.gift, name: g.name })));
  }

  send(res, 404, { ok: false, error: 'not found' });
});

server.listen(PORT, () => {
  console.log(`relay listening on ${PORT}`);
});
