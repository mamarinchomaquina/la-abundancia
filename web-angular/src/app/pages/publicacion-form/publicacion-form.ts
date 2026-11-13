import { Component, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Observable, of, switchMap } from 'rxjs';
import {
  ApiService, CATEGORIAS, IMAGEN_MAX_BYTES, IMAGEN_TIPOS, Publicacion, PublicacionForm, mensajeDeError, urlImagen,
} from '../../services/api';
import { LayoutComponent } from '../../shared/layout/layout';

/** Formulario reutilizable: crea (/publicaciones/nueva) o edita (/publicaciones/:id/editar). */
@Component({
  selector: 'app-publicacion-form',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, LayoutComponent],
  templateUrl: './publicacion-form.html',
  styleUrls: ['../../shared/panel.css'],
})
export class PublicacionFormComponent implements OnInit {
  categorias = CATEGORIAS;
  id: string | null = null;
  cargando = signal(false);
  guardando = signal(false);
  error = signal('');

  // Foto: archivo elegido (aún sin subir), vista previa y foto actual guardada en el servidor
  archivo: File | null = null;
  vistaPrevia = signal<string | null>(null);
  fotoActual = signal<string | null>(null);
  errorFoto = signal('');
  quitarFoto = false;

  form = this.fb.nonNullable.group({
    titulo: ['', [Validators.required, Validators.maxLength(150), Validators.pattern(/\S/)]],
    descripcion: ['', [Validators.required, Validators.maxLength(3000), Validators.pattern(/\S/)]],
    categoria: ['', Validators.required],
    cantidad: [1, [Validators.required, Validators.min(1), Validators.max(99999), Validators.pattern(/^\d+$/)]],
    ubicacion: ['', [Validators.required, Validators.maxLength(200), Validators.pattern(/\S/)]],
  });

  constructor(
    private fb: FormBuilder,
    private api: ApiService,
    private route: ActivatedRoute,
    private router: Router,
  ) {}

  get editando() { return this.id !== null; }

  ngOnInit() {
    this.id = this.route.snapshot.paramMap.get('id');
    if (!this.id) return;
    this.cargando.set(true);
    this.api.getPublicacion(this.id).subscribe({
      next: p => {
        this.form.patchValue({
          titulo: p.titulo, descripcion: p.descripcion, categoria: p.categoria,
          cantidad: p.cantidad, ubicacion: p.ubicacion,
        });
        this.fotoActual.set(urlImagen(p.imagen));
        this.cargando.set(false);
      },
      error: e => { this.error.set(mensajeDeError(e, 'No se pudo cargar la publicación.')); this.cargando.set(false); },
    });
  }

  /** ¿Mostrar el error del campo? (solo después de tocarlo o de intentar guardar) */
  invalido(campo: 'titulo' | 'descripcion' | 'categoria' | 'cantidad' | 'ubicacion') {
    const c = this.form.controls[campo];
    return c.invalid && (c.touched || c.dirty);
  }

  elegirFoto(evento: Event) {
    const input = evento.target as HTMLInputElement;
    const f = input.files?.[0] ?? null;
    this.errorFoto.set('');
    if (!f) return;
    // Misma validación que el backend (Multer), para avisar antes de enviar
    if (!IMAGEN_TIPOS.includes(f.type)) {
      this.errorFoto.set('Formato no permitido. Usa JPG, PNG o WEBP.');
      input.value = '';
      return;
    }
    if (f.size > IMAGEN_MAX_BYTES) {
      this.errorFoto.set('La imagen supera el tamaño máximo de 2 MB.');
      input.value = '';
      return;
    }
    this.archivo = f;
    this.quitarFoto = false;
    const previa = this.vistaPrevia();
    if (previa) URL.revokeObjectURL(previa);
    this.vistaPrevia.set(URL.createObjectURL(f));
  }

  descartarFoto(input: HTMLInputElement) {
    input.value = '';
    this.archivo = null;
    const previa = this.vistaPrevia();
    if (previa) URL.revokeObjectURL(previa);
    this.vistaPrevia.set(null);
    if (this.fotoActual()) { this.quitarFoto = true; this.fotoActual.set(null); }
  }

  /** Después de guardar los datos: sube la foto nueva o borra la anterior si se pidió. */
  private procesarFoto(p: Publicacion): Observable<Publicacion> {
    if (this.archivo) return this.api.subirImagen(p._id, this.archivo);
    if (this.quitarFoto) return this.api.eliminarImagen(p._id);
    return of(p);
  }

  guardar() {
    if (this.guardando()) return;          // evita doble envío
    this.form.markAllAsTouched();
    if (this.form.invalid) return;

    const v = this.form.getRawValue();
    const datos: PublicacionForm = {
      titulo: v.titulo.trim(),
      descripcion: v.descripcion.trim(),
      categoria: v.categoria,
      cantidad: Number(v.cantidad),
      ubicacion: v.ubicacion.trim(),
    };

    this.guardando.set(true);
    this.error.set('');
    const peticion = this.id ? this.api.editarPublicacion(this.id, datos) : this.api.crearPublicacion(datos);
    const paso: { guardada: Publicacion | null } = { guardada: null };
    peticion.pipe(
      switchMap(p => { paso.guardada = p; return this.procesarFoto(p); }),
    ).subscribe({
      next: p => this.router.navigate(['/publicaciones', p._id]),
      error: e => {
        if (paso.guardada) {
          // Los datos sí se guardaron; solo falló la foto. Se pasa a modo edición para reintentar.
          this.id = paso.guardada._id;
          this.error.set('Los datos se guardaron, pero la foto no: ' + mensajeDeError(e, 'error al subir la imagen.'));
        } else {
          // Si falla, el formulario conserva lo escrito
          this.error.set(mensajeDeError(e, 'No se pudo guardar la publicación.'));
        }
        this.guardando.set(false);
      },
    });
  }
}
