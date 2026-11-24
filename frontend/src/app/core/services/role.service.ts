import { Injectable, computed, inject, signal } from '@angular/core';
import { MsalService } from '@azure/msal-angular';
import { UserRole, isUserRole } from '../models/role.model';

/**
 * Resuelve el rol del usuario autenticado.
 * <p>
 * Primero intenta leer el claim `roles` del token de Azure Entra ID. Si el
 * registro de aplicación no asigna roles, permite un override manual que solo
 * se utiliza para demostrar la adaptación de las vistas por rol.
 */
@Injectable({ providedIn: 'root' })
export class RoleService {
  private readonly msal = inject(MsalService);

  private readonly rolesDelToken = signal<UserRole[]>([]);
  private readonly overrideRol = signal<UserRole | null>(null);
  private readonly usuarioEmail = signal<string>('demo@vidasalud.cl');

  /** Rol efectivo con el que se renderiza la navegación y se autorizan rutas. */
  readonly currentRole = computed<UserRole>(
    () => this.overrideRol() ?? this.rolesDelToken()[0] ?? 'ADMIN'
  );

  /** Correo (o nombre) del usuario autenticado, usado para validar la propiedad de los datos. */
  readonly usuarioActual = this.usuarioEmail.asReadonly();

  /** Indica si el rol proviene del selector de demostración. */
  readonly esOverride = computed(() => this.overrideRol() !== null);

  /** Roles disponibles en el token (para mostrarlos en la interfaz). */
  readonly rolesDisponibles = computed(() => this.rolesDelToken());

  /** Relee los roles desde la cuenta activa de MSAL. */
  refreshFromToken(): void {
    this.rolesDelToken.set(this.leerRolesDelToken());
    this.usuarioEmail.set(this.leerUsuario());
  }

  /** Establece un rol de demostración; `null` vuelve al rol del token. */
  setOverride(role: UserRole | null): void {
    this.overrideRol.set(role);
  }

  hasRole(permitidos: readonly UserRole[]): boolean {
    return permitidos.includes(this.currentRole());
  }

  /** Identidad del usuario autenticado (correo o nombre de la cuenta activa). */
  private leerUsuario(): string {
    const cuenta = this.msal.instance.getActiveAccount();
    return cuenta?.username ?? cuenta?.name ?? 'demo@vidasalud.cl';
  }

  private leerRolesDelToken(): UserRole[] {
    const cuenta = this.msal.instance.getActiveAccount();
    const claims = cuenta?.idTokenClaims as Record<string, unknown> | undefined;
    const roles = claims ? claims['roles'] : undefined;

    if (!Array.isArray(roles)) {
      return [];
    }

    return roles
      .filter((rol): rol is string => typeof rol === 'string')
      .filter(isUserRole);
  }
}
