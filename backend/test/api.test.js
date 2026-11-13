const assert = require('node:assert/strict');
const { after, test } = require('node:test');
const app = require('../app');

const server = app.listen(0, '127.0.0.1');

after(() => {
  server.close();
});

async function request(path, options = {}) {
  const address = server.address();
  return fetch(`http://127.0.0.1:${address.port}${path}`, options);
}

test('registration rejects invalid input before database access', async () => {
  const response = await request('/api/auth/registro', {
    method: 'POST',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify({ email: 'not-an-email', password: 'short', rol: 'admin' }),
  });

  assert.equal(response.status, 400);
});

test('publication creation requires authentication', async () => {
  const response = await request('/api/publicaciones', {
    method: 'POST',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify({ titulo: 'Oferta' }),
  });
  const body = await response.json();

  assert.equal(response.status, 401);
  assert.equal(body.error, 'Se requiere autenticación.');
});

test('statistics require authentication', async () => {
  const response = await request('/api/estadisticas');

  assert.equal(response.status, 401);
});
