const express = require('express');
const {
  listSolicitudes,
  createSolicitud,
  updateEstadoSolicitud,
} = require('../controllers/solicitudController');

const router = express.Router();

router.get('/', listSolicitudes);
router.post('/', createSolicitud);
router.patch('/:id/estado', updateEstadoSolicitud);

module.exports = router;
