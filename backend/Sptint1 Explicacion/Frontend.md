# Arquitectura del Frontend ERP (Angular 19 Standalone)

El frontend está desarrollado bajo el patrón de **módulos por dominio funcional** utilizando la arquitectura **Standalone Components** de Angular 19. El diseño se adapta estrictamente a los 3 módulos declarados en el backend: `modulo_acceso`, `modulo_comercial` y `modulo_inventario`.

---

## 1. Stack Tecnológico e Infraestructura

* **Framework:** Angular 19+ (Standalone Components, Signals & RxJS)
* **Estilos:** SCSS + Tailwind CSS / Angular Material
* **Estado Global / Data Fetching:** Angular Signals + RxJS (para flujos asíncronos complejos)
* **Cliente HTTP:** `provideHttpClient` con interceptores funcionales (`withInterceptors`)
* **Autenticación:** JWT con control de permisos RBAC granulares segun backend



---

## 2. Estructura General del Proyecto Angular

```text
frontend/
├── public/
├── src/
│   ├── environments/
│   │   ├── environment.ts
│   │   └── environment.development.ts
│   │
│   ├── app/
│   │   ├── components/                 <-- Componentes globales compartidos
│   │   │
│   │   ├── core/                       <-- Infraestructura global del sistema
│   │   │   ├── guards/
│   │   │   │   ├── auth.guard.ts
│   │   │   │   └── permission.guard.ts
│   │   │   ├── interceptors/
│   │   │   │   ├── auth.interceptor.ts
│   │   │   │   └── error.interceptor.ts
│   │   │   ├── models/
│   │   │   │   ├── api-response.model.ts
│   │   │   │   ├── auth.model.ts
│   │   │   │   └── user-session.model.ts
│   │   │   └── services/
│   │   │       ├── auth.service.ts
│   │   │       └── theme.service.ts
│   │   │
│   │   ├── pages/                      <-- Páginas base y Layouts
│   │   │   ├── landing/                <-- Home pública (`/`)
│   │   │   ├── auth/                   <-- Layout público
│   │   │   │   ├── login/              <-- (HU-01) Nicolas
│   │   │   │   └── reset-password/
│   │   │   └── main-layout/            <-- Layout protegido (`/app`)
│   │   │
│   │   ├── modulo_acceso/              <-- (HU-01, HU-02, HU-03) Nicolas, Alessandro, Santi
│   │   │   ├── components/
│   │   │   ├── models/                 <-- (usuario.model.ts, rol.model.ts, permiso.model.ts)
│   │   │   ├── pages/
│   │   │   │   ├── usuarios/           <-- (HU-02) Alessandro
│   │   │   │   ├── roles/              <-- (HU-03) Santi
│   │   │   │   └── bitacora/           <-- (HU-03) Santi
│   │   │   └── services/
│   │   │       ├── usuario.service.ts
│   │   │       ├── rol.service.ts
│   │   │       └── bitacora.service.ts
│   │   │
│   │   ├── modulo_comercial/           <-- (HU-04) Javier
│   │   │   ├── components/
│   │   │   ├── models/                 <-- (cliente.model.ts)
│   │   │   ├── pages/
│   │   │   │   └── clientes/           <-- (HU-04) Javier
│   │   │   └── services/
│   │   │       └── cliente.service.ts
│   │   │
│   │   ├── modulo_inventario/          <-- (HU-05, HU-06) Javier, Leonardo
│   │   │   ├── components/
│   │   │   ├── models/                 <-- (producto.model.ts, stock.model.ts)
│   │   │   ├── pages/
│   │   │   │   ├── productos/          <-- (HU-05) Javier
│   │   │   │   ├── categorias/         <-- (HU-05) Javier
│   │   │   │   └── stock/              <-- (HU-06) Leonardo
│   │   │   └── services/
│   │   │       ├── producto.service.ts
│   │   │       └── stock.service.ts
│   │   │
│   │   ├── app.config.ts
│   │   ├── app.routes.ts
│   │   └── app.ts
│   │
│   └── styles.scss


```

---

## 3. Configuración del Sidebar Dinámico

El Sidebar filtra sus ítems de navegación según los permisos (`modulo:pantalla:accion`) parseados del JWT o cargados desde el backend en `AuthService`.

| Módulo Visual Sidebar | Sub-menús (Pantallas) | Permiso Requerido (Backend)

 | Responsable |
| --- | --- | --- | --- |
| **Acceso & Seguridad** | Gestión de Usuarios | `ACCESO:USUARIOS:LECTURA`<br> | Alessandro (HU-02)

 |
|  | Roles y Permisos | `ACCESO:ROLES:LECTURA`<br> | Santi (HU-03)

 |
|  | Bitácora de Auditoría | `ACCESO:BITACORA:LECTURA`<br> | Santi (HU-03)

 |
| **Comercial** | Clientes | `COMERCIALL:CLIENTES:LECTURA` | Javier (HU-04)

 |
| **Inventario** | Productos y Categorías | `INVENTARIO:PRODUCTOS:LECTURA`<br> | Javier (HU-05)

 |
|  | Stock por Almacén | `INVENTARIO:STOCK:LECTURA` | Leonardo (HU-06)

 |

---

## 4. Implementación del Enrutamiento (`app.routes.ts`)

```typescript
import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { permissionGuard } from './core/guards/permission.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./pages/auth/login/login.component').then(m => m.LoginComponent)
  },
  {
    path: '',
    loadComponent: () => import('./pages/main-layout/main-layout.component').then(m => m.MainLayoutComponent),
    canActivate: [authGuard],
    children: [
      { path: '', redirectTo: 'inventario/productos', pathMatch: 'full' },
      
      // Módulo Acceso
      {
        path: 'acceso/usuarios',
        loadComponent: () => import('./modulo_acceso/pages/usuarios/usuarios.component').then(m => m.UsuariosComponent),
        canActivate: [permissionGuard],
        data: { permission: 'ACCESO:USUARIOS:LECTURA' }
      },
      {
        path: 'acceso/roles',
        loadComponent: () => import('./modulo_acceso/pages/roles/roles.component').then(m => m.RolesComponent),
        canActivate: [permissionGuard],
        data: { permission: 'ACCESO:ROLES:LECTURA' }
      },
      
      // Módulo Comercial
      {
        path: 'comercial/clientes',
        loadComponent: () => import('./modulo_comercial/pages/clientes/clientes.component').then(m => m.ClientesComponent),
        canActivate: [permissionGuard],
        data: { permission: 'COMERCIAL:CLIENTES:LECTURA' }
      },
      
      // Módulo Inventario
      {
        path: 'inventario/productos',
        loadComponent: () => import('./modulo_inventario/pages/productos/productos.component').then(m => m.ProductosComponent),
        canActivate: [permissionGuard],
        data: { permission: 'INVENTARIO:PRODUCTOS:LECTURA' }
      },
      {
        path: 'inventario/stock',
        loadComponent: () => import('./modulo_inventario/pages/stock/stock.component').then(m => m.StockComponent),
        canActivate: [permissionGuard],
        data: { permission: 'INVENTARIO:STOCK:LECTURA' }
      }
    ]
  },
  { path: '**', redirectTo: 'login' }
];

```

---

## 5. Implementación del Guard de Permisos RBAC (`permission.guard.ts`)

```typescript
import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const permissionGuard: CanActivateFn = (route) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const requiredPermission = route.data['permission'] as string;

  if (requiredPermission && authService.hasPermission(requiredPermission)) {
    return true;
  }

  router.navigate(['/unauthorized']);
  return false;
};

```

---

## 6. Reglas de Arquitectura Frontend para el Equipo

* **Simetría Backend/Frontend:** La carpeta de cada módulo (`modulo_acceso`, `modulo_comercial`, `modulo_inventario`) contiene exactamente las páginas y llamadas a API correspondientes a sus User Stories del backend.


* **Signals para Estado Local y Reactividad:** Priorizar `signal()` y `computed()` para estados de componentes y desacoplar la reactividad visual del ciclo de detección de cambios clásico.
* **Componentes Standalone Exclusivos:** Todos los componentes se importan directamente en sus rutas utilizando `loadComponent` para lazy loading nativo.
* **Manejo Estricto de DTOs:** Crear interfaces estrictas en `models/` que reflejen las respuestas JSON del backend sin mapear tipos `any`.