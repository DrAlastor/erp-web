import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./pages/landing/landing.component').then(m => m.LandingComponent),
    title: 'NexoERP | Ámbito Comercial y Contable'
  },
  {
    path: 'login',
    loadComponent: () => import('./pages/auth/login/login.component').then(m => m.LoginComponent),
    title: 'Iniciar Sesión | NexoERP'
  },
  {
    path: 'app',
    loadComponent: () => import('./pages/main-layout/main-layout.component').then(m => m.MainLayoutComponent),
    canActivate: [authGuard],
    title: 'Panel | NexoERP'
  },
  {
    path: '**',
    redirectTo: ''
  }
];
