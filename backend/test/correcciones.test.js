// Pruebas de las correcciones de Desarrollo Web 2: eliminar publicaciones y crear usuarios.
// No usan la base de datos: se reemplazan temporalmente los métodos del modelo.
const assert = require('node:assert/strict');
const { test, afterEach } = require('node:test');
const Publicacion = require('../models/Publicacion');
const Solicitud = require('../models/Solicitud');
const Usuario = require('../models/Usuario');
const { deletePublicacion } = require('../controllers/publicacionController');
const { createUsuario } = require('../controllers/usuariosController');
const { createSolicitud } = require('../controllers/solicitudController');

const originales = {
  findById: Publicacion.findById,
  deleteOne: Publicacion.deleteOne,
  exists: Solicitud.exists,
  deleteMany: Solicitud.deleteMany,
  findOne: Solicitud.findOne,
  create: Solicitud.create,
  usuarioExists: Usuario.exists,
};
afterEach(() => {
  Publicacion.findById = originales.findById;
  Publicacion.deleteOne = originales.deleteOne;
  Solicitud.exists = originales.exists;
  Solicitud.deleteMany = originales.deleteMany;
  Solicitud.findOne = originales.findOne;
  Solicitud.create = originales.create;
  Usuario.exists = originales.usuarioExists;
});

// Respuesta simulada de Express: guarda el código y el cuerpo
function respuesta() {
  return {
    codigo: 200,
    cuerpo: null,
    status(c) { this.codigo = c; return this; },
    json(b) { this.cuerpo = b; return this; },
    end() { return this; },
  };
}
// Llama al controlador; si termina en next(error) el servidor respondería 500
async function ejecutar(controlador, req) {
  const res = respuesta();
  await controlador(req, res, (error) => { res.codigo = 500; res.cuerpo = { error: error.message }; });
  return res;
}

const ID = '6a0000000000000000000001';
const admin = { _id: '6a00000000000000000000aa', rol: 'admin' };

// ── Eliminar publicación ──
test('No elimina una publicación que tiene solicitudes (responde 409 y conserva el historial)', async () => {
  const llamadas = [];
  Publicacion.findById = async () => ({ _id: ID, usuario: admin._id, estado: 'entregada', imagen: null });
  Solicitud.exists = async () => ({ _id: 'una-solicitud' });
  Solicitud.deleteMany = async () => { llamadas.push('Solicitud.deleteMany'); };
  Publicacion.deleteOne = async () => { llamadas.push('Publicacion.deleteOne'); };

  const res = await ejecutar(deletePublicacion, { params: { id: ID }, usuario: admin });

  assert.equal(res.codigo, 409, `respondió ${res.codigo} y ejecutó: ${llamadas.join(', ') || 'nada'}`);
  assert.deepEqual(llamadas, [], 'no debe borrar nada');
});

test('Sí elimina una publicación sin solicitudes (responde 204)', async () => {
  const llamadas = [];
  Publicacion.findById = async () => ({ _id: ID, usuario: admin._id, estado: 'pendiente', imagen: null });
  Solicitud.exists = async () => null;
  Solicitud.deleteMany = async () => { llamadas.push('Solicitud.deleteMany'); };
  Publicacion.deleteOne = async () => { llamadas.push('Publicacion.deleteOne'); };

  const res = await ejecutar(deletePublicacion, { params: { id: ID }, usuario: admin });

  assert.equal(res.codigo, 204);
  assert.ok(llamadas.includes('Publicacion.deleteOne'));
});

// ── Crear usuario ──
const casosUsuario = [
  ['sin nombre', { email: 'ana@correo.com', password: '12345678' }],
  ['sin correo', { nombre: 'Ana', password: '12345678' }],
  ['sin contraseña', { nombre: 'Ana', email: 'ana@correo.com' }],
  ['cuerpo vacío', {}],
];
for (const [nombre, body] of casosUsuario) {
  test(`Crear usuario ${nombre} responde 400 con mensaje (no 500)`, async () => {
    Usuario.exists = async () => null;
    const res = await ejecutar(createUsuario, { body, usuario: admin });
    assert.equal(res.codigo, 400, `respondió ${res.codigo}: ${res.cuerpo?.error}`);
    assert.equal(typeof res.cuerpo.error, 'string');
  });
}

// ── Solicitudes simultáneas ──
test('El modelo Solicitud tiene un índice único para usuario y publicación (solo activas)', () => {
  const indice = Solicitud.schema.indexes().find(([campos]) => campos.publicacion === 1 && campos.usuario === 1);
  assert.ok(indice, 'no existe un índice sobre { publicacion, usuario }');
  assert.equal(indice[1].unique, true);
  assert.deepEqual(indice[1].partialFilterExpression, { estado: { $in: ['pendiente', 'aceptada'] } });
});

test('Si la base rechaza una solicitud duplicada (dos peticiones a la vez) responde 409, no 500', async () => {
  Publicacion.findById = async () => ({ _id: ID, usuario: '6a00000000000000000000bb', estado: 'aprobada' });
  Solicitud.findOne = async () => null; // las dos peticiones pasaron la consulta al mismo tiempo
  Solicitud.create = async () => { const e = new Error('E11000 duplicate key error'); e.code = 11000; throw e; };
  const solicitante = { _id: '6a00000000000000000000cc', rol: 'usuario' };
  const res = await ejecutar(createSolicitud, { body: { publicacion: ID, mensaje: 'La necesito' }, usuario: solicitante });
  assert.equal(res.codigo, 409, `respondió ${res.codigo}: ${res.cuerpo?.error}`);
  assert.equal(res.cuerpo.error, 'Ya tienes una solicitud activa para esta publicación.');
});
