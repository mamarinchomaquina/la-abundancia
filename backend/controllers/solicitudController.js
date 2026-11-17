const mongoose = require('mongoose');
const Publicacion = require('../models/Publicacion');
const Solicitud = require('../models/Solicitud');

async function listSolicitudes(req, res, next) {
  try {
    let filter;
    if (req.usuario.rol === 'admin') {
      filter = {};
    } else {
      const publicaciones = await Publicacion.find({ usuario: req.usuario._id }).select('_id');
      filter = {
        $or: [
          { usuario: req.usuario._id },
          { publicacion: { $in: publicaciones.map((item) => item._id) } },
        ],
      };
    }
    const solicitudes = await Solicitud.find(filter)
      .populate('usuario', 'nombre')
      .populate('publicacion', 'titulo categoria ubicacion usuario')
      .sort({ createdAt: -1 });
    return res.json(solicitudes);
  } catch (error) {
    return next(error);
  }
}

async function createSolicitud(req, res, next) {
  try {
    const { publicacion: publicacionId, mensaje = '' } = req.body;
    if (
      Object.keys(req.body).some((field) => !['publicacion', 'mensaje'].includes(field)) ||
      !mongoose.isObjectIdOrHexString(publicacionId) ||
      typeof mensaje !== 'string'
    ) {
      return res.status(400).json({ error: 'Indica una publicación y un mensaje válido.' });
    }

    const publicacion = await Publicacion.findById(publicacionId);
    if (!publicacion || publicacion.estado !== 'aprobada') {
      return res.status(404).json({ error: 'Publicación aprobada no encontrada.' });
    }
    if (publicacion.usuario.toString() === req.usuario._id.toString()) {
      return res.status(400).json({ error: 'No puedes solicitar tu propia publicación.' });
    }

    const existing = await Solicitud.findOne({
      publicacion: publicacion._id,
      usuario: req.usuario._id,
      estado: { $in: ['pendiente', 'aceptada'] },
    });
    if (existing) {
      return res.status(409).json({ error: 'Ya tienes una solicitud activa para esta publicación.' });
    }

    const solicitud = await Solicitud.create({
      publicacion: publicacion._id,
      usuario: req.usuario._id,
      mensaje: mensaje.trim(),
    });
    return res.status(201).json(solicitud);
  } catch (error) {
    if (error.code === 11000) {
      return res.status(409).json({ error: 'Ya tienes una solicitud activa para esta publicación.' });
    }
    return next(error);
  }
}

async function updateEstadoSolicitud(req, res, next) {
  try {
    if (!mongoose.isObjectIdOrHexString(req.params.id)) {
      return res.status(400).json({ error: 'El identificador no es válido.' });
    }
    const { estado } = req.body;
    if (Object.keys(req.body).length !== 1 || !['aceptada', 'rechazada'].includes(estado)) {
      return res.status(400).json({ error: 'El estado debe ser aceptada o rechazada.' });
    }

    const solicitud = await Solicitud.findById(req.params.id).populate('publicacion', 'usuario');
    if (!solicitud) {
      return res.status(404).json({ error: 'Solicitud no encontrada.' });
    }
    const isOwner = solicitud.publicacion.usuario.toString() === req.usuario._id.toString();
    if (req.usuario.rol !== 'admin' && !isOwner) {
      return res.status(403).json({
        error: 'Solo el propietario de la publicación o un admin puede resolverla.',
      });
    }
    if (solicitud.estado !== 'pendiente') {
      return res.status(409).json({ error: 'La solicitud ya fue resuelta.' });
    }

    solicitud.estado = estado;
    await solicitud.save();
    return res.json(solicitud);
  } catch (error) {
    return next(error);
  }
}

module.exports = { listSolicitudes, createSolicitud, updateEstadoSolicitud };
