const mongoose = require('mongoose');
const Usuario  = require('../models/Usuario');
const Publicacion = require('../models/Publicacion');
const Solicitud = require('../models/Solicitud');
const bcrypt = require('bcryptjs');

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

function validarDatos({ nombre, email, password, rol }, creando) {
  if (nombre !== undefined && (typeof nombre !== 'string' || !nombre.trim() || nombre.trim().length > 100))
    return 'El nombre es obligatorio (máximo 100 caracteres).';
  if (email !== undefined && (typeof email !== 'string' || !EMAIL_REGEX.test(email.trim())))
    return 'El correo no es válido.';
  if (creando && (typeof password !== 'string' || password.length < 8))
    return 'La contraseña debe tener al menos 8 caracteres.';
  if (!creando && password !== undefined && password !== '' && (typeof password !== 'string' || password.length < 8))
    return 'La contraseña debe tener al menos 8 caracteres.';
  if (rol !== undefined && !['usuario', 'admin'].includes(rol))
    return 'El rol debe ser usuario o admin.';
  return null;
}

async function listUsuarios(_req, res, next) {
  try {
    const usuarios = await Usuario.find({}, '-password').sort({ createdAt: -1 });

    // cantidad de publicaciones por usuario
    const counts = await Publicacion.aggregate([
      { $group: { _id: '$usuario', total: { $sum: 1 } } },
    ]);
    const countMap = Object.fromEntries(counts.map(c => [c._id.toString(), c.total]));

    const result = usuarios.map(u => ({
      ...u.toObject(),
      publicaciones: countMap[u._id.toString()] || 0,
    }));

    return res.json(result);
  } catch (err) {
    return next(err);
  }
}

async function toggleActivo(req, res, next) {
  try {
    const { id } = req.params;
    if (!mongoose.isObjectIdOrHexString(id))
      return res.status(400).json({ error: 'ID no válido.' });

    const usuario = await Usuario.findById(id, '-password');
    if (!usuario) return res.status(404).json({ error: 'Usuario no encontrado.' });
    if (usuario.rol === 'admin')
      return res.status(403).json({ error: 'No puedes modificar un administrador.' });

    usuario.activo = !usuario.activo;
    await usuario.save();
    return res.json(usuario);
  } catch (err) {
    return next(err);
  }
}

async function getUsuario(req, res, next) {
  try {
    const { id } = req.params;
    if (!mongoose.isObjectIdOrHexString(id))
      return res.status(400).json({ error: 'ID no válido.' });
    const usuario = await Usuario.findById(id, '-password');
    if (!usuario) return res.status(404).json({ error: 'Usuario no encontrado.' });

    const [publicaciones, solicitudes] = await Promise.all([
      Publicacion.find({ usuario: id }).sort({ createdAt: -1 }),
      Solicitud.find({ usuario: id }).populate('publicacion', 'titulo estado').sort({ createdAt: -1 }),
    ]);
    return res.json({ ...usuario.toObject(), publicaciones, solicitudes });
  } catch (err) {
    return next(err);
  }
}

async function createUsuario(req, res, next) {
  try {
    const permitidos = ['nombre', 'email', 'password', 'rol'];
    if (Object.keys(req.body).some(k => !permitidos.includes(k)))
      return res.status(400).json({ error: 'La solicitud contiene campos no permitidos.' });
    const error = validarDatos(req.body, true);
    if (error) return res.status(400).json({ error });

    const email = req.body.email.trim().toLowerCase();
    if (await Usuario.exists({ email }))
      return res.status(409).json({ error: 'Ya existe una cuenta con ese correo.' });

    const usuario = await Usuario.create({
      nombre: req.body.nombre.trim(),
      email,
      password: await bcrypt.hash(req.body.password, 12),
      rol: req.body.rol || 'usuario',
    });
    const { password, ...sinPassword } = usuario.toObject();
    return res.status(201).json(sinPassword);
  } catch (err) {
    if (err.code === 11000) return res.status(409).json({ error: 'Ya existe una cuenta con ese correo.' });
    return next(err);
  }
}

async function updateUsuario(req, res, next) {
  try {
    const { id } = req.params;
    if (!mongoose.isObjectIdOrHexString(id))
      return res.status(400).json({ error: 'ID no válido.' });
    const permitidos = ['nombre', 'email', 'password', 'rol'];
    if (Object.keys(req.body).length === 0 || Object.keys(req.body).some(k => !permitidos.includes(k)))
      return res.status(400).json({ error: 'La solicitud contiene campos vacíos o no permitidos.' });
    const error = validarDatos(req.body, false);
    if (error) return res.status(400).json({ error });

    const usuario = await Usuario.findById(id);
    if (!usuario) return res.status(404).json({ error: 'Usuario no encontrado.' });
    // Regla: un admin no puede quitarse su propio rol de administrador
    if (req.body.rol && req.body.rol !== usuario.rol && usuario._id.equals(req.usuario._id))
      return res.status(403).json({ error: 'No puedes cambiar tu propio rol.' });

    if (req.body.email) {
      const email = req.body.email.trim().toLowerCase();
      if (email !== usuario.email && await Usuario.exists({ email }))
        return res.status(409).json({ error: 'Ya existe una cuenta con ese correo.' });
      usuario.email = email;
    }
    if (req.body.nombre) usuario.nombre = req.body.nombre.trim();
    if (req.body.rol) usuario.rol = req.body.rol;
    if (req.body.password) usuario.password = await bcrypt.hash(req.body.password, 12);
    await usuario.save();

    const { password, ...sinPassword } = usuario.toObject();
    return res.json(sinPassword);
  } catch (err) {
    if (err.code === 11000) return res.status(409).json({ error: 'Ya existe una cuenta con ese correo.' });
    return next(err);
  }
}

module.exports = { listUsuarios, getUsuario, createUsuario, updateUsuario, toggleActivo };
