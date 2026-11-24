import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { UserRole } from '../models/role.model';
import { RoleService } from '../services/role.service';

/**
 * Guard funcional de autorización por rol.
 * <p>
 * Lee los roles permitidos desde `data.roles` de la ruta y, si el rol efectivo
 * del usuario no está autorizado, redirige al dashboard.
 */
export const roleGuard: CanActivateFn = (route) => {
  const roleService = inject(RoleService);
  const router = inject(Router);

  const permitidos = (route.data['roles'] as UserRole[] | undefined) ?? [];
  if (permitidos.length === 0 || roleService.hasRole(permitidos)) {
    return true;
  }

  return router.createUrlTree(['/dashboard']);
};
