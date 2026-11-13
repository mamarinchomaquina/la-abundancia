const path = require('node:path');
const express = require('express');
const mongoose = require('mongoose');
const cors = require('cors');
const authRoutes = require('./routes/authRoutes');
const publicacionRoutes = require('./routes/publicacionRoutes');
const solicitudRoutes = require('./routes/solicitudRoutes');
const { optionalAuth, requireAuth, requireRole } = require('./middleware/auth');
const { getEstadisticas, getGraficas } = require('./controllers/estadisticasController');
const usuariosRoutes = require('./routes/usuariosRoutes');

const app = express();

app.use(cors());
app.use(express.json());
app.use((req, _res, next) => {
  if (req.body === undefined) req.body = {};
  next();
});
app.use((req, res, next) => {
  if (!req.body || typeof req.body !== 'object' || Array.isArray(req.body)) {
    return res.status(400).json({ error: 'El cuerpo de la solicitud debe ser un objeto JSON.' });
  }
  return next();
});

// Fotos subidas con Multer
app.use('/uploads', express.static(path.join(__dirname, 'uploads'), { maxAge: '7d' }));

app.get('/health', (_req, res) => {
  const databaseConnected = mongoose.connection.readyState === 1;

  res.status(databaseConnected ? 200 : 503).json({
    status: databaseConnected ? 'ok' : 'unavailable',
    database: databaseConnected ? 'connected' : 'disconnected',
  });
});

app.use('/api/auth', authRoutes);
app.use('/api/publicaciones', optionalAuth, publicacionRoutes);
app.use('/api/solicitudes', requireAuth, solicitudRoutes);
app.get('/api/estadisticas', requireAuth, requireRole('admin'), getEstadisticas);
app.get('/api/graficas', requireAuth, requireRole('admin'), getGraficas);
app.use('/api/usuarios', requireAuth, requireRole('admin'), usuariosRoutes);


app.use((req, res) => {
  res.status(404).json({ error: 'Ruta no encontrada.' });
});

app.use((error, _req, res, _next) => {
  if (error instanceof SyntaxError && error.status === 400 && 'body' in error) {
    return res.status(400).json({ error: 'El cuerpo de la solicitud no es JSON válido.' });
  }

  if (error.name === 'ValidationError') {
    return res.status(400).json({ error: error.message });
  }

  console.error('Error en la solicitud:', error);
  return res.status(500).json({ error: 'Error interno del servidor.' });
});

module.exports = app;
