require('dotenv').config();

const mongoose = require('mongoose');
const app = require('./app');
const { anunciarServidor } = require('./descubrimiento');

const port = process.env.PORT || 3000;
const mongoUri = process.env.MONGODB_URI;

if (!mongoUri) {
  console.error('Falta configurar MONGODB_URI en el archivo .env.');
  process.exit(1);
}

async function start() {
  try {
    await mongoose.connect(mongoUri);
    console.log('Conexión a MongoDB establecida.');

    app.listen(port, () => {
      console.log(`Servidor escuchando en http://localhost:${port}`);
      anunciarServidor(port);
    });
  } catch (error) {
    console.error('No se pudo conectar a MongoDB:', error.message);
    process.exit(1);
  }
}

start();
