import { Component, OnInit, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';
import { ApiService, UsuarioDetalle, mensajeDeError } from '../../services/api';
import { LayoutComponent } from '../../shared/layout/layout';
import { EstadoBadgeComponent } from '../../shared/estado-badge/estado-badge';

/** Detalle de un usuario: datos, sus publicaciones y sus solicitudes (historial). */
@Component({
  selector: 'app-usuario-detalle',
  standalone: true,
  imports: [RouterLink, DatePipe, LayoutComponent, EstadoBadgeComponent],
  templateUrl: './usuario-detalle.html',
  styleUrls: ['../../shared/panel.css'],
})
export class UsuarioDetalleComponent implements OnInit {
  id = '';
  usuario = signal<UsuarioDetalle | null>(null);
  cargando = signal(true);
  error = signal('');

  constructor(private api: ApiService, private route: ActivatedRoute) {}

  ngOnInit() {
    this.id = this.route.snapshot.paramMap.get('id') ?? '';
    this.cargar();
  }

  cargar() {
    this.cargando.set(true);
    this.error.set('');
    this.api.getUsuario(this.id).subscribe({
      next: u => { this.usuario.set(u); this.cargando.set(false); },
      error: e => { this.error.set(mensajeDeError(e, 'No se pudo cargar el usuario.')); this.cargando.set(false); },
    });
  }

  toggleActivo() {
    const u = this.usuario();
    if (!u) return;
    const accion = u.activo ? 'suspender' : 'activar';
    const extra = u.activo ? ' No podrá iniciar sesión en la app.' : '';
    if (!confirm(`¿Deseas ${accion} a "${u.nombre}"?${extra}`)) return;
    this.api.toggleActivoUsuario(u._id).subscribe({
      next: act => this.usuario.update(x => x ? { ...x, activo: act.activo } : x),
      error: e => alert(mensajeDeError(e, `No se pudo ${accion} el usuario.`)),
    });
  }
}
