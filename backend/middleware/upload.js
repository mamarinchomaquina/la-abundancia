const crypto = require('node:crypto');
const fs = require('node:fs');
const path = require('node:path');
const multer = require('multer');

const UPLOADS_DIR = path.join(__dirname, '..', 'uploads');
fs.mkdirSync(UPLOADS_DIR, { recursive: true });

// Formatos permitidos y tamaño máximo de la foto de una publicación
const TIPOS_PERMITIDOS = {
  'image/jpeg': '.jpg',
  'image/png': '.png',
  'image/webp': '.webp',
};
const TAMANO_MAXIMO = 2 * 1024 * 1024; // 2 MB

function tipoPermitido(mimetype) {
  return Object.hasOwn(TIPOS_PERMITIDOS, mimetype);
}

const storage = multer.diskStorage({
  destination: (_req, _file, cb) => cb(null, UPLOADS_DIR),
  // Nombre aleatorio: evita choques y que el usuario controle la ruta del archivo
  filename: (req, file, cb) => {
    const nombre = `${crypto.randomUUID()}${TIPOS_PERMITIDOS[file.mimetype]}`;
    req.archivoEnProceso = nombre; // para borrarlo si la subida falla a mitad
    cb(null, nombre);
  },
});

const subirImagen = multer({
  storage,
  limits: { fileSize: TAMANO_MAXIMO, files: 1 },
  fileFilter: (_req, file, cb) => {
    if (!tipoPermitido(file.mimetype)) {
      return cb(new multer.MulterError('LIMIT_UNEXPECTED_FILE', 'tipo'));
    }
    return cb(null, true);
  },
}).single('imagen');

/** Ejecuta Multer y convierte sus errores en respuestas 400 claras. */
function recibirImagen(req, res, next) {
  subirImagen(req, res, (error) => {
    if (!error) {
      if (!req.file) return res.status(400).json({ error: 'Adjunta una imagen en el campo "imagen".' });
      return next();
    }
    // Si se alcanzó a escribir parte del archivo, se elimina
    if (req.archivoEnProceso) borrarImagen(req.archivoEnProceso);
    if (error instanceof multer.MulterError) {
      if (error.code === 'LIMIT_FILE_SIZE')
        return res.status(400).json({ error: 'La imagen supera el tamaño máximo de 2 MB.' });
      if (error.field === 'tipo')
        return res.status(400).json({ error: 'Formato no permitido. Usa JPG, PNG o WEBP.' });
      return res.status(400).json({ error: 'Envía una sola imagen en el campo "imagen".' });
    }
    return next(error);
  });
}

/** Borra un archivo de /uploads a partir de la ruta guardada ("/uploads/xxx.jpg"). */
function borrarImagen(ruta) {
  if (!ruta) return;
  const archivo = path.join(UPLOADS_DIR, path.basename(ruta));
  fs.promises.unlink(archivo).catch(() => {});
}

module.exports = { recibirImagen, borrarImagen, tipoPermitido, UPLOADS_DIR, TAMANO_MAXIMO };
