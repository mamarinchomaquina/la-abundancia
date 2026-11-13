const assert = require('node:assert/strict');
const { test } = require('node:test');
const { transicionPermitida } = require('../controllers/publicacionController');

test('una publicación pendiente puede aprobarse o rechazarse', () => {
  assert.equal(transicionPermitida('pendiente', 'aprobada'), true);
  assert.equal(transicionPermitida('pendiente', 'rechazada'), true);
});

test('una publicación aprobada solo puede pasar a entregada', () => {
  assert.equal(transicionPermitida('aprobada', 'entregada'), true);
  assert.equal(transicionPermitida('aprobada', 'pendiente'), false);
  assert.equal(transicionPermitida('aprobada', 'rechazada'), false);
});

test('rechazada y entregada son estados finales', () => {
  for (const nuevo of ['pendiente', 'aprobada', 'rechazada', 'entregada']) {
    assert.equal(transicionPermitida('rechazada', nuevo), false);
    assert.equal(transicionPermitida('entregada', nuevo), false);
  }
});

test('no se puede saltar de pendiente a entregada', () => {
  assert.equal(transicionPermitida('pendiente', 'entregada'), false);
});
