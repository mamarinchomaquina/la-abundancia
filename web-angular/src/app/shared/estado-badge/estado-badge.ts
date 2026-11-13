import { Component, Input } from '@angular/core';

const ETIQUETAS: Record<string, string> = {
  pendiente: 'Pendiente',
  aprobada: 'Aprobada',
  rechazada: 'Rechazada',
  entregada: 'Entregada',
  aceptada: 'Aceptada',
};

/** Insignia de estado reutilizable (publicaciones y solicitudes). */
@Component({
  selector: 'app-estado-badge',
  standalone: true,
  template: `<span class="badge" [class]="'badge estado-' + estado">{{ etiqueta }}</span>`,
  styles: [`
    .badge { display: inline-block; font-size: 11.5px; font-weight: 500; padding: 3px 10px; border-radius: 20px; white-space: nowrap; }
    .estado-pendiente { background: var(--amarillo-suave); color: var(--amarillo); }
    .estado-aprobada, .estado-aceptada { background: var(--morado-suave); color: var(--morado); }
    .estado-rechazada { background: var(--rojo-suave); color: var(--rojo); }
    .estado-entregada { background: var(--fondo); color: var(--texto-suave); border: 1px solid var(--borde); }
  `],
})
export class EstadoBadgeComponent {
  @Input({ required: true }) estado = '';
  get etiqueta() { return ETIQUETAS[this.estado] ?? this.estado; }
}
