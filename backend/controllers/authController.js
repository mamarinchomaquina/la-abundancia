const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const Usuario = require('../models/Usuario');
const { getJwtSecret } = require('../middleware/auth');

function publicUser(usuario) {
  return {
    id: usuario._id,
    nombre: usuario.nombre,
    email: usuario.email,
    rol: usuario.rol,
  };
}

async function register(req, res, next) {
  try {
    const { nombre, email, password } = req.body;
    if (
      typeof nombre !== 'string' ||
      !nombre.trim() ||
      typeof email !== 'string' ||
      !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email) ||
      typeof password !== 'string' ||
      password.length < 8
    ) {
      return res.status(400).json({
        error: 'Indica nombre, un email válido y una contraseña de al menos 8 caracteres.',
      });
    }

    const normalizedEmail = email.trim().toLowerCase();
    if (await Usuario.exists({ email: normalizedEmail })) {
      return res.status(409).json({ error: 'Ya existe una cuenta con ese email.' });
    }

    const usuario = await Usuario.create({
      nombre: nombre.trim(),
      email: normalizedEmail,
      password: await bcrypt.hash(password, 12),
    });

    return res.status(201).json({ usuario: publicUser(usuario) });
  } catch (error) {
    if (error.code === 11000) {
      return res.status(409).json({ error: 'Ya existe una cuenta con ese email.' });
    }
    return next(error);
  }
}

async function login(req, res, next) {
  try {
    const { email, password } = req.body;
    if (typeof email !== 'string' || typeof password !== 'string') {
      return res.status(400).json({ error: 'Email y contraseña son obligatorios.' });
    }

    const usuario = await Usuario.findOne({ email: email.trim().toLowerCase() }).select('+password');
    if (!usuario || !(await bcrypt.compare(password, usuario.password))) {
      return res.status(401).json({ error: 'Email o contraseña incorrectos.' });
    }
    if (usuario.activo === false) {
      return res.status(403).json({ error: 'Tu cuenta está desactivada. Contacta al administrador.' });
    }

    const token = jwt.sign({}, getJwtSecret(), {
      subject: usuario._id.toString(),
      expiresIn: '8h',
    });
    return res.json({ token, tipo: 'Bearer', usuario: publicUser(usuario) });
  } catch (error) {
    return next(error);
  }
}

module.exports = { register, login };
