const express = require('express');
const {
  listUsuarios, getUsuario, createUsuario, updateUsuario, toggleActivo,
} = require('../controllers/usuariosController');
const router = express.Router();

router.get('/', listUsuarios);
router.post('/', createUsuario);
router.get('/:id', getUsuario);
router.put('/:id', updateUsuario);
router.patch('/:id/activo', toggleActivo);

module.exports = router;
