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

// Un usuario solo puede tener una solicitud activa (pendiente o aceptada) por publicación.
// MongoDB lo garantiza aunque lleguen dos peticiones al mismo tiempo.
solicitudSchema.index(
  { publicacion: 1, usuario: 1 },
  { unique: true, partialFilterExpression: { estado: { $in: ['pendiente', 'aceptada'] } } }
);

module.exports = mongoose.models.Solicitud || mongoose.model('Solicitud', solicitudSchema);
