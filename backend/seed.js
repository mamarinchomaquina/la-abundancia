const mongoose = require('mongoose');
const bcrypt   = require('bcryptjs');
require('dotenv').config();

mongoose.connect(process.env.MONGODB_URI).then(async () => {
  console.log('✓ Conectado a MongoDB');

  // ── Limpiar colecciones ───────────────────────────────────────
  await mongoose.connection.collection('usuarios').deleteMany({});
  await mongoose.connection.collection('publicacions').deleteMany({});
  console.log('✓ Colecciones limpiadas');

  // ── Usuarios ─────────────────────────────────────────────────
  const hash = (pw) => bcrypt.hash(pw, 12);

  const ahora  = new Date();
  const dias   = (n) => new Date(ahora - n * 86400000);

  const usuariosData = [
    { nombre: 'Admin',           email: 'admin@abundancia.com', password: await hash('admin123'),  rol: 'admin',   activo: true,  createdAt: dias(30) },
    { nombre: 'María García',    email: 'maria@gmail.com',      password: await hash('pass1234'),  rol: 'usuario', activo: true,  createdAt: dias(25) },
    { nombre: 'Juan López',      email: 'juan@gmail.com',       password: await hash('pass1234'),  rol: 'usuario', activo: false, createdAt: dias(20) },
    { nombre: 'Laura Martínez',  email: 'laura@email.com',      password: await hash('pass1234'),  rol: 'usuario', activo: true,  createdAt: dias(15) },
    { nombre: 'Pedro Sánchez',   email: 'pedro@correo.com',     password: await hash('pass1234'),  rol: 'usuario', activo: true,  createdAt: dias(5)  },
    { nombre: 'Sofía Ramírez',   email: 'sofia@gmail.com',      password: await hash('pass1234'),  rol: 'usuario', activo: true,  createdAt: dias(10) },
    { nombre: 'Andrés Torres',   email: 'andres@email.com',     password: await hash('pass1234'),  rol: 'usuario', activo: true,  createdAt: dias(8)  },
    { nombre: 'Camila Herrera',  email: 'camila@correo.com',    password: await hash('pass1234'),  rol: 'usuario', activo: false, createdAt: dias(12) },
  ];

  usuariosData.forEach(u => { u.updatedAt = u.createdAt; });
  const insUsuarios = await mongoose.connection.collection('usuarios').insertMany(usuariosData);
  const ids = insUsuarios.insertedIds;
  console.log(`✓ ${Object.keys(ids).length} usuarios creados`);

  // Mapear por nombre para referencias fáciles
  const U = {};
  usuariosData.forEach((u, i) => { U[u.nombre] = ids[i]; });

  // ── Publicaciones ─────────────────────────────────────────────
  const pubs = [
    // ── María García (12 pubs) ──
    { titulo: 'Arroz con leche casero',       descripcion: 'Porciones de arroz con leche preparado con canela y azúcar morena, ideal para compartir en familia. Rinde para 8 personas.', categoria: 'Alimentos', cantidad: 8,  ubicacion: 'Barrio Centro, Calle 5 #12-30', estado: 'aprobada',  usuario: U['María García'],   createdAt: dias(24) },
    { titulo: 'Pandebono recién horneado',    descripcion: 'Pandebonos de queso fresco hechos en casa esta mañana. Paquetes de 6 unidades, blanditos y esponjosos.', categoria: 'Alimentos', cantidad: 12, ubicacion: 'Barrio Centro, Calle 5 #12-30', estado: 'aprobada',  usuario: U['María García'],   createdAt: dias(22) },
    { titulo: 'Tamales de pipián',            descripcion: 'Tamales rellenos de pollo y pipián, envueltos en hoja de plátano. Preparados ayer, listos para calentar.', categoria: 'Alimentos', cantidad: 10, ubicacion: 'Barrio Centro, Calle 5 #12-30', estado: 'entregada', usuario: U['María García'],   createdAt: dias(20) },
    { titulo: 'Ropa interior niños 2–6 años', descripcion: 'Lote de ropa interior en buen estado para niños entre 2 y 6 años. Lavada y doblada, lista para entregar.', categoria: 'Ropa',      cantidad: 15, ubicacion: 'Barrio Centro, Calle 5 #12-30', estado: 'aprobada',  usuario: U['María García'],   createdAt: dias(18) },
    { titulo: 'Cuadernos escolares',          descripcion: 'Cuadernos de 50 hojas cuadriculados y de doble línea, casi sin usar. Perfectos para estudiantes de primaria.', categoria: 'Útiles',    cantidad: 20, ubicacion: 'Barrio Centro, Calle 5 #12-30', estado: 'aprobada',  usuario: U['María García'],   createdAt: dias(15) },
    { titulo: 'Sancocho de gallina',          descripcion: 'Ollas de sancocho de gallina criolla con papa, yuca y plátano. Suficiente para 6 porciones grandes.', categoria: 'Alimentos', cantidad: 6,  ubicacion: 'Barrio Centro, Calle 5 #12-30', estado: 'pendiente', usuario: U['María García'],   createdAt: dias(3)  },
    { titulo: 'Chaquetas invierno adulto',    descripcion: 'Chaquetas de paño en tallas S, M y L. En buen estado, lavadas. Ideales para temporada de frío.', categoria: 'Ropa',      cantidad: 5,  ubicacion: 'Barrio Centro, Calle 5 #12-30', estado: 'aprobada',  usuario: U['María García'],   createdAt: dias(12) },
    { titulo: 'Frijoles rojos cocinados',     descripcion: 'Frijoles rojos con guiso, carne y chicharrón. Listos para comer, acompañados de arroz blanco.', categoria: 'Alimentos', cantidad: 10, ubicacion: 'Barrio Centro, Calle 5 #12-30', estado: 'entregada', usuario: U['María García'],   createdAt: dias(10) },
    { titulo: 'Leche en polvo entera',        descripcion: 'Tarros de leche en polvo sin abrir, fecha vencimiento junio 2027. Donados por supermercado local.', categoria: 'Alimentos', cantidad: 4,  ubicacion: 'Barrio Centro, Calle 5 #12-30', estado: 'aprobada',  usuario: U['María García'],   createdAt: dias(8)  },
    { titulo: 'Diccionarios escolares',       descripcion: 'Diccionarios de español Larousse en buen estado. Útiles para estudiantes de bachillerato.', categoria: 'Útiles',    cantidad: 3,  ubicacion: 'Barrio Centro, Calle 5 #12-30', estado: 'aprobada',  usuario: U['María García'],   createdAt: dias(6)  },
    { titulo: 'Aguacates maduros',            descripcion: 'Aguacates Hass de finca propia, maduros y listos para comer. Bolsas de 5 unidades.', categoria: 'Alimentos', cantidad: 15, ubicacion: 'Barrio Centro, Calle 5 #12-30', estado: 'pendiente', usuario: U['María García'],   createdAt: dias(2)  },
    { titulo: 'Pantalones jean niña',         descripcion: 'Pantalones jean de niña tallas 6, 8 y 10 en excelente estado. Marca Arturo Calle, lavados.', categoria: 'Ropa',      cantidad: 7,  ubicacion: 'Barrio Centro, Calle 5 #12-30', estado: 'aprobada',  usuario: U['María García'],   createdAt: dias(1)  },

    // ── Juan López (7 pubs, suspendido) ──
    { titulo: 'Mazamorra con leche',          descripcion: 'Mazamorra de maíz pelado con leche fría y bocadillo. Ración familiar para 4 personas.', categoria: 'Alimentos', cantidad: 4,  ubicacion: 'Barrio Las Palmas, Carrera 8 #45-10', estado: 'aprobada',  usuario: U['Juan López'],    createdAt: dias(19) },
    { titulo: 'Ropa hombre adulto',           descripcion: 'Camisas, pantalones y zapatos de hombre talla M-L. Ropa en buen estado, limpia y doblada.', categoria: 'Ropa',      cantidad: 12, ubicacion: 'Barrio Las Palmas, Carrera 8 #45-10', estado: 'entregada', usuario: U['Juan López'],    createdAt: dias(17) },
    { titulo: 'Aceite de cocina',             descripcion: 'Botellas de aceite de girasol de 1 litro sin abrir. Donadas por cierre de tienda de barrio.', categoria: 'Alimentos', cantidad: 6,  ubicacion: 'Barrio Las Palmas, Carrera 8 #45-10', estado: 'aprobada',  usuario: U['Juan López'],    createdAt: dias(14) },
    { titulo: 'Platos y vasos desechables',   descripcion: 'Paquetes de platos y vasos desechables para eventos. Nunca usados, en su empaque original.', categoria: 'Útiles',    cantidad: 50, ubicacion: 'Barrio Las Palmas, Carrera 8 #45-10', estado: 'entregada', usuario: U['Juan López'],    createdAt: dias(13) },
    { titulo: 'Pasta y salsa de tomate',      descripcion: 'Espaguetis y salsa de tomate enlatada. Fecha vigente. Paquetes familiares de 500g.', categoria: 'Alimentos', cantidad: 8,  ubicacion: 'Barrio Las Palmas, Carrera 8 #45-10', estado: 'aprobada',  usuario: U['Juan López'],    createdAt: dias(11) },
    { titulo: 'Zapatos deportivos niño',      descripcion: 'Zapatos deportivos niño tallas 32 y 34. Poco uso, en buen estado. Marca genérica.', categoria: 'Ropa',      cantidad: 2,  ubicacion: 'Barrio Las Palmas, Carrera 8 #45-10', estado: 'aprobada',  usuario: U['Juan López'],    createdAt: dias(9)  },
    { titulo: 'Colores y pinturas',           descripcion: 'Cajas de colores Norma de 24 tonos y témperas escolares. Para niños de primaria.', categoria: 'Útiles',    cantidad: 10, ubicacion: 'Barrio Las Palmas, Carrera 8 #45-10', estado: 'pendiente', usuario: U['Juan López'],    createdAt: dias(1)  },

    // ── Laura Martínez (23 pubs) ──
    { titulo: 'Arepas de chócolo',            descripcion: 'Arepas de chócolo con queso rallado, recién hechas. Paquetes de 4 unidades para llevar.', categoria: 'Alimentos', cantidad: 20, ubicacion: 'Urbanización Villa Rosa, Apt 304',     estado: 'aprobada',  usuario: U['Laura Martínez'], createdAt: dias(14) },
    { titulo: 'Empanadas de pipián',          descripcion: 'Empanadas de pipián con papa y huevo. Horneadas, sin freír. Lote de 15 unidades.', categoria: 'Alimentos', cantidad: 15, ubicacion: 'Urbanización Villa Rosa, Apt 304',     estado: 'aprobada',  usuario: U['Laura Martínez'], createdAt: dias(13) },
    { titulo: 'Ropa bebé 0-12 meses',         descripcion: 'Lote de ropa de bebé en excelente estado: bodies, mamelucossand pijamas tallas 0-12 meses.', categoria: 'Ropa',      cantidad: 25, ubicacion: 'Urbanización Villa Rosa, Apt 304',     estado: 'entregada', usuario: U['Laura Martínez'], createdAt: dias(12) },
    { titulo: 'Leche materna pasteurizada',   descripcion: 'Bolsas de leche materna pasteurizada para bebés. Contactar antes de retirar por protocolo.', categoria: 'Alimentos', cantidad: 10, ubicacion: 'Urbanización Villa Rosa, Apt 304',     estado: 'aprobada',  usuario: U['Laura Martínez'], createdAt: dias(11) },
    { titulo: 'Juguetes didácticos',          descripcion: 'Juguetes didácticos para niños de 1 a 5 años. Rompecabezas, bloques y anillos de colores.', categoria: 'Útiles',    cantidad: 8,  ubicacion: 'Urbanización Villa Rosa, Apt 304',     estado: 'aprobada',  usuario: U['Laura Martínez'], createdAt: dias(10) },
    { titulo: 'Cocada con panela',            descripcion: 'Cocadas de coco rallado con panela, sin conservantes. Bolsas de 6 unidades.', categoria: 'Alimentos', cantidad: 18, ubicacion: 'Urbanización Villa Rosa, Apt 304',     estado: 'aprobada',  usuario: U['Laura Martínez'], createdAt: dias(9)  },
    { titulo: 'Ropa colegial niña',           descripcion: 'Uniformes escolares de niña tallas 8-12: blusa blanca, falda cuadros y medias.', categoria: 'Ropa',      cantidad: 6,  ubicacion: 'Urbanización Villa Rosa, Apt 304',     estado: 'entregada', usuario: U['Laura Martínez'], createdAt: dias(8)  },
    { titulo: 'Caldo de costilla',            descripcion: 'Caldo de costilla de res con papa, cilantro y chocolate. Porciones individuales en recipiente.', categoria: 'Alimentos', cantidad: 10, ubicacion: 'Urbanización Villa Rosa, Apt 304',     estado: 'aprobada',  usuario: U['Laura Martínez'], createdAt: dias(7)  },
    { titulo: 'Morral escolar usado',         descripcion: 'Morrales escolares en buen estado, lavados. Tallas para primaria y bachillerato.', categoria: 'Útiles',    cantidad: 5,  ubicacion: 'Urbanización Villa Rosa, Apt 304',     estado: 'aprobada',  usuario: U['Laura Martínez'], createdAt: dias(6)  },
    { titulo: 'Plátano maduro asado',         descripcion: 'Plátanos maduros asados al carbón con queso. Listos para consumir, paquetes de 2 unidades.', categoria: 'Alimentos', cantidad: 12, ubicacion: 'Urbanización Villa Rosa, Apt 304',     estado: 'entregada', usuario: U['Laura Martínez'], createdAt: dias(5)  },
    { titulo: 'Medias y ropa interior mujer', descripcion: 'Medias y ropa interior de mujer tallas S y M, nuevas en su empaque. Variedad de colores.', categoria: 'Ropa',      cantidad: 20, ubicacion: 'Urbanización Villa Rosa, Apt 304',     estado: 'aprobada',  usuario: U['Laura Martínez'], createdAt: dias(4)  },
    { titulo: 'Papas criolla con guiso',      descripcion: 'Papas criollas cocidas con guiso de tomate y cebolla. Porción familiar lista para servir.', categoria: 'Alimentos', cantidad: 8,  ubicacion: 'Urbanización Villa Rosa, Apt 304',     estado: 'pendiente', usuario: U['Laura Martínez'], createdAt: dias(3)  },
    { titulo: 'Calculadora científica',       descripcion: 'Calculadora científica Casio FX-82 en buen estado. Para estudiantes de bachillerato.', categoria: 'Útiles',    cantidad: 1,  ubicacion: 'Urbanización Villa Rosa, Apt 304',     estado: 'aprobada',  usuario: U['Laura Martínez'], createdAt: dias(3)  },
    { titulo: 'Chocoramo y pan bimbo',        descripcion: 'Paquetes de chocoramo y pan tajado bimbo próximos a vencer pero en perfecto estado.', categoria: 'Alimentos', cantidad: 15, ubicacion: 'Urbanización Villa Rosa, Apt 304',     estado: 'aprobada',  usuario: U['Laura Martínez'], createdAt: dias(2)  },
    { titulo: 'Ropa colegial niño',           descripcion: 'Uniforme de niño talla 10-12: pantalón gris, camisa blanca y suéter azul marino.', categoria: 'Ropa',      cantidad: 4,  ubicacion: 'Urbanización Villa Rosa, Apt 304',     estado: 'entregada', usuario: U['Laura Martínez'], createdAt: dias(2)  },
    { titulo: 'Atún y sardinas en lata',      descripcion: 'Latas de atún en agua y sardinas en aceite. Fecha vigente, sin abrir.', categoria: 'Alimentos', cantidad: 20, ubicacion: 'Urbanización Villa Rosa, Apt 304',     estado: 'aprobada',  usuario: U['Laura Martínez'], createdAt: dias(1)  },
    { titulo: 'Loncheras escolares',          descripcion: 'Loncheras plásticas con tapa hermética para niños. Lavadas y desinfectadas.', categoria: 'Útiles',    cantidad: 7,  ubicacion: 'Urbanización Villa Rosa, Apt 304',     estado: 'pendiente', usuario: U['Laura Martínez'], createdAt: dias(1)  },
    { titulo: 'Buñuelos de queso',            descripcion: 'Buñuelos de queso esponjosos, recién fritos. Bolsas de 5 unidades, calientes.', categoria: 'Alimentos', cantidad: 10, ubicacion: 'Urbanización Villa Rosa, Apt 304',     estado: 'pendiente', usuario: U['Laura Martínez'], createdAt: dias(0)  },
    { titulo: 'Pantalones de sudadera',       descripcion: 'Pantalones de sudadera para adulto tallas M, L y XL. En buen estado, lavados.', categoria: 'Ropa',      cantidad: 9,  ubicacion: 'Urbanización Villa Rosa, Apt 304',     estado: 'aprobada',  usuario: U['Laura Martínez'], createdAt: dias(0)  },
    { titulo: 'Aceite de oliva importado',    descripcion: 'Botellas de aceite de oliva extra virgen importado. Sin abrir. Donación de empresa local.', categoria: 'Alimentos', cantidad: 3,  ubicacion: 'Urbanización Villa Rosa, Apt 304',     estado: 'aprobada',  usuario: U['Laura Martínez'], createdAt: dias(0)  },
    { titulo: 'Botas de caucho niño',         descripcion: 'Botas de caucho para niño tallas 28 y 30. Perfectas para épocas de lluvia.', categoria: 'Ropa',      cantidad: 2,  ubicacion: 'Urbanización Villa Rosa, Apt 304',     estado: 'aprobada',  usuario: U['Laura Martínez'], createdAt: dias(0)  },
    { titulo: 'Tijeras y reglas escolares',   descripcion: 'Kit escolar con tijeras punta redonda, reglas de 30cm y transportadores. Para primaria.', categoria: 'Útiles',    cantidad: 12, ubicacion: 'Urbanización Villa Rosa, Apt 304',     estado: 'pendiente', usuario: U['Laura Martínez'], createdAt: dias(0)  },
    { titulo: 'Leche entera pasteurizada',    descripcion: 'Bolsas de leche entera pasteurizada de 1 litro. Vencimiento en 4 días, perfecta.', categoria: 'Alimentos', cantidad: 10, ubicacion: 'Urbanización Villa Rosa, Apt 304',     estado: 'pendiente', usuario: U['Laura Martínez'], createdAt: dias(0)  },

    // ── Pedro Sánchez (5 pubs) ──
    { titulo: 'Changua bogotana',             descripcion: 'Changua santafereña con huevo pochado, calostro y cilantro. Porciones listas para llevar.', categoria: 'Alimentos', cantidad: 6,  ubicacion: 'Conjunto Residencial Los Pinos, Casa 7', estado: 'aprobada',  usuario: U['Pedro Sánchez'],  createdAt: dias(4)  },
    { titulo: 'Ropa de cama doble',           descripcion: 'Juego de sábanas para cama doble, lavadas. Incluye sábana plana, ajustable y 2 fundas.', categoria: 'Ropa',      cantidad: 2,  ubicacion: 'Conjunto Residencial Los Pinos, Casa 7', estado: 'aprobada',  usuario: U['Pedro Sánchez'],  createdAt: dias(3)  },
    { titulo: 'Avena con frutas',             descripcion: 'Avena en agua con banano, maracuyá y miel de abeja. Preparada esta mañana, fría.', categoria: 'Alimentos', cantidad: 8,  ubicacion: 'Conjunto Residencial Los Pinos, Casa 7', estado: 'entregada', usuario: U['Pedro Sánchez'],  createdAt: dias(2)  },
    { titulo: 'Materiales de dibujo',         descripcion: 'Lápices de dibujo 2B, 4B y 6B, borrador de nata y sacapuntas. Para estudiantes de arte.', categoria: 'Útiles',    cantidad: 5,  ubicacion: 'Conjunto Residencial Los Pinos, Casa 7', estado: 'aprobada',  usuario: U['Pedro Sánchez'],  createdAt: dias(1)  },
    { titulo: 'Almojábanas con mantequilla',  descripcion: 'Almojábanas esponjosas recién horneadas con mantequilla de maní casera. Bolsas de 4.', categoria: 'Alimentos', cantidad: 12, ubicacion: 'Conjunto Residencial Los Pinos, Casa 7', estado: 'pendiente', usuario: U['Pedro Sánchez'],  createdAt: dias(0)  },

    // ── Sofía Ramírez ──
    { titulo: 'Obleas con arequipe',          descripcion: 'Obleas con arequipe, queso y mermelada de mora. Paquetes de 3 obleas listas para comer.', categoria: 'Alimentos', cantidad: 15, ubicacion: 'Barrio El Refugio, Torre 2 Apt 501',   estado: 'aprobada',  usuario: U['Sofía Ramírez'],  createdAt: dias(9)  },
    { titulo: 'Ropa de maternidad',           descripcion: 'Blusas y pantalones de maternidad talla M y L en buen estado. Poco uso, lavadas.', categoria: 'Ropa',      cantidad: 8,  ubicacion: 'Barrio El Refugio, Torre 2 Apt 501',   estado: 'entregada', usuario: U['Sofía Ramírez'],  createdAt: dias(7)  },
    { titulo: 'Sal, azúcar y panela',         descripcion: 'Paquetes de sal de cocina, azúcar blanca y panela de 500g. Productos básicos sin abrir.', categoria: 'Alimentos', cantidad: 10, ubicacion: 'Barrio El Refugio, Torre 2 Apt 501',   estado: 'aprobada',  usuario: U['Sofía Ramírez'],  createdAt: dias(5)  },
    { titulo: 'Libros de texto bachillerato', descripcion: 'Libros de matemáticas, español e inglés para grados 9 y 10. Usados pero completos.', categoria: 'Útiles',    cantidad: 6,  ubicacion: 'Barrio El Refugio, Torre 2 Apt 501',   estado: 'aprobada',  usuario: U['Sofía Ramírez'],  createdAt: dias(3)  },
    { titulo: 'Sudaderas adulto mayor',       descripcion: 'Sudaderas cómodas para adultos mayores, talla única. Suaves, abrigadas y fáciles de poner.', categoria: 'Ropa',      cantidad: 4,  ubicacion: 'Barrio El Refugio, Torre 2 Apt 501',   estado: 'pendiente', usuario: U['Sofía Ramírez'],  createdAt: dias(1)  },

    // ── Andrés Torres ──
    { titulo: 'Mondongo santafereño',         descripcion: 'Mondongo con papa, zanahoria y maíz. Preparado con receta casera, porciones de 500ml.', categoria: 'Alimentos', cantidad: 5,  ubicacion: 'Vereda El Porvenir, Finca La Esperanza', estado: 'aprobada',  usuario: U['Andrés Torres'],  createdAt: dias(7)  },
    { titulo: 'Botas de caucho adulto',       descripcion: 'Botas de caucho para trabajo de campo tallas 40 y 42. En buen estado, lavadas.', categoria: 'Ropa',      cantidad: 3,  ubicacion: 'Vereda El Porvenir, Finca La Esperanza', estado: 'aprobada',  usuario: U['Andrés Torres'],  createdAt: dias(5)  },
    { titulo: 'Miel de abejas pura',          descripcion: 'Frascos de miel de abejas cosechada en finca propia. 100% natural, sin procesar.', categoria: 'Alimentos', cantidad: 8,  ubicacion: 'Vereda El Porvenir, Finca La Esperanza', estado: 'entregada', usuario: U['Andrés Torres'],  createdAt: dias(4)  },
    { titulo: 'Herramientas de jardín',       descripcion: 'Palas, rastrillos y palitas de jardín en buen estado. Útiles para huertas escolares.', categoria: 'Útiles',    cantidad: 4,  ubicacion: 'Vereda El Porvenir, Finca La Esperanza', estado: 'aprobada',  usuario: U['Andrés Torres'],  createdAt: dias(2)  },
    { titulo: 'Tomates y pimentones frescos', descripcion: 'Canastilla de tomates chonto y pimentones rojos de finca. Recién cosechados esta semana.', categoria: 'Alimentos', cantidad: 10, ubicacion: 'Vereda El Porvenir, Finca La Esperanza', estado: 'pendiente', usuario: U['Andrés Torres'],  createdAt: dias(0)  },

    // ── Camila Herrera (suspendida) ──
    { titulo: 'Papas fritas artesanales',     descripcion: 'Papas criollas fritas en aceite de girasol con sal marina. Bolsas de 100g.', categoria: 'Alimentos', cantidad: 20, ubicacion: 'Barrio Nuevo Horizonte, Cra 15 #22-18',  estado: 'aprobada',  usuario: U['Camila Herrera'], createdAt: dias(11) },
    { titulo: 'Uniformes trabajo social',     descripcion: 'Camisas institucionales de trabajo social tallas S y M. Usadas pero en buen estado.', categoria: 'Ropa',      cantidad: 5,  ubicacion: 'Barrio Nuevo Horizonte, Cra 15 #22-18',  estado: 'entregada', usuario: U['Camila Herrera'], createdAt: dias(10) },
    { titulo: 'Cocoa y café instantáneo',     descripcion: 'Tarros de cocoa en polvo y café instantáneo. Sin abrir, fecha vigente.', categoria: 'Alimentos', cantidad: 6,  ubicacion: 'Barrio Nuevo Horizonte, Cra 15 #22-18',  estado: 'aprobada',  usuario: U['Camila Herrera'], createdAt: dias(8)  },
  ];

  // Agregar timestamps a publicaciones
  pubs.forEach(p => { p.updatedAt = p.createdAt; p.fecha = p.createdAt; });

  const insPubs = await mongoose.connection.collection('publicacions').insertMany(pubs);
  console.log(`✓ ${Object.keys(insPubs.insertedIds).length} publicaciones creadas`);

  // ── Resumen ───────────────────────────────────────────────────
  const conteos = await mongoose.connection.collection('publicacions').aggregate([
    { $group: { _id: '$estado', total: { $sum: 1 } } }
  ]).toArray();
  console.log('\n── Resumen publicaciones ──');
  conteos.forEach(c => console.log(`  ${c._id}: ${c.total}`));

  const usuariosResumen = await mongoose.connection.collection('usuarios').find({}, { projection: { nombre: 1, email: 1, rol: 1, activo: 1 }}).toArray();
  console.log('\n── Usuarios creados ──');
  usuariosResumen.forEach(u => console.log(`  [${u.rol}] ${u.nombre} <${u.email}> activo=${u.activo}`));
  console.log('\n✓ Seed completo');
  mongoose.disconnect();
}).catch(err => { console.error('Error:', err.message); process.exit(1); });
