const mongoose = require('mongoose');
const bcrypt = require('bcryptjs');
require('dotenv').config();

mongoose.connect(process.env.MONGODB_URI).then(async () => {
  // Borrar cualquier usuario admin existente
  const result = await mongoose.connection.collection('usuarios').deleteMany({});
  console.log('Usuarios borrados:', result.deletedCount);

  const hash = await bcrypt.hash('admin123', 12);
  await mongoose.connection.collection('usuarios').insertOne({
    nombre: 'Admin',
    email: 'admin@abundancia.com',
    password: hash,
    rol: 'admin',
    createdAt: new Date(),
    updatedAt: new Date()
  });

  const user = await mongoose.connection.collection('usuarios').findOne({ email: 'admin@abundancia.com' });
  console.log('Usuario creado:', { email: user.email, rol: user.rol, tienePassword: !!user.password });

  mongoose.disconnect();
});
