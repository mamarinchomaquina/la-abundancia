const dgram = require('node:dgram');
const os = require('node:os');

/**
 * Anuncia el backend en la red local para que la app móvil lo encuentre sola.
 * Cada 2 segundos envía un mensaje UDP de difusión (broadcast) al puerto 41234:
 *   {"servicio":"la-abundancia","puerto":3000}
 * La app lo escucha y toma la IP de quien lo envió.
 */
const PUERTO_ANUNCIO = 41234;

function direccionesDeDifusion() {
  const direcciones = new Set(['255.255.255.255']);
  for (const lista of Object.values(os.networkInterfaces())) {
    for (const red of lista || []) {
      if (red.family !== 'IPv4' || red.internal || !red.netmask) continue;
      const ip = red.address.split('.').map(Number);
      const mascara = red.netmask.split('.').map(Number);
      direcciones.add(ip.map((parte, i) => (parte | (~mascara[i] & 255))).join('.'));
    }
  }
  return [...direcciones];
}

function anunciarServidor(puertoHttp) {
  const socket = dgram.createSocket('udp4');
  socket.on('error', (error) => {
    console.warn('Anuncio en la red desactivado:', error.message);
    socket.close();
  });
  socket.bind(() => {
    socket.setBroadcast(true);
    const mensaje = Buffer.from(JSON.stringify({ servicio: 'la-abundancia', puerto: Number(puertoHttp) }));
    const enviar = () => {
      for (const destino of direccionesDeDifusion()) {
        socket.send(mensaje, PUERTO_ANUNCIO, destino, () => {});
      }
    };
    enviar();
    setInterval(enviar, 2000).unref();
    console.log(`Anunciando el servidor en la red local (UDP ${PUERTO_ANUNCIO}) para la app móvil.`);
  });
}

module.exports = { anunciarServidor, direccionesDeDifusion, PUERTO_ANUNCIO };
