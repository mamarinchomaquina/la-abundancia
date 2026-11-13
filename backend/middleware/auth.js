const jwt = require('jsonwebtoken');
const Usuario = require('../models/Usuario');

function getJwtSecret() {
  const secret = process.env.JWT_SECRET;
  if (!secret) {
    throw new Error('Falta configurar JWT_SECRET en el archivo .env.');
  }
  if (secret.length < 32) {
    throw new Error('JWT_SECRET debe tener al menos 32 caracteres.');
  }
  return secret;
}

async function authenticate(req, res, next, optional) {
  const authorization = req.get('authorization');
  if (!authorization) {
    if (optional) return next();
    return res.status(401).json({ error: 'Se requiere autenticación.' });
  }

  const [scheme, token, extra] = authorization.split(' ');
  if (scheme !== 'Bearer' || !token || extra) {
    return res.status(401).json({ error: 'El encabezado de autenticación no es válido.' });
  }

  try {
    const payload = jwt.verify(token, getJwtSecret());
    if (typeof payload !== 'object' || typeof payload.sub !== 'string') {
      return res.status(401).json({ error: 'El token no es válido.' });
    }

    const usuario = await Usuario.findById(payload.sub).select('_id rol activo');
    if (!usuario) {
      return res.status(401).json({ error: 'La cuenta asociada al token no existe.' });
    }
    if (usuario.activo === false) {
      return res.status(401).json({ error: 'Tu cuenta está desactivada.' });
    }

    req.usuario = usuario;
    return next();
  } catch (error) {
    if (error.name === 'JsonWebTokenError' || error.name === 'TokenExpiredError') {
      return res.status(401).json({ error: 'El token no es válido o ha expirado.' });
    }
    return next(error);
  }
}

function requireAuth(req, res, next) {
  return authenticate(req, res, next, false);
}

function optionalAuth(req, res, next) {
  return authenticate(req, res, next, true);
}

function requireRole(...roles) {
  return (req, res, next) => {
    if (!req.usuario) {
      return res.status(401).json({ error: 'Se requiere autenticación.' });
    }
    if (!roles.includes(req.usuario.rol)) {
      return res.status(403).json({ error: 'No tienes permiso para realizar esta acción.' });
    }
    return next();
  };
}

module.exports = { getJwtSecret, requireAuth, optionalAuth, requireRole };
