const express = require('express');
const {
  listPublicaciones,
  getPublicacion,
  createPublicacion,
  updatePublicacion,
  deletePublicacion,
  subirImagenPublicacion,
  eliminarImagenPublicacion,
} = require('../controllers/publicacionController');
const { recibirImagen } = require('../middleware/upload');
const { requireAuth } = require('../middleware/auth');

const router = express.Router();

router.get('/', listPublicaciones);
router.get('/:id', getPublicacion);
router.post('/', requireAuth, createPublicacion);
router.put('/:id', requireAuth, updatePublicacion);
router.delete('/:id', requireAuth, deletePublicacion);
// Foto de la publicación (multipart/form-data, campo "imagen")
router.post('/:id/imagen', requireAuth, recibirImagen, subirImagenPublicacion);
router.delete('/:id/imagen', requireAuth, eliminarImagenPublicacion);

module.exports = router;
