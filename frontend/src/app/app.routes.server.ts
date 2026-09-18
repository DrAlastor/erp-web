import { RenderMode, ServerRoute } from '@angular/ssr';

export const serverRoutes: ServerRoute[] = [
  {
    // La landing es pública y no cambia: se puede generar de antemano.
    path: '',
    renderMode: RenderMode.Prerender
  },
  {
    // El login y el área logueada dependen de la sesión, que vive en el navegador.
    // Prerenderizarlos daría una pantalla que no corresponde al usuario.
    path: 'login',
    renderMode: RenderMode.Client
  },
  {
    path: 'app/**',
    renderMode: RenderMode.Client
  },
  {
    path: '**',
    renderMode: RenderMode.Client
  }
];
