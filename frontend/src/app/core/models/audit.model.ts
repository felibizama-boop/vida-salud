/**
 * Entrada del timeline de auditoría.
 * Se nutre de las acciones ejecutadas en el frontend y de los eventos
 * de auditoría publicados en el tópico `audit.timeline`.
 */
export type AuditOrigin = 'FRONTEND' | 'KAFKA';

export interface AuditEntry {
  id: string;
  entityType: string;
  entityId: string;
  action: string;
  performedBy: string;
  details: string;
  occurredAt: string;
  origin: AuditOrigin;
}

export interface NuevoAuditEntry {
  entityType: string;
  entityId: string;
  action: string;
  performedBy: string;
  details: string;
}

export interface AuditFiltro {
  texto: string;
  entityType: string;
  action: string;
  desde: string;
  hasta: string;
}

export const AUDIT_FILTRO_VACIO: AuditFiltro = {
  texto: '',
  entityType: '',
  action: '',
  desde: '',
  hasta: '',
};

/** Fecha local `YYYY-MM-DD` usada para filtrar por rango. */
export function fechaLocal(iso: string): string {
  const d = new Date(iso);
  const mes = String(d.getMonth() + 1).padStart(2, '0');
  const dia = String(d.getDate()).padStart(2, '0');
  return `${d.getFullYear()}-${mes}-${dia}`;
}

export function fechaHoraAudit(iso: string): string {
  return new Date(iso).toLocaleString('es-CL', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  });
}
