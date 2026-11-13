const assert = require('node:assert/strict');
const { after, test } = require('node:test');
const app = require('../app');
const { tipoPermitido, TAMANO_MAXIMO } = require('../middleware/upload');

const server = app.listen(0, '127.0.0.1');
after(() => server.close());

test('solo se aceptan imágenes JPG, PNG o WEBP', () => {
  assert.equal(tipoPermitido('image/jpeg'), true);
  assert.equal(tipoPermitido('image/png'), true);
  assert.equal(tipoPermitido('image/webp'), true);
  assert.equal(tipoPermitido('application/pdf'), false);
  assert.equal(tipoPermitido('image/gif'), false);
});

test('el tamaño máximo de la foto es 2 MB', () => {
  assert.equal(TAMANO_MAXIMO, 2 * 1024 * 1024);
});

test('subir una foto requiere autenticación', async () => {
  const form = new FormData();
  form.append('imagen', new Blob([new Uint8Array([1, 2, 3])], { type: 'image/png' }), 'foto.png');
  const { port } = server.address();
  const res = await fetch(`http://127.0.0.1:${port}/api/publicaciones/507f1f77bcf86cd799439011/imagen`, {
    method: 'POST',
    body: form,
  });
  assert.equal(res.status, 401);
});

// Multer probado solo (sin base de datos): formato, tamaño y archivo correcto
const express = require('express');
const { recibirImagen, borrarImagen } = require('../middleware/upload');
const mini = express();
mini.post('/subir', recibirImagen, (req, res) => res.json({ archivo: req.file.filename }));
const miniServer = mini.listen(0, '127.0.0.1');
after(() => miniServer.close());

async function subir(contenido, tipo, nombre) {
  const form = new FormData();
  form.append('imagen', new Blob([contenido], { type: tipo }), nombre);
  const { port } = miniServer.address();
  return fetch(`http://127.0.0.1:${port}/subir`, { method: 'POST', body: form });
}

test('rechaza un archivo que no es imagen', async () => {
  const res = await subir(new Uint8Array([37, 80, 68, 70]), 'application/pdf', 'doc.pdf');
  assert.equal(res.status, 400);
  assert.match((await res.json()).error, /Formato no permitido/);
});

test('rechaza una imagen de más de 2 MB', async () => {
  const res = await subir(new Uint8Array(2 * 1024 * 1024 + 10), 'image/jpeg', 'grande.jpg');
  assert.equal(res.status, 400);
  assert.match((await res.json()).error, /2 MB/);
});

test('rechaza la petición si no trae imagen', async () => {
  const { port } = miniServer.address();
  const res = await fetch(`http://127.0.0.1:${port}/subir`, { method: 'POST', body: new FormData() });
  assert.equal(res.status, 400);
});

test('acepta una imagen PNG válida y la guarda con nombre aleatorio', async () => {
  const res = await subir(new Uint8Array([137, 80, 78, 71]), 'image/png', 'foto.png');
  assert.equal(res.status, 200);
  const { archivo } = await res.json();
  assert.match(archivo, /^[0-9a-f-]{36}\.png$/);
  borrarImagen(archivo);
});
