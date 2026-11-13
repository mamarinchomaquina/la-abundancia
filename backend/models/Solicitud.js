const mongoose = require('mongoose');

const solicitudSchema = new mongoose.Schema(
  {
    publicacion: {
      type: mongoose.Schema.Types.ObjectId,
      ref: 'Publicacion',
      required: true,
      immutable: true,
    },
    usuario: {
      type: mongoose.Schema.Types.ObjectId,
      ref: 'Usuario',
      required: true,
      immutable: true,
    },
    mensaje: { type: String, trim: true, maxlength: 1000, default: '' },
    estado: {
      type: String,
      enum: ['pendiente', 'aceptada', 'rechazada'],
      default: 'pendiente',
    },
  },
  { timestamps: true }
);

module.exports = mongoose.models.Solicitud || mongoose.model('Solicitud', solicitudSchema);
