import { Routes } from '@angular/router';
import { authGuard } from './guards/auth-guard';

export const routes: Routes = [
  { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
  {
    path: 'login',
    loadComponent: () =>
      import('./pages/login/login').then(m => m.LoginComponent),
  },
  {
    path: 'dashboard',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./pages/dashboard/dashboard').then(m => m.DashboardComponent),
  },
  {
    path: 'publicaciones',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./pages/publicaciones/publicaciones').then(m => m.PublicacionesComponent),
  },
  {
    path: 'publicaciones/nueva',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./pages/publicacion-form/publicacion-form').then(m => m.PublicacionFormComponent),
  },
  {
    path: 'publicaciones/:id',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./pages/publicacion-detalle/publicacion-detalle').then(m => m.PublicacionDetalleComponent),
  },
  {
    path: 'publicaciones/:id/editar',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./pages/publicacion-form/publicacion-form').then(m => m.PublicacionFormComponent),
  },
  {
    path: 'solicitudes',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./pages/solicitudes/solicitudes').then(m => m.SolicitudesComponent),
  },
  {
    path: 'usuarios/nuevo',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./pages/usuario-form/usuario-form').then(m => m.UsuarioFormComponent),
  },
  {
    path: 'usuarios/:id',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./pages/usuario-detalle/usuario-detalle').then(m => m.UsuarioDetalleComponent),
  },
  {
    path: 'usuarios/:id/editar',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./pages/usuario-form/usuario-form').then(m => m.UsuarioFormComponent),
  },
  {
    path: 'usuarios',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./pages/usuarios/usuarios').then(m => m.UsuariosComponent),
  },
  { path: '**', redirectTo: 'dashboard' },
];
