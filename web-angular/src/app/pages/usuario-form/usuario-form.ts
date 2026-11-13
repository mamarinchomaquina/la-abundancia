import { Component, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { ApiService, UsuarioForm, mensajeDeError } from '../../services/api';
import { AuthService } from '../../services/auth';
import { LayoutComponent } from '../../shared/layout/layout';

/** Valida que "confirmar" sea igual a "password". */
function passwordsIguales(grupo: AbstractControl): ValidationErrors | null {
  const pass = grupo.get('password')?.value ?? '';
  const conf = grupo.get('confirmar')?.value ?? '';
  return pass === conf ? null : { noCoinciden: true };
}

/** Formulario reutilizable: crea (/usuarios/nuevo) o edita (/usuarios/:id/editar). */
@Component({
  selector: 'app-usuario-form',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, LayoutComponent],
  templateUrl: './usuario-form.html',
  styleUrls: ['../../shared/panel.css'],
})
export class UsuarioFormComponent implements OnInit {
  id: string | null = null;
  cargando = signal(false);
  guardando = signal(false);
  error = signal('');
  esYoMismo = false;

  form = this.fb.nonNullable.group(
    {
      nombre: ['', [Validators.required, Validators.maxLength(100), Validators.pattern(/\S/)]],
      email: ['', [Validators.required, Validators.email, Validators.maxLength(254)]],
      rol: ['usuario' as 'usuario' | 'admin', Validators.required],
      password: ['', [Validators.minLength(8)]],
      confirmar: [''],
    },
    { validators: passwordsIguales },
  );

  constructor(
    private fb: FormBuilder,
    private api: ApiService,
    private auth: AuthService,
    private route: ActivatedRoute,
    private router: Router,
  ) {}

  get editando() { return this.id !== null; }

  ngOnInit() {
    this.id = this.route.snapshot.paramMap.get('id');
    if (!this.id) {
      // Al crear, la contraseña es obligatoria
      this.form.controls.password.addValidators(Validators.required);
      this.form.controls.password.updateValueAndValidity();
      return;
    }
    this.esYoMismo = this.auth.usuario()?.id === this.id;
    this.cargando.set(true);
    this.api.getUsuario(this.id).subscribe({
      next: u => {
        this.form.patchValue({ nombre: u.nombre, email: u.email, rol: u.rol });
        if (this.esYoMismo) this.form.controls.rol.disable();
        this.cargando.set(false);
      },
      error: e => { this.error.set(mensajeDeError(e, 'No se pudo cargar el usuario.')); this.cargando.set(false); },
    });
  }

  invalido(campo: 'nombre' | 'email' | 'rol' | 'password' | 'confirmar') {
    const c = this.form.controls[campo];
    const tocado = c.touched || c.dirty;
    if (campo === 'confirmar') return tocado && this.form.hasError('noCoinciden');
    return c.invalid && tocado;
  }

  guardar() {
    if (this.guardando()) return;
    this.form.markAllAsTouched();
    if (this.form.invalid) return;

    const v = this.form.getRawValue();
    const datos: UsuarioForm = { nombre: v.nombre.trim(), email: v.email.trim(), rol: v.rol };
    if (v.password) datos.password = v.password;
    if (this.esYoMismo) delete (datos as Partial<UsuarioForm>).rol;

    this.guardando.set(true);
    this.error.set('');
    const peticion = this.id ? this.api.editarUsuario(this.id, datos) : this.api.crearUsuario(datos);
    peticion.subscribe({
      next: u => this.router.navigate(['/usuarios', u._id]),
      error: e => { this.error.set(mensajeDeError(e, 'No se pudo guardar el usuario.')); this.guardando.set(false); },
    });
  }
}
