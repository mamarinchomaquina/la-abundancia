import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { tap } from 'rxjs';
import { environment } from '../../environments/environment';

export interface Usuario {
  id: string;
  nombre: string;
  email: string;
  rol: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly TOKEN_KEY = 'abundancia_token';
  private readonly USER_KEY  = 'abundancia_user';

  usuario = signal<Usuario | null>(this.storedUser());
  token   = signal<string | null>(localStorage.getItem(this.TOKEN_KEY));

  constructor(private http: HttpClient) {}

  login(email: string, password: string) {
    return this.http
      .post<{ token: string; usuario: Usuario }>(
        `${environment.apiUrl}/auth/login`,
        { email, password }
      )
      .pipe(
        tap(({ token, usuario }) => {
          localStorage.setItem(this.TOKEN_KEY, token);
          localStorage.setItem(this.USER_KEY, JSON.stringify(usuario));
          this.token.set(token);
          this.usuario.set(usuario);
        })
      );
  }

  logout() {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.USER_KEY);
    this.token.set(null);
    this.usuario.set(null);
  }

  /** Hay sesión solo si existe un token y NO ha vencido (el backend lo emite por 8 horas). */
  isLoggedIn() {
    const token = this.token();
    if (!token) return false;
    if (this.tokenVencido(token)) {
      this.logout();
      return false;
    }
    return true;
  }

  private tokenVencido(token: string): boolean {
    try {
      const base64 = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
      const payload = JSON.parse(atob(base64));
      return typeof payload.exp === 'number' && payload.exp * 1000 <= Date.now();
    } catch {
      return true; // token mal formado
    }
  }

  isAdmin() {
    return this.usuario()?.rol === 'admin';
  }

  private storedUser(): Usuario | null {
    try {
      const raw = localStorage.getItem(this.USER_KEY);
      return raw ? JSON.parse(raw) : null;
    } catch {
      return null;
    }
  }
}
