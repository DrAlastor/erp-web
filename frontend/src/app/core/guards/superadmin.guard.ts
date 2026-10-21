import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { SuperAdminService } from '../services/superadmin.service';

export const superAdminGuard: CanActivateFn = () => {
  const superAdminService = inject(SuperAdminService);
  const router = inject(Router);

  if (superAdminService.isAuthenticated()) {
    return true;
  }
  return router.createUrlTree(['/superadmin/login']);
};
