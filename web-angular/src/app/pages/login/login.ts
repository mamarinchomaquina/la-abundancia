import { Component, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../services/auth';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class LoginComponent {
  email    = '';
  password = '';
  error    = signal('');
  cargando = signal(false);

  constructor(private auth: AuthService, private router: Router, route: ActivatedRoute) {
    if (this.auth.isLoggedIn() && this.auth.isAdmin()) {
      this.router.navigate(['/dashboard']);
    } else if (route.snapshot.queryParamMap.get('motivo') === 'sesion') {
      this.error.set('Tu sesión expiró. Ingresa de nuevo.');
    }
  }

  submit() {
    if (!this.email || !this.password) {
      this.error.set('Completa todos los campos.');
      return;
    }
    this.error.set('');
    this.cargando.set(true);

    this.auth.login(this.email, this.password).subscribe({
      next: () => {
        if (!this.auth.isAdmin()) {
          this.auth.logout();
          this.error.set('Esta cuenta no es de administrador. Usa la app móvil.');
          this.cargando.set(false);
          return;
        }
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        this.error.set(err.status === 0
          ? 'No se pudo conectar con el servidor. Revisa que el backend esté encendido.'
          : err.error?.error ?? 'No se pudo iniciar sesión.');
        this.cargando.set(false);
      },
    });
  }
}
