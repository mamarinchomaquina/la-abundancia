import { Component, OnInit, computed, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { ApiService, Solicitud, mensajeDeError } from '../../services/api';
import { LayoutComponent } from '../../shared/layout/layout';
import { EstadoBadgeComponent } from '../../shared/estado-badge/estado-badge';

type Filtro = 'todas' | 'pendiente' | 'aceptada' | 'rechazada';

@Component({
  selector: 'app-solicitudes',
  standalone: true,
  imports: [RouterLink, FormsModule, DatePipe, LayoutComponent, EstadoBadgeComponent],
  templateUrl: './solicitudes.html',
  styleUrls: ['../../shared/panel.css'],
})
export class SolicitudesComponent implements OnInit {
  items = signal<Solicitud[]>([]);
  cargando = signal(true);
  error = signal('');
  aviso = signal('');
  procesando = signal<string | null>(null);
  filtro = signal<Filtro>('todas');
  busqueda = signal('');

  filtroOpciones: { valor: Filtro; etiqueta: string }[] = [
    { valor: 'todas', etiqueta: 'Todas' },
    { valor: 'pendiente', etiqueta: 'Pendientes' },
    { valor: 'aceptada', etiqueta: 'Aceptadas' },
    { valor: 'rechazada', etiqueta: 'Rechazadas' },
  ];

  filtradas = computed(() => {
    const f = this.filtro();
    const b = this.busqueda().toLowerCase().trim();
    return this.items().filter(s =>
      (f === 'todas' || s.estado === f) &&
      (!b ||
        (s.publicacion?.titulo ?? '').toLowerCase().includes(b) ||
        this.solicitante(s).toLowerCase().includes(b) ||
        (s.mensaje ?? '').toLowerCase().includes(b))
    );
  });

  constructor(private api: ApiService) {}

  ngOnInit() { this.cargar(); }

  cargar() {
    this.cargando.set(true);
    this.error.set('');
    this.api.getSolicitudes().subscribe({
      next: data => { this.items.set(data); this.cargando.set(false); },
      error: e => { this.error.set(mensajeDeError(e, 'No se pudieron cargar las solicitudes.')); this.cargando.set(false); },
    });
  }

  resolver(s: Solicitud, estado: 'aceptada' | 'rechazada') {
    const accion = estado === 'aceptada' ? 'aceptar' : 'rechazar';
    if (!confirm(`¿Deseas ${accion} la solicitud de ${this.solicitante(s)} para "${s.publicacion?.titulo}"?`)) return;
    if (this.procesando()) return;
    this.procesando.set(s._id);
    this.aviso.set('');
    this.api.cambiarEstadoSolicitud(s._id, estado).subscribe({
      next: act => {
        this.items.update(list => list.map(x => x._id === act._id ? { ...x, estado: act.estado } : x));
        this.aviso.set(`Solicitud ${estado}.`);
        this.procesando.set(null);
      },
      error: e => {
        alert(mensajeDeError(e, `No se pudo ${accion} la solicitud.`));
        this.procesando.set(null);
      },
    });
  }

  solicitante(s: Solicitud): string {
    return typeof s.usuario === 'object' && s.usuario ? s.usuario.nombre : '—';
  }

  solicitanteId(s: Solicitud): string | null {
    return typeof s.usuario === 'object' && s.usuario ? s.usuario._id : null;
  }
}
