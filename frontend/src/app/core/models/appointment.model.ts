/**
 * Modelo de dominio de las atenciones de VidaSalud.
 * El ciclo de vida se refleja en el estado y en el evento publicado en Kafka.
 */
export type EstadoAtencion =
  | 'SOLICITADA'
  | 'CONFIRMADA'
  | 'EN_ESPERA'
  | 'EN_ATENCION'
  | 'CERRADA'
  | 'CANCELADA';

export const ESTADOS_ATENCION: readonly EstadoAtencion[] = [
  'SOLICITADA',
  'CONFIRMADA',
  'EN_ESPERA',
  'EN_ATENCION',
  'CERRADA',
  'CANCELADA',
];

export const ESTADO_LABELS: Record<EstadoAtencion, string> = {
  SOLICITADA: 'Solicitada',
  CONFIRMADA: 'Confirmada',
  EN_ESPERA: 'En espera',
  EN_ATENCION: 'En atención',
  CERRADA: 'Cerrada',
  CANCELADA: 'Cancelada',
};

export type PrioridadAtencion = 'NORMAL' | 'ALTA' | 'URGENTE';

export const PRIORIDADES: readonly PrioridadAtencion[] = ['NORMAL', 'ALTA', 'URGENTE'];

export interface Atencion {
  id: string;
  patientId: string;
  paciente: string;
  rut: string;
  practitionerId: string;
  profesional: string;
  especialidad: string;
  prestacionId: string;
  prestacion: string;
  box: string;
  /** Fecha/hora agendada en formato ISO. */
  fecha: string;
  estado: EstadoAtencion;
  prioridad: PrioridadAtencion;
  observaciones: string;
  /** Marcas de tiempo usadas para calcular tiempos de espera. */
  enEsperaAt: string | null;
  enAtencionAt: string | null;
  /**
   * Correo del usuario que solicitó la atención. `null` cuando fue agendada por
   * el personal (permite distinguir las atenciones propias de un paciente).
   */
  solicitanteEmail: string | null;
  creadaAt: string;
  actualizadaAt: string;
}

export interface NuevaAtencion {
  paciente: string;
  rut: string;
  especialidad: string;
  prestacionId: string;
  profesional: string;
  box: string;
  fecha: string;
  prioridad: PrioridadAtencion;
  observaciones: string;
}

/**
 * Transiciones de estado permitidas. Evita estados imposibles y alimenta
 * los botones de acción disponibles en la vista de atenciones.
 */
export const TRANSICIONES_ESTADO: Record<EstadoAtencion, EstadoAtencion[]> = {
  SOLICITADA: ['CONFIRMADA', 'CANCELADA'],
  CONFIRMADA: ['EN_ESPERA', 'CANCELADA'],
  EN_ESPERA: ['EN_ATENCION', 'CANCELADA'],
  EN_ATENCION: ['CERRADA'],
  CERRADA: [],
  CANCELADA: [],
};

/** Tipo de evento de dominio publicado en `appointments.events` según el nuevo estado. */
export const ESTADO_EVENT_TYPE: Record<EstadoAtencion, string> = {
  SOLICITADA: 'CREATED',
  CONFIRMADA: 'UPDATED',
  EN_ESPERA: 'UPDATED',
  EN_ATENCION: 'UPDATED',
  CERRADA: 'COMPLETED',
  CANCELADA: 'CANCELLED',
};

export function fechaHoraLocal(iso: string): string {
  return new Date(iso).toLocaleString('es-CL', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}
