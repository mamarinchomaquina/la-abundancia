# La Abundancia — Sistema de gestión de donaciones

Proyecto Integrador · Móvil I y Desarrollo Web 2 · Integrante: Mateo

Sistema para gestionar donaciones (alimentos, ropa, útiles y otros) desde que se publican hasta que se entregan. Tiene tres partes que comparten la misma base de datos:

| Parte | Tecnología | Quién la usa |
|---|---|---|
| **App Android** (`/android`) | Kotlin, Jetpack Compose, MVVM, Room, DataStore, WorkManager, Retrofit | Donantes y personas que solicitan donaciones |
| **Panel web** (`/web-angular`) | Angular 19 | Administrador del negocio |
| **API REST** (`/backend`) | Node.js, Express, Mongoose, Multer, JWT | Las dos interfaces (única que accede a MongoDB) |

La documentación completa está en `/documentacion`: documento técnico (historias, reglas, arquitectura, modelo de datos, contrato de la API y matriz de pruebas), requerimientos, manual de usuario y colección de Postman.

---

## 1. Estructura

```
abundancia/
├── backend/          API REST (Express + MongoDB)
│   ├── controllers/  Lógica y reglas de negocio
│   ├── middleware/   Autenticación JWT, roles y carga de imágenes (Multer)
│   ├── models/       Usuario, Publicacion, Solicitud (Mongoose)
│   ├── routes/       Rutas de la API
│   ├── test/         Pruebas automáticas (node --test)
│   ├── uploads/      Fotos subidas (se crea sola)
│   ├── .env.example  Plantilla de configuración
│   └── index.js      Punto de entrada
├── web-angular/      Panel de administración (Angular)
├── android/          App móvil (abrir con Android Studio)
├── documentacion/    Documentos, colección de Postman y evidencias
└── iniciar.bat       Arranca el backend y el panel con doble clic
```

## 2. Versiones utilizadas

| Herramienta | Versión |
|---|---|
| Node.js | 22 (LTS) |
| MongoDB Community Server | 8.0 |
| Express / Mongoose / Multer | 5.3 / 9.11 / 2.5 |
| Angular / TypeScript | 19.2 / 5.7 |
| Android Studio | Versión reciente con JDK 17+ y Android SDK 35 |
| Kotlin / Android Gradle Plugin | 2.1.0 / 8.7.3 |
| App Android | minSdk 26 (Android 8.0) · targetSdk 35 |

## 3. Requisitos

- PC con Windows 10 u 11.
- Node.js 22 y MongoDB 8.0 instalados.
- Android Studio (para compilar la app) y un celular Android 8.0 o superior.
- El PC y el celular en la **misma red Wi-Fi**.

## 4. Instalación (solo la primera vez)

### 4.1 Backend
```bash
cd backend
npm install
copy .env.example .env
```
Edita `backend/.env`:
```
PORT=3000
MONGODB_URI=mongodb://localhost:27017/abundancia
JWT_SECRET=<texto aleatorio de mínimo 32 caracteres>
```
Para generar un secreto: `node -e "console.log(require('crypto').randomBytes(48).toString('hex'))"`

Para cargar datos de demostración (borra usuarios y publicaciones existentes): `node seed.js`
Usuario administrador de demostración: `admin@abundancia.com` / `admin123`.

### 4.2 Panel web
```bash
cd web-angular
npm install
```

### 4.3 Firewall de Windows
Para que el celular llegue al backend, en una terminal **como administrador**:
```
netsh advfirewall firewall add rule name="Abundancia backend" dir=in action=allow protocol=TCP localport=3000
```

### 4.4 App Android
1. Abrir la carpeta `android` en Android Studio y esperar el Gradle Sync.
2. (Opcional) Poner la IP del PC en `android/gradle.properties` → `API_HOST=192.168.x.x`. No es obligatorio: la app también la busca sola o se cambia en **Ajustes**.

## 5. Orden de ejecución

1. **Prender MongoDB.**
2. **Doble clic en `iniciar.bat`.** Abre dos ventanas:
   - *Backend*: debe decir `Conexión a MongoDB establecida`, `Servidor escuchando en http://localhost:3000` y `Anunciando el servidor en la red local`.
   - *Frontend*: compila el panel y lo abre en `http://localhost:4200`.
3. **Panel:** iniciar sesión con una cuenta de administrador.
4. **App:** en Android Studio, **Run ▶** con el celular conectado (o instalar el APK). Si no conecta, ir a **Ajustes → Buscar servidor ahora**.

Comandos equivalentes sin el `.bat`:
```bash
cd backend && node index.js          # API en http://localhost:3000
cd web-angular && npx ng serve       # Panel en http://localhost:4200
```

## 6. Configuración de la URL de la API

| Cliente | Dónde se configura |
|---|---|
| Panel web | `web-angular/src/environments/environment.ts` usa `apiUrl: '/api'`. El archivo `proxy.conf.json` redirige `/api` y `/uploads` a `http://127.0.0.1:3000`. |
| App Android | IP por defecto en `android/gradle.properties` (`API_HOST`). En la app: **Ajustes** para escribirla, probarla o buscarla automáticamente. El backend se anuncia por UDP en el puerto 41234 para que la app lo encuentre. |

Puertos: **3000** TCP (API), **4200** TCP (panel), **41234** UDP (anuncio para la app).

## 7. Pruebas

- **Automáticas del backend:** `cd backend && npm test` → 19 pruebas (reglas de estado, imágenes, validaciones y autenticación).
- **Postman:** importar `documentacion/La_Abundancia.postman_collection.json` y ejecutarla completa con *Run collection* (55 peticiones con validaciones). Crea usuarios y datos de prueba.
- **Matriz de pruebas:** en el documento técnico, sección 7.

## 8. Generar el APK

En Android Studio: **Build → Build App Bundle(s) / APK(s) → Build APK(s)**. El archivo queda en `android/app/build/outputs/apk/debug/app-debug.apk`.

## 9. Problemas comunes

| Síntoma | Solución |
|---|---|
| Panel: "No se pudo conectar con el servidor" | El backend no está corriendo o MongoDB está apagado. Prender MongoDB y volver a correr `iniciar.bat`. |
| Panel: "Tu sesión expiró" | El token dura 8 horas; volver a iniciar sesión. |
| App: "No se pudo conectar con 192.168…" | La IP del PC cambió: **Ajustes → Buscar servidor ahora**. Revisar el firewall (sección 4.3) y que estén en el mismo Wi-Fi. |
| La búsqueda automática no encuentra el servidor | Algunas redes institucionales bloquean la comunicación entre dispositivos. Usar el punto de acceso del celular. |
| Backend: `EADDRINUSE` | Ya hay otro backend abierto. Cerrar todas las ventanas y correr `iniciar.bat` una vez. |

## 10. Datos y respaldo

- Los datos quedan en la base local `abundancia` de MongoDB y las fotos en `backend/uploads`.
- Respaldo: `mongodump --db abundancia --out respaldo` y copiar la carpeta `backend/uploads`. Restaurar con `mongorestore respaldo`.
- No subir `backend/.env` ni `node_modules` al repositorio.
