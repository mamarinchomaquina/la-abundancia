const Publicacion = require('../models/Publicacion');
const Solicitud   = require('../models/Solicitud');
const Usuario     = require('../models/Usuario');

async function getEstadisticas(_req, res, next) {
  try {
    const [usuarios, publicaciones, solicitudes] = await Promise.all([
      Usuario.countDocuments(),
      Publicacion.aggregate([{ $group: { _id: '$estado', total: { $sum: 1 } } }]),
      Solicitud.aggregate([{ $group: { _id: '$estado', total: { $sum: 1 } } }]),
    ]);
    const totalsByStatus = (rows, statuses) =>
      Object.fromEntries(statuses.map(s => [s, rows.find(r => r._id === s)?.total || 0]));
    return res.json({
      usuarios,
      publicaciones: totalsByStatus(publicaciones, ['pendiente', 'aprobada', 'rechazada', 'entregada']),
      solicitudes:   totalsByStatus(solicitudes,   ['pendiente', 'aceptada', 'rechazada']),
    });
  } catch (error) { return next(error); }
}

async function getGraficas(_req, res, next) {
  try {
    // Publicaciones por categoría
    const porCategoria = await Publicacion.aggregate([
      { $group: { _id: '$categoria', total: { $sum: 1 } } },
      { $sort: { total: -1 } },
    ]);

    // Publicaciones de las últimas 8 semanas
    const ahora = new Date();
    const haceOchoSemanas = new Date(ahora);
    haceOchoSemanas.setDate(ahora.getDate() - 7 * 8);

    const porSemana = await Publicacion.aggregate([
      { $match: { createdAt: { $gte: haceOchoSemanas } } },
      {
        $group: {
          _id: {
            year: { $isoWeekYear: '$createdAt' },
            week: { $isoWeek: '$createdAt' },
          },
          total: { $sum: 1 },
          fecha: { $min: '$createdAt' },
        },
      },
      { $sort: { '_id.year': 1, '_id.week': 1 } },
    ]);

    // Llenar semanas vacías
    const semanas = [];
    for (let i = 7; i >= 0; i--) {
      const d = new Date(ahora);
      d.setDate(ahora.getDate() - 7 * i);
      const year = (() => {
        const jan4 = new Date(d.getFullYear(), 0, 4);
        const startOfWeek = new Date(jan4);
        startOfWeek.setDate(jan4.getDate() - ((jan4.getDay() + 6) % 7));
        return d >= startOfWeek ? d.getFullYear() : d.getFullYear() - 1;
      })();
      const week = (() => {
        const startOfYear = new Date(year, 0, 4);
        const startOfWeek = new Date(startOfYear);
        startOfWeek.setDate(startOfYear.getDate() - ((startOfYear.getDay() + 6) % 7));
        return Math.round((d - startOfWeek) / (7 * 24 * 60 * 60 * 1000)) + 1;
      })();
      const found = porSemana.find(s => s._id.year === year && s._id.week === week);
      semanas.push({
        label: d.toLocaleDateString('es-CO', { month: 'short', day: 'numeric' }),
        total: found ? found.total : 0,
      });
    }

    return res.json({
      porCategoria: porCategoria.map(c => ({ categoria: c._id || 'Sin categoría', total: c.total })),
      porSemana: semanas,
    });
  } catch (error) { return next(error); }
}

module.exports = { getEstadisticas, getGraficas };
