const mongoose = require('mongoose');

const publicacionSchema = new mongoose.Schema(
  {
    titulo: { type: String, required: true, trim: true, maxlength: 150 },
    descripcion: { type: String, required: true, trim: true, maxlength: 3000 },
    categoria: {
      type: String,
      required: true,
      enum: ['Alimentos', 'Ropa', 'Útiles', 'Otros'],
    },
    cantidad: { type: Number, required: true, min: 1, validate: Number.isInteger },
    ubicacion: { type: String, required: true, trim: true, maxlength: 200 },
    estado: {
      type: String,
      enum: ['pendiente', 'aprobada', 'rechazada', 'entregada'],
      default: 'pendiente',
    },
    fecha: { type: Date, default: Date.now, immutable: true },
    usuario: {
      type: mongoose.Schema.Types.ObjectId,
      ref: 'Usuario',
      required: true,
      immutable: true,
    },
    // Ruta pública de la foto subida con Multer, p. ej. "/uploads/3f2c....jpg"
    imagen: { type: String, default: null },
    // Identificador generado por la app móvil: evita duplicados si reintenta un envío offline
    clienteId: { type: String, trim: true, maxlength: 64, immutable: true },
  },
  { timestamps: true }
);

publicacionSchema.index(
  { usuario: 1, clienteId: 1 },
  { unique: true, partialFilterExpression: { clienteId: { $type: 'string' } } }
);

module.exports = mongoose.models.Publicacion || mongoose.model('Publicacion', publicacionSchema);
