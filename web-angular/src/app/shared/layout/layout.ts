import { Component, Input } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { UpperCasePipe } from '@angular/common';
import { AuthService } from '../../services/auth';

/**
 * Estructura común del panel: sidebar morado + cabecera + contenido.
 * Uso: <app-layout titulo="..." subtitulo="..."> ...contenido... </app-layout>
 * Lo que tenga el atributo "acciones" se ubica a la derecha de la cabecera.
 */
@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, UpperCasePipe],
  templateUrl: './layout.html',
  styleUrl: './layout.css',
})
export class LayoutComponent {
  @Input({ required: true }) titulo = '';
  @Input() subtitulo = '';

  constructor(public auth: AuthService, private router: Router) {}

  cerrarSesion() {
    this.auth.logout();
    this.router.navigate(['/login']);
  }
}
