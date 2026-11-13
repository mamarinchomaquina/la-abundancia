const assert = require('node:assert/strict');
const { after, test } = require('node:test');
const app = require('../app');

const server = app.listen(0, '127.0.0.1');

after(() => {
  server.close();
});

test('GET /health reports MongoDB as unavailable when disconnected', async () => {
  const address = server.address();
  const response = await fetch(`http://127.0.0.1:${address.port}/health`);
  const body = await response.json();

  assert.equal(response.status, 503);
  assert.deepEqual(body, {
    status: 'unavailable',
    database: 'disconnected',
  });
});
