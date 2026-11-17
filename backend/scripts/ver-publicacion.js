// Muestra cuántas publicaciones hay en MongoDB con un título y sus datos (para comprobar que no hay duplicados).
// Uso:  cd backend  →  node scripts/ver-publicacion.js "Título de la publicación"
require('dotenv').config();
const mongoose = require('mongoose');
const Publicacion = require('../models/Publicacion');

async function main() {
  const titulo = process.argv[2];
  if (!titulo) { console.log('Uso: node scripts/ver-publicacion.js "Título"'); return; }
  await mongoose.connect(process.env.MONGODB_URI);
  const lista = await Publicacion.find({ titulo }).sort({ createdAt: 1 });
  console.log(`Consulta en MongoDB (${new Date().toLocaleString('es-CO')}) — título: "${titulo}"`);
  console.log(`Publicaciones encontradas: ${lista.length}`);
  for (const p of lista) {
    console.log(`  _id: ${p._id}  clienteId: ${p.clienteId ?? '—'}  estado: ${p.estado}  creada: ${p.createdAt.toLocaleString('es-CO')}`);
  }
  await mongoose.disconnect();
}
main().catch(async (e) => { console.error('Error:', e.message); await mongoose.disconnect(); process.exit(1); });
