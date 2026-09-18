import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./pages/landing/landing.component').then(m => m.LandingComponent),
    title: 'NexoERP | Ámbito Comercial y Contable'
  },
  {
    path: 'catalogo',
    loadComponent: () =>
      import('./features/catalogo/catalogo.component').then(m => m.CatalogoComponent),
    title: 'NexoERP | Catálogo de Artículos'
  },
  {
    path: '**',
    redirectTo: ''
  }
];
