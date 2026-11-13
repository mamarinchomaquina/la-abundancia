const assert = require('node:assert/strict');
const { test } = require('node:test');
const Publicacion = require('../models/Publicacion');
const Solicitud = require('../models/Solicitud');
const Usuario = require('../models/Usuario');

test('publicación requires valid offer fields and starts pending', async () => {
  const publicacion = new Publicacion({
    titulo: 'Bolsa de ropa',
    descripcion: 'Ropa en buen estado',
    categoria: 'Ropa',
    cantidad: 8,
    ubicacion: 'Pereira',
    usuario: '507f1f77bcf86cd799439011',
  });

  await publicacion.validate();
  assert.equal(publicacion.estado, 'pendiente');
  assert.ok(publicacion.fecha instanceof Date);
});

test('publicación rejects unsupported category and non-integer quantity', async () => {
  const publicacion = new Publicacion({
    titulo: 'Oferta',
    descripcion: 'Descripción',
    categoria: 'Muebles',
    cantidad: 1.5,
    ubicacion: 'Pereira',
    usuario: '507f1f77bcf86cd799439011',
  });
  await assert.rejects(publicacion.validate(), (error) => {
    assert.ok(error.errors.categoria);
    assert.ok(error.errors.cantidad);
    return true;
  });
});

test('new accounts default to the non-admin role and hide passwords', () => {
  const usuario = new Usuario({
    nombre: 'Ana',
    email: 'ana@example.com',
    password: 'a-secure-password',
  });

  assert.equal(usuario.rol, 'usuario');
  assert.equal(usuarioSchemaPasswordIsHidden(), true);
});

test('solicitudes default to pending', async () => {
  const solicitud = new Solicitud({
    publicacion: '507f1f77bcf86cd799439011',
    usuario: '507f1f77bcf86cd799439012',
  });

  await solicitud.validate();
  assert.equal(solicitud.estado, 'pendiente');
});

function usuarioSchemaPasswordIsHidden() {
  return Usuario.schema.path('password').options.select === false;
}
