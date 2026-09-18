import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { permisoGuard } from './core/guards/permiso.guard';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./pages/landing/landing.component').then(m => m.LandingComponent),
    title: 'NexoERP | Ámbito Comercial y Contable'
  },
  {
    path: 'login',
    loadComponent: () => import('./pages/login/login.component').then(m => m.LoginComponent),
    title: 'Ingresar | NexoERP'
  },
  {
    // Área logueada. El authGuard exige sesión y deja los permisos cargados antes de dibujar.
    path: 'app',
    loadComponent: () => import('./layout/shell.component').then(m => m.ShellComponent),
    canActivate: [authGuard],
    children: [
      {
        path: '',
        loadComponent: () => import('./pages/inicio/inicio.component').then(m => m.InicioComponent),
        title: 'Inicio | NexoERP'
      },
      {
        path: 'sin-acceso',
        loadComponent: () => import('./pages/sin-acceso/sin-acceso.component').then(m => m.SinAccesoComponent),
        title: 'Sin acceso | NexoERP'
      },
      {
        path: 'seguridad/roles',
        loadComponent: () => import('./pages/seguridad/roles/roles.component').then(m => m.RolesComponent),
        canActivate: [permisoGuard('SEGURIDAD_CONSULTAR')],
        title: 'Roles y permisos | NexoERP'
      }
    ]
  },
  {
    path: '**',
    redirectTo: ''
  }
];
