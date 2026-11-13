const mongoose = require('mongoose');
const Publicacion = require('../models/Publicacion');
const Solicitud = require('../models/Solicitud');
const { borrarImagen } = require('../middleware/upload');

const editableFields = ['titulo', 'descripcion', 'categoria', 'cantidad', 'ubicacion'];
const validStatuses = ['pendiente', 'aprobada', 'rechazada', 'entregada'];

// Flujo de una publicación: pendiente → aprobada | rechazada; aprobada → entregada.
// "rechazada" y "entregada" son estados finales.
const transiciones = {
  pendiente: ['aprobada', 'rechazada'],
  aprobada: ['entregada'],
  rechazada: [],
  entregada: [],
};

function transicionPermitida(actual, nuevo) {
  return (transiciones[actual] || []).includes(nuevo);
}

function isAdmin(req) {
  return req.usuario?.rol === 'admin';
}

function hasOnlyAllowedFields(body, allowedFields) {
  return Object.keys(body).every((field) => allowedFields.includes(field));
}

async function listPublicaciones(req, res, next) {
  try {
    const filter = isAdmin(req)
      ? {}
      : req.usuario
        ? { $or: [{ estado: 'aprobada' }, { usuario: req.usuario._id }] }
        : { estado: 'aprobada' };
    const publicaciones = await Publicacion.find(filter)
      .populate('usuario', 'nombre')
      .sort({ createdAt: -1 });
    return res.json(publicaciones);
  } catch (error) {
    return next(error);
  }
}

async function getPublicacion(req, res, next) {
  try {
    if (!mongoose.isObjectIdOrHexString(req.params.id)) {
      return res.status(400).json({ error: 'El identificador no es válido.' });
    }
    const publicacion = await Publicacion.findById(req.params.id).populate('usuario', 'nombre');
    if (
      !publicacion ||
      (!isAdmin(req) &&
        publicacion.estado !== 'aprobada' &&
        publicacion.usuario._id.toString() !== req.usuario?._id.toString())
    ) {
      return res.status(404).json({ error: 'Publicación no encontrada.' });
    }
    return res.json(publicacion);
  } catch (error) {
    return next(error);
  }
}

async function createPublicacion(req, res, next) {
  const { clienteId } = req.body;
  try {
    if (!hasOnlyAllowedFields(req.body, [...editableFields, 'clienteId'])) {
      return res.status(400).json({ error: 'La solicitud contiene campos no permitidos.' });
    }
    if (
      clienteId !== undefined &&
      (typeof clienteId !== 'string' || !clienteId.trim() || clienteId.length > 64)
    ) {
      return res.status(400).json({ error: 'El identificador del cliente no es válido.' });
    }
    // Reenvío desde la app móvil (modo offline): si ya existe, se devuelve la misma
    if (clienteId) {
      const existente = await Publicacion.findOne({ usuario: req.usuario._id, clienteId });
      if (existente) return res.status(200).json(existente);
    }
    const publicacion = await Publicacion.create({
      ...req.body,
      estado: 'pendiente',
      usuario: req.usuario._id,
    });
    return res.status(201).json(publicacion);
  } catch (error) {
    if (error.code === 11000 && clienteId) {
      const existente = await Publicacion.findOne({ usuario: req.usuario._id, clienteId });
      if (existente) return res.status(200).json(existente);
    }
    return next(error);
  }
}

async function updatePublicacion(req, res, next) {
  try {
    if (!mongoose.isObjectIdOrHexString(req.params.id)) {
      return res.status(400).json({ error: 'El identificador no es válido.' });
    }
    const allowedFields = isAdmin(req) ? [...editableFields, 'estado'] : editableFields;
    if (!hasOnlyAllowedFields(req.body, allowedFields) || Object.keys(req.body).length === 0) {
      return res.status(400).json({ error: 'La solicitud contiene campos vacíos o no permitidos.' });
    }
    if (
      Object.hasOwn(req.body, 'estado') &&
      !validStatuses.includes(req.body.estado)
    ) {
      return res.status(400).json({ error: 'El estado de publicación no es válido.' });
    }

    const publicacion = await Publicacion.findById(req.params.id);
    if (!publicacion) {
      return res.status(404).json({ error: 'Publicación no encontrada.' });
    }
    if (!isAdmin(req)) {
      if (publicacion.usuario.toString() !== req.usuario._id.toString()) {
        return res.status(403).json({ error: 'Solo puedes modificar tus publicaciones.' });
      }
      if (publicacion.estado !== 'pendiente') {
        return res.status(403).json({ error: 'Solo puedes editar publicaciones pendientes.' });
      }
    }

    if (Object.hasOwn(req.body, 'estado') && req.body.estado !== publicacion.estado) {
      const nuevo = req.body.estado;
      if (!transicionPermitida(publicacion.estado, nuevo)) {
        return res.status(409).json({
          error: `No se puede pasar una publicación de "${publicacion.estado}" a "${nuevo}".`,
        });
      }
      // Regla: solo se cierra como entregada si alguien la solicitó y se aceptó
      if (nuevo === 'entregada') {
        const aceptada = await Solicitud.exists({ publicacion: publicacion._id, estado: 'aceptada' });
        if (!aceptada) {
          return res.status(409).json({
            error: 'Para marcarla como entregada debe tener al menos una solicitud aceptada.',
          });
        }
      }
    }

    Object.assign(publicacion, req.body);
    await publicacion.save();
    return res.json(publicacion);
  } catch (error) {
    return next(error);
  }
}

async function deletePublicacion(req, res, next) {
  try {
    if (!mongoose.isObjectIdOrHexString(req.params.id)) {
      return res.status(400).json({ error: 'El identificador no es válido.' });
    }
    const publicacion = await Publicacion.findById(req.params.id);
    if (!publicacion) {
      return res.status(404).json({ error: 'Publicación no encontrada.' });
    }
    if (
      !isAdmin(req) &&
      (publicacion.usuario.toString() !== req.usuario._id.toString() ||
        publicacion.estado !== 'pendiente')
    ) {
      return res.status(403).json({
        error: 'Solo puedes eliminar tus publicaciones pendientes.',
      });
    }

    await Solicitud.deleteMany({ publicacion: publicacion._id });
    await Publicacion.deleteOne({ _id: publicacion._id });
    borrarImagen(publicacion.imagen);
    return res.status(204).end();
  } catch (error) {
    return next(error);
  }
}

/** POST /api/publicaciones/:id/imagen — Multer ya guardó el archivo en req.file */
async function subirImagenPublicacion(req, res, next) {
  try {
    if (!mongoose.isObjectIdOrHexString(req.params.id)) {
      borrarImagen(req.file.filename);
      return res.status(400).json({ error: 'El identificador no es válido.' });
    }
    const publicacion = await Publicacion.findById(req.params.id);
    if (!publicacion) {
      borrarImagen(req.file.filename);
      return res.status(404).json({ error: 'Publicación no encontrada.' });
    }
    if (!isAdmin(req)) {
      if (publicacion.usuario.toString() !== req.usuario._id.toString()) {
        borrarImagen(req.file.filename);
        return res.status(403).json({ error: 'Solo puedes cambiar la foto de tus publicaciones.' });
      }
      if (publicacion.estado !== 'pendiente') {
        borrarImagen(req.file.filename);
        return res.status(403).json({ error: 'Solo puedes cambiar la foto mientras está pendiente.' });
      }
    }
    const anterior = publicacion.imagen;
    publicacion.imagen = `/uploads/${req.file.filename}`;
    await publicacion.save();
    if (anterior) borrarImagen(anterior);   // se reemplaza la foto anterior
    return res.json(publicacion);
  } catch (error) {
    if (req.file) borrarImagen(req.file.filename);
    return next(error);
  }
}

/** DELETE /api/publicaciones/:id/imagen */
async function eliminarImagenPublicacion(req, res, next) {
  try {
    if (!mongoose.isObjectIdOrHexString(req.params.id)) {
      return res.status(400).json({ error: 'El identificador no es válido.' });
    }
    const publicacion = await Publicacion.findById(req.params.id);
    if (!publicacion) return res.status(404).json({ error: 'Publicación no encontrada.' });
    if (!isAdmin(req) && publicacion.usuario.toString() !== req.usuario._id.toString()) {
      return res.status(403).json({ error: 'Solo puedes cambiar la foto de tus publicaciones.' });
    }
    const anterior = publicacion.imagen;
    publicacion.imagen = null;
    await publicacion.save();
    borrarImagen(anterior);
    return res.json(publicacion);
  } catch (error) {
    return next(error);
  }
}

module.exports = {
  transicionPermitida,
  subirImagenPublicacion,
  eliminarImagenPublicacion,
  listPublicaciones,
  getPublicacion,
  createPublicacion,
  updatePublicacion,
  deletePublicacion,
};
