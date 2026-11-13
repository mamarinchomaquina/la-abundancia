import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { AuthService } from './auth';
import { environment } from '../../environments/environment';

export interface Estadisticas {
  usuarios: number;
  publicaciones: { pendiente: number; aprobada: number; rechazada: number; entregada: number };
  solicitudes:   { pendiente: number; aceptada: number; rechazada: number };
}

export interface Publicacion {
  _id: string;
  titulo: string;
  descripcion: string;
  categoria: string;
  cantidad: number;
  ubicacion: string;
  estado: 'pendiente' | 'aprobada' | 'rechazada' | 'entregada';
  fecha: string;
  createdAt: string;
  usuario: { _id: string; nombre: string } | string;
  imagen?: string | null;
}

export interface UsuarioAdmin {
  _id: string;
  nombre: string;
  email: string;
  rol: 'usuario' | 'admin';
  activo: boolean;
  publicaciones: number;
  createdAt: string;
}

export interface PublicacionForm {
  titulo: string;
  descripcion: string;
  categoria: string;
  cantidad: number;
  ubicacion: string;
}

export interface Solicitud {
  _id: string;
  publicacion: { _id: string; titulo: string; categoria?: string; ubicacion?: string; estado?: string; usuario?: string } | null;
  usuario: { _id: string; nombre: string } | string;
  mensaje: string;
  estado: 'pendiente' | 'aceptada' | 'rechazada';
  createdAt: string;
}

export interface UsuarioDetalle extends Omit<UsuarioAdmin, 'publicaciones'> {
  updatedAt: string;
  publicaciones: Publicacion[];
  solicitudes: Solicitud[];
}

export interface UsuarioForm {
  nombre: string;
  email: string;
  rol: 'usuario' | 'admin';
  password?: string;
}

export const CATEGORIAS = ['Alimentos', 'Ropa', 'Útiles', 'Otros'];

/** Mismas reglas que valida el backend con Multer */
export const IMAGEN_TIPOS = ['image/jpeg', 'image/png', 'image/webp'];
export const IMAGEN_MAX_BYTES = 2 * 1024 * 1024;

/** URL completa de una foto guardada en el backend ("/uploads/xxx.jpg"). */
export function urlImagen(ruta?: string | null): string | null {
  if (!ruta) return null;
  return environment.apiUrl.replace(/\/api\/?$/, '') + ruta;
}

export interface Graficas {
  porCategoria: { categoria: string; total: number }[];
  porSemana:    { label: string; total: number }[];
}

@Injectable({ providedIn: 'root' })
export class ApiService {
  private base = environment.apiUrl;

  constructor(private http: HttpClient, private auth: AuthService) {}

  private headers(): HttpHeaders {
    return new HttpHeaders({ Authorization: `Bearer ${this.auth.token()}` });
  }

  getEstadisticas() {
    return this.http.get<Estadisticas>(`${this.base}/estadisticas`, { headers: this.headers() });
  }

  getGraficas() {
    return this.http.get<Graficas>(`${this.base}/graficas`, { headers: this.headers() });
  }

  getPublicaciones() {
    return this.http.get<Publicacion[]>(`${this.base}/publicaciones`, { headers: this.headers() });
  }

  aprobarPublicacion(id: string) {
    return this.http.put<Publicacion>(`${this.base}/publicaciones/${id}`, { estado: 'aprobada' }, { headers: this.headers() });
  }

  rechazarPublicacion(id: string) {
    return this.http.put<Publicacion>(`${this.base}/publicaciones/${id}`, { estado: 'rechazada' }, { headers: this.headers() });
  }
  marcarEntregada(id: string) {
    return this.http.put<Publicacion>(`${this.base}/publicaciones/${id}`, { estado: 'entregada' }, { headers: this.headers() });
  }


  eliminarPublicacion(id: string) {
    return this.http.delete(`${this.base}/publicaciones/${id}`, { headers: this.headers() });
  }

  getUsuarios() {
    return this.http.get<UsuarioAdmin[]>(`${this.base}/usuarios`, { headers: this.headers() });
  }

  toggleActivoUsuario(id: string) {
    return this.http.patch<UsuarioAdmin>(`${this.base}/usuarios/${id}/activo`, {}, { headers: this.headers() });
  }

  // ── Publicaciones (detalle y formularios) ──
  getPublicacion(id: string) {
    return this.http.get<Publicacion>(`${this.base}/publicaciones/${id}`, { headers: this.headers() });
  }

  crearPublicacion(datos: PublicacionForm) {
    return this.http.post<Publicacion>(`${this.base}/publicaciones`, datos, { headers: this.headers() });
  }

  editarPublicacion(id: string, datos: PublicacionForm) {
    return this.http.put<Publicacion>(`${this.base}/publicaciones/${id}`, datos, { headers: this.headers() });
  }

  /** Sube la foto (multipart/form-data, campo "imagen"). No se fija Content-Type: el navegador lo arma. */
  subirImagen(id: string, archivo: File) {
    const datos = new FormData();
    datos.append('imagen', archivo);
    return this.http.post<Publicacion>(`${this.base}/publicaciones/${id}/imagen`, datos, { headers: this.headers() });
  }

  eliminarImagen(id: string) {
    return this.http.delete<Publicacion>(`${this.base}/publicaciones/${id}/imagen`, { headers: this.headers() });
  }

  // ── Solicitudes ──
  getSolicitudes() {
    return this.http.get<Solicitud[]>(`${this.base}/solicitudes`, { headers: this.headers() });
  }

  cambiarEstadoSolicitud(id: string, estado: 'aceptada' | 'rechazada') {
    return this.http.patch<Solicitud>(`${this.base}/solicitudes/${id}/estado`, { estado }, { headers: this.headers() });
  }

  // ── Usuarios (detalle y formularios) ──
  getUsuario(id: string) {
    return this.http.get<UsuarioDetalle>(`${this.base}/usuarios/${id}`, { headers: this.headers() });
  }

  crearUsuario(datos: UsuarioForm) {
    return this.http.post<UsuarioAdmin>(`${this.base}/usuarios`, datos, { headers: this.headers() });
  }

  editarUsuario(id: string, datos: UsuarioForm) {
    return this.http.put<UsuarioAdmin>(`${this.base}/usuarios/${id}`, datos, { headers: this.headers() });
  }
}

/** Mensaje de error que devuelve el servidor ({ error: '...' }) o uno por defecto. */
export function mensajeDeError(e: any, porDefecto: string): string {
  if (e?.status === 0) return 'No se pudo conectar con el servidor. Revisa que el backend esté encendido.';
  return e?.error?.error ?? porDefecto;
}
