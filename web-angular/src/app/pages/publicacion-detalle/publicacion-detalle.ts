import { Component, OnInit, computed, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';
import { ApiService, Publicacion, Solicitud, mensajeDeError, urlImagen } from '../../services/api';
import { LayoutComponent } from '../../shared/layout/layout';
import { EstadoBadgeComponent } from '../../shared/estado-badge/estado-badge';

/** Detalle de una publicación con sus solicitudes y las acciones del flujo. */
@Component({
  selector: 'app-publicacion-detalle',
  standalone: true,
  imports: [RouterLink, DatePipe, LayoutComponent, EstadoBadgeComponent],
  templateUrl: './publicacion-detalle.html',
  styleUrls: ['../../shared/panel.css'],
})
export class PublicacionDetalleComponent implements OnInit {
  id = '';
  pub = signal<Publicacion | null>(null);
  solicitudes = signal<Solicitud[]>([]);
  cargando = signal(true);
  error = signal('');
  aviso = signal('');
  procesando = signal(false);

  foto = computed(() => urlImagen(this.pub()?.imagen));

  tieneAceptada = computed(() => this.solicitudes().some(s => s.estado === 'aceptada'));

  constructor(private api: ApiService, private route: ActivatedRoute, private router: Router) {}

  ngOnInit() {
    this.id = this.route.snapshot.paramMap.get('id') ?? '';
    this.cargar();
  }

  cargar() {
    this.cargando.set(true);
    this.error.set('');
    this.api.getPublicacion(this.id).subscribe({
      next: p => { this.pub.set(p); this.cargando.set(false); },
      error: e => { this.error.set(mensajeDeError(e, 'No se pudo cargar la publicación.')); this.cargando.set(false); },
    });
    this.api.getSolicitudes().subscribe({
      next: lista => this.solicitudes.set(lista.filter(s => s.publicacion?._id === this.id)),
      error: () => this.solicitudes.set([]),
    });
  }

  autor(): string {
    const u = this.pub()?.usuario;
    return typeof u === 'object' && u ? u.nombre : '—';
  }

  autorId(): string | null {
    const u = this.pub()?.usuario;
    return typeof u === 'object' && u ? u._id : null;
  }

  solicitante(s: Solicitud): string {
    return typeof s.usuario === 'object' && s.usuario ? s.usuario.nombre : '—';
  }

  cambiarEstado(estado: 'aprobada' | 'rechazada' | 'entregada') {
    const textos = { aprobada: 'aprobar', rechazada: 'rechazar', entregada: 'marcar como entregada' };
    if (!confirm(`¿Deseas ${textos[estado]} esta publicación?`)) return;
    if (this.procesando()) return;
    this.procesando.set(true);
    this.aviso.set('');
    const peticion =
      estado === 'aprobada' ? this.api.aprobarPublicacion(this.id)
      : estado === 'rechazada' ? this.api.rechazarPublicacion(this.id)
      : this.api.marcarEntregada(this.id);
    peticion.subscribe({
      next: p => {
        this.pub.update(actual => actual ? { ...actual, estado: p.estado } : actual);
        this.aviso.set(`La publicación ahora está ${p.estado}.`);
        this.procesando.set(false);
      },
      error: e => { alert(mensajeDeError(e, 'No se pudo cambiar el estado.')); this.procesando.set(false); },
    });
  }

  resolver(s: Solicitud, estado: 'aceptada' | 'rechazada') {
    if (!confirm(`¿Deseas ${estado === 'aceptada' ? 'aceptar' : 'rechazar'} la solicitud de ${this.solicitante(s)}?`)) return;
    this.api.cambiarEstadoSolicitud(s._id, estado).subscribe({
      next: act => this.solicitudes.update(l => l.map(x => x._id === act._id ? { ...x, estado: act.estado } : x)),
      error: e => alert(mensajeDeError(e, 'No se pudo actualizar la solicitud.')),
    });
  }

  eliminar() {
    const p = this.pub();
    if (!p || !confirm(`¿Eliminar "${p.titulo}"? Esta acción no se puede deshacer.`)) return;
    this.api.eliminarPublicacion(this.id).subscribe({
      next: () => this.router.navigate(['/publicaciones']),
      error: e => alert(mensajeDeError(e, 'No se pudo eliminar la publicación.')),
    });
  }
}
