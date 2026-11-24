/**
 * Roles funcionales de VidaSalud.
 * Se resuelven a partir del claim `roles` del token de Azure Entra ID
 * y pueden sobreescribirse manualmente solo para fines de demostración.
 */
export type UserRole = 'ADMIN' | 'RECEPCIONISTA' | 'PACIENTE' | 'AUDITOR';

export const USER_ROLES: readonly UserRole[] = [
  'ADMIN',
  'RECEPCIONISTA',
  'PACIENTE',
  'AUDITOR',
];

export const ROLE_LABELS: Record<UserRole, string> = {
  ADMIN: 'Administrador',
  RECEPCIONISTA: 'Recepcionista',
  PACIENTE: 'Paciente',
  AUDITOR: 'Auditor',
};

export const ROLE_DESCRIPTIONS: Record<UserRole, string> = {
  ADMIN: 'Acceso total: atenciones, catálogo, reportería y auditoría.',
  RECEPCIONISTA: 'Gestiona atenciones y el catálogo de prestaciones.',
  PACIENTE: 'Solicita y sigue sus propias atenciones.',
  AUDITOR: 'Consulta reportería y el timeline de auditoría.',
};

/** Verifica en tiempo de compilación/ejecución si un string es un rol válido. */
export function isUserRole(value: string): value is UserRole {
  return (USER_ROLES as readonly string[]).includes(value);
}
