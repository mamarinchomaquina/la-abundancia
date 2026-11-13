import { Component, OnInit, signal, computed } from '@angular/core';
import { RouterLink, RouterLinkActive, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { UpperCasePipe, DatePipe } from '@angular/common';
import { AuthService } from '../../services/auth';
import { ApiService, UsuarioAdmin } from '../../services/api';

@Component({
  selector: 'app-usuarios',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, FormsModule, UpperCasePipe, DatePipe],
  templateUrl: './usuarios.html',
  styleUrls: ['../../shared/panel.css', './usuarios.css'],
})
export class UsuariosComponent implements OnInit {
  items    = signal<UsuarioAdmin[]>([]);
  cargando = signal(true);
  error    = signal('');
  buscando = '';
  filtroActivo = signal<'todos' | 'activo' | 'suspendido'>('todos');

  filtrados = computed(() => {
    const f = this.filtroActivo();
    const b = this.buscando.toLowerCase();
    return this.items().filter(u =>
      (f === 'todos' || (f === 'activo' ? u.activo : !u.activo)) &&
      (!b || u.nombre.toLowerCase().includes(b) || u.email.toLowerCase().includes(b))
    );
  });

  constructor(
    public auth: AuthService,
    private api: ApiService,
    private router: Router,
  ) {}

  ngOnInit() { this.cargar(); }

  cargar() {
    this.cargando.set(true);
    this.api.getUsuarios().subscribe({
      next: (data) => { this.items.set(data); this.cargando.set(false); },
      error: ()     => { this.error.set('No se pudieron cargar los usuarios.'); this.cargando.set(false); },
    });
  }

  toggleActivo(u: UsuarioAdmin) {
    const accion = u.activo ? 'suspender' : 'activar';
    if (!confirm(`¿Deseas ${accion} a "${u.nombre}"?`)) return;
    this.api.toggleActivoUsuario(u._id).subscribe({
      next: (actualizado) => {
        this.items.update(list => list.map(x => x._id === actualizado._id ? actualizado : x));
      },
      error: () => alert(`No se pudo ${accion} el usuario.`),
    });
  }

  cerrarSesion() {
    this.auth.logout();
    this.router.navigate(['/login']);
  }

  cambiarFiltro(f: 'todos' | 'activo' | 'suspendido') {
    this.filtroActivo.set(f);
  }
}
