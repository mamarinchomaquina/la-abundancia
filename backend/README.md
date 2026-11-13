# API de La Abundancia

## Configuración

1. Copia `.env.example` a `.env`.
2. Completa `MONGODB_URI` con la URI de MongoDB local (`mongodb://localhost:27017/abundancia`).
3. Genera un secreto para JWT con `node -e "console.log(require('node:crypto').randomBytes(48).toString('hex'))"` y guárdalo en `JWT_SECRET`.
4. Ejecuta `npm start`. La API queda disponible en `http://localhost:3000`.

No compartas ni subas `.env` al repositorio.

## Autenticación

- `POST /api/auth/registro` — `{ "nombre": "...", "email": "...", "password": "..." }`. El registro siempre crea cuentas `usuario`.
- `POST /api/auth/login` — `{ "email": "...", "password": "..." }`. Devuelve un JWT válido por 8 horas.
- Para rutas autenticadas, envía `Authorization: Bearer <token>`.

Para crear el primer admin, registra una cuenta normal y cambia su rol desde mongosh (o usa `node crear-admin.js`):

```javascript
db.usuarios.updateOne(
  { email: "admin@ejemplo.com" },
  { $set: { rol: "admin" } }
)
```

## Publicaciones

- `GET /api/publicaciones` y `GET /api/publicaciones/:id` — sin token muestran publicaciones aprobadas; con token incluyen también las propias. El admin puede ver todas.
- `POST /api/publicaciones` — usuario autenticado; requiere `titulo`, `descripcion`, `categoria` (`Alimentos`, `Ropa`, `Útiles` o `Otros`), `cantidad` entera mayor a cero y `ubicacion`. El servidor asigna propietario y estado inicial `pendiente`.
- `PUT /api/publicaciones/:id` y `DELETE /api/publicaciones/:id` — el propietario puede editar/eliminar las propias mientras estén pendientes. El admin puede gestionar cualquiera, incluido cambiar su estado a `pendiente`, `aprobada` o `entregada`.

## Solicitudes y estadísticas

- `GET /api/solicitudes` — devuelve las propias, las recibidas en publicaciones propias o todas para admin.
- `POST /api/solicitudes` — usuario autenticado; `{ "publicacion": "<id>", "mensaje": "..." }`. Solo permite solicitar publicaciones aprobadas de otra persona y no tener ya una solicitud pendiente/aceptada para ella.
- `PATCH /api/solicitudes/:id/estado` — propietario de la publicación o admin; `{ "estado": "aceptada" }` o `{ "estado": "rechazada" }`.
- `GET /api/estadisticas` — solo admin; totales de usuarios, publicaciones por estado y solicitudes por estado.

`GET /health` informa si el proceso y MongoDB están disponibles.


> El README general del proyecto (instalación completa, panel, app y pruebas) está en la carpeta raíz. El contrato completo de la API está en `documentacion/Documento_tecnico_La_Abundancia.docx`.
