// Envía DOS solicitudes al mismo tiempo, del mismo usuario y para la misma publicación,
// usando el controlador real y la base de datos real. Al final borra los datos de prueba.
// Uso (con MongoDB encendido):  cd backend  →  node scripts/probar-solicitudes-simultaneas.js
require('dotenv').config();
const mongoose = require('mongoose');
const Usuario = require('../models/Usuario');
const Publicacion = require('../models/Publicacion');
const Solicitud = require('../models/Solicitud');
const { createSolicitud } = require('../controllers/solicitudController');

function respuesta() {
  return { codigo: 200, cuerpo: null, status(c) { this.codigo = c; return this; }, json(b) { this.cuerpo = b; return this; } };
}

async function main() {
  await mongoose.connect(process.env.MONGODB_URI);
  console.log('Base de datos:', mongoose.connection.name);
  try { await Solicitud.init(); } catch (e) { console.log('No se pudo crear el índice:', e.message); }
  const indices = await Solicitud.collection.indexes();
  console.log('Índices de solicitudes:', indices.map(i => i.name).join(', '));

  const marca = Date.now();
  const donante = await Usuario.create({ nombre: 'Prueba Donante', email: `donante.${marca}@prueba.local`, password: 'x' });
  const solicitante = await Usuario.create({ nombre: 'Prueba Solicitante', email: `solicitante.${marca}@prueba.local`, password: 'x' });
  const pub = await Publicacion.create({ titulo: 'Prueba simultánea', descripcion: 'Datos temporales', categoria: 'Otros', cantidad: 1, ubicacion: 'Prueba', estado: 'aprobada', usuario: donante._id });

  try {
    const pedir = async () => {
      const res = respuesta();
      await createSolicitud({ body: { publicacion: pub._id.toString(), mensaje: 'La necesito' }, usuario: solicitante },
        res, (e) => { res.codigo = 500; res.cuerpo = { error: e.message }; });
      return res;
    };
    const [a, b] = await Promise.all([pedir(), pedir()]); // las dos al mismo tiempo
    console.log('\nPetición 1 → HTTP', a.codigo, a.cuerpo?.error ? JSON.stringify(a.cuerpo.error) : '(solicitud creada)');
    console.log('Petición 2 → HTTP', b.codigo, b.cuerpo?.error ? JSON.stringify(b.cuerpo.error) : '(solicitud creada)');
    const total = await Solicitud.countDocuments({ publicacion: pub._id, usuario: solicitante._id });
    console.log(`\nSolicitudes guardadas en MongoDB para ese usuario y esa publicación: ${total}`);
    console.log(total === 1 ? 'CORRECTO: solo quedó una solicitud.' : 'ERROR: quedaron solicitudes duplicadas.');
  } finally {
    await Solicitud.deleteMany({ publicacion: pub._id });
    await Publicacion.deleteOne({ _id: pub._id });
    await Usuario.deleteMany({ _id: { $in: [donante._id, solicitante._id] } });
    console.log('(Datos de prueba eliminados)');
    await mongoose.disconnect();
  }
}
main().catch(async (e) => { console.error('Error:', e.message); await mongoose.disconnect(); process.exit(1); });
