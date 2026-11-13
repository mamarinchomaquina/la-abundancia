import { Component, OnInit, signal, computed } from '@angular/core';
import { RouterLink, RouterLinkActive, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { UpperCasePipe } from '@angular/common';
import { AuthService } from '../../services/auth';
import { ApiService, Publicacion, urlImagen } from '../../services/api';

type Filtro = 'todas' | 'pendiente' | 'aprobada' | 'rechazada' | 'entregada';

@Component({
  selector: 'app-publicaciones',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, FormsModule, UpperCasePipe],
  templateUrl: './publicaciones.html',
  styleUrls: ['../../shared/panel.css', './publicaciones.css'],
})
export class PublicacionesComponent implements OnInit {
  items    = signal<Publicacion[]>([]);
  cargando = signal(true);
  error    = signal('');
  filtro   = signal<Filtro>('todas');
  buscando = '';

  filtroOpciones: { valor: Filtro; etiqueta: string }[] = [
    { valor: 'todas',     etiqueta: 'Todas' },
    { valor: 'pendiente', etiqueta: 'Pendientes' },
    { valor: 'aprobada',  etiqueta: 'Aprobadas' },
    { valor: 'rechazada', etiqueta: 'Rechazadas' },
    { valor: 'entregada', etiqueta: 'Entregadas' },
  ];

  filtradas = computed(() => {
    const f = this.filtro();
    const b = this.buscando.toLowerCase();
    return this.items().filter(p =>
      (f === 'todas' || p.estado === f) &&
      (!b || p.titulo.toLowerCase().includes(b) || p.categoria.toLowerCase().includes(b))
    );
  });

  // Paleta de gradientes para las cards
  private gradientes = [
    'linear-gradient(135deg, #6B21A8 0%, #7C3AED 100%)',
    'linear-gradient(135deg, #065F46 0%, #059669 100%)',
    'linear-gradient(135deg, #92400E 0%, #D97706 100%)',
    'linear-gradient(135deg, #991B1B 0%, #DC2626 100%)',
    'linear-gradient(135deg, #1E40AF 0%, #3B82F6 100%)',
    'linear-gradient(135deg, #064E3B  0%, #10B981 100%)',
    'linear-gradient(135deg, #7C2D12 0%, #EA580C 100%)',
    'linear-gradient(135deg, #1E3A5F 0%, #0EA5E9 100%)',
  ];

  constructor(
    public auth: AuthService,
    private api: ApiService,
    private router: Router,
  ) {}

  ngOnInit() { this.cargar(); }

  cargar() {
    this.cargando.set(true);
    this.api.getPublicaciones().subscribe({
      next: (data) => { this.items.set(data); this.cargando.set(false); },
      error: ()     => { this.error.set('No se pudieron cargar las publicaciones.'); this.cargando.set(false); },
    });
  }

  aprobar(pub: Publicacion) {
    this.api.aprobarPublicacion(pub._id).subscribe({
      next: (actualizado) => {
        this.items.update(list => list.map(p => p._id === actualizado._id ? { ...p, estado: actualizado.estado } : p));
      },
      error: (e) => alert(this.mensajeError(e, 'No se pudo aprobar la publicación.')),
    });
  }

  rechazar(pub: Publicacion) {
    this.api.rechazarPublicacion(pub._id).subscribe({
      next: (actualizado) => {
        this.items.update(list => list.map(p => p._id === actualizado._id ? { ...p, estado: actualizado.estado } : p));
      },
      error: (e) => alert(this.mensajeError(e, 'No se pudo rechazar la publicación.')),
    });
  }

  marcarEntregada(pub: Publicacion) {
    if (!confirm(`¿Marcar "${pub.titulo}" como entregada? Ya no podrá cambiar de estado.`)) return;
    this.api.marcarEntregada(pub._id).subscribe({
      next: (actualizado) => {
        this.items.update(list => list.map(p => p._id === actualizado._id ? { ...p, estado: actualizado.estado } : p));
      },
      error: (e) => alert(this.mensajeError(e, 'No se pudo marcar como entregada.')),
    });
  }

  /** Muestra el mensaje que devuelve el servidor (reglas de negocio) */
  private mensajeError(e: any, porDefecto: string): string {
    return e?.error?.error ?? porDefecto;
  }

  eliminar(pub: Publicacion) {
    if (!confirm(`¿Eliminar "${pub.titulo}"? Esta acción no se puede deshacer.`)) return;
    this.api.eliminarPublicacion(pub._id).subscribe({
      next: () => this.items.update(list => list.filter(p => p._id !== pub._id)),
      error: (e) => alert(this.mensajeError(e, 'No se pudo eliminar la publicación.')),
    });
  }

  cerrarSesion() {
    this.auth.logout();
    this.router.navigate(['/login']);
  }

  nombreUsuario(pub: Publicacion): string {
    return typeof pub.usuario === 'object' ? pub.usuario.nombre : '—';
  }

  cambiarFiltro(f: Filtro) { this.filtro.set(f); }

  foto(pub: Publicacion) { return urlImagen(pub.imagen); }

  colorBanner(id: string): string {
    // Asignar gradiente consistente según el ID
    let hash = 0;
    for (let i = 0; i < id.length; i++) {
      hash = (hash * 31 + id.charCodeAt(i)) & 0xFFFFFF;
    }
    return this.gradientes[hash % this.gradientes.length];
  }

  emojiCategoria(categoria: string, titulo: string): string {
    const t = titulo.toLowerCase();
    // Comida específica
    if (t.includes('arroz') || t.includes('mazamorra')) return '🍚';
    if (t.includes('pandebono') || t.includes('pan') || t.includes('arepa')) return '🍞';
    if (t.includes('tamal')) return '🌽';
    if (t.includes('sancocho') || t.includes('caldo') || t.includes('sopa')) return '🍲';
    if (t.includes('frijol') || t.includes('frijoles')) return '🫘';
    if (t.includes('empanada')) return '🥟';
    if (t.includes('leche') || t.includes('lácteo')) return '🥛';
    if (t.includes('queso')) return '🧀';
    if (t.includes('postre') || t.includes('cocada') || t.includes('dulce')) return '🍮';
    if (t.includes('arroz')) return '🍚';
    if (t.includes('plátano') || t.includes('platano')) return '🍌';
    if (t.includes('aguacate')) return '🥑';
    if (t.includes('ají') || t.includes('aji') || t.includes('picante')) return '🌶️';
    if (t.includes('aceite')) return '🫙';
    if (t.includes('pasta') || t.includes('espagueti')) return '🍝';
    if (t.includes('atún') || t.includes('sardina')) return '🐟';
    if (t.includes('chocoramo') || t.includes('galleta')) return '🍪';
    // Ropa
    if (categoria === 'Ropa') {
      if (t.includes('zapato') || t.includes('bota')) return '👟';
      if (t.includes('chaqueta') || t.includes('abrigo')) return '🧥';
      if (t.includes('pantalón') || t.includes('pantalon') || t.includes('jean')) return '👖';
      if (t.includes('niño') || t.includes('bebé') || t.includes('bebe')) return '👶';
      return '👕';
    }
    // Útiles
    if (categoria === 'Útiles') {
      if (t.includes('cuaderno') || t.includes('libro')) return '📓';
      if (t.includes('color') || t.includes('pintura')) return '🎨';
      if (t.includes('calculadora')) return '🔢';
      if (t.includes('morral') || t.includes('mochila')) return '🎒';
      if (t.includes('diccionario')) return '📚';
      if (t.includes('juguete')) return '🧸';
      return '✏️';
    }
    // General alimentos
    if (categoria === 'Alimentos') return '🥘';
    return '📦';
  }

  etiquetaEstado(estado: string): string {
    const map: Record<string, string> = {
      pendiente: 'Pendiente',
      aprobada:  'Aprobado',
      rechazada: 'Rechazado',
      entregada: 'Entregado',
    };
    return map[estado] ?? estado;
  }
}
