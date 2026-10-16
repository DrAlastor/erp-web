import { Routes } from '@angular/router';
import { authGuard } from './modulos/seguridad-y-auditoria/cu01-gestionar-acceso-al-sistema/guards/auth.guard';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./pages/landing/landing.component').then(m => m.LandingComponent),
    title: 'NexoERP | Ámbito Comercial y Contable'
  },
  {
    path: 'login',
    loadComponent: () => import('./modulos/seguridad-y-auditoria/cu01-gestionar-acceso-al-sistema/components/login/login.component').then(m => m.LoginComponent),
    title: 'Iniciar Sesión | NexoERP'
  },
  {
    path: 'registro-cliente',
    loadComponent: () => import('./modulos/seguridad-y-auditoria/cu01-gestionar-acceso-al-sistema/components/registro-cliente/registro-cliente.component').then(m => m.RegistroClienteComponent),
    title: 'Registro de Cliente | NexoERP'
  },
  {
    path: 'registro-suscripcion',
    loadComponent: () => import('./pages/registro-suscripcion/registro-suscripcion.component').then(m => m.RegistroSuscripcionComponent),
    title: 'Suscripción & Onboarding de Empresa | NexoERP'
  },
  {
    path: 'recuperar-contrasena',
    loadComponent: () => import('./modulos/seguridad-y-auditoria/cu01-gestionar-acceso-al-sistema/components/recuperar-contrasena/recuperar-contrasena.component').then(m => m.RecuperarContrasenaComponent),
    title: 'Recuperar Contraseña | NexoERP'
  },
  {
    path: 'reset-password',
    redirectTo: 'recuperar-contrasena'
  },
  {
    path: 'app',
    loadComponent: () => import('./pages/main-layout/main-layout.component').then(m => m.MainLayoutComponent),
    canActivate: [authGuard],
    title: 'Panel | NexoERP'
  },
  {
    path: 'app/:funcion',
    loadComponent: () => import('./pages/main-layout/main-layout.component').then(m => m.MainLayoutComponent),
    canActivate: [authGuard],
    title: 'Panel | NexoERP'
  },
  {
    path: '**',
    redirectTo: ''
  }
];
