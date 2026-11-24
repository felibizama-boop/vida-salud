import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { environment } from '../../../environments/environment';
import {
  Atencion,
  ESTADO_EVENT_TYPE,
  EstadoAtencion,
  NuevaAtencion,
  TRANSICIONES_ESTADO,
} from '../models/appointment.model';
import { AuditService } from './audit.service';

export type BitacoraEstado = 'ENVIANDO' | 'OK' | 'ERROR';

export interface BitacoraItem {
  id: string;
  fecha: string;
  etiqueta: string;
  endpoint: string;
  estado: BitacoraEstado;
  detalle: string;
}

/**
 * Servicio de atenciones de VidaSalud.
 * <p>
 * Mantiene el estado de la agenda en memoria y sincroniza cada cambio con el
 * backend: publica eventos de dominio en `appointments.events` y notificaciones
 * en la topología RabbitMQ. El resultado de cada llamada queda en la bitácora
 * de integración que se muestra en la vista de atenciones.
 */
@Injectable({ providedIn: 'root' })
export class AppointmentsService {
  private readonly http = inject(HttpClient);
  private readonly audit = inject(AuditService);
  private readonly baseUrl = environment.apiConfig.uri;

  private readonly estado = signal<Atencion[]>(this.semilla());
  private readonly bitacoraEstado = signal<BitacoraItem[]>([]);

  readonly atenciones = this.estado.asReadonly();
  readonly bitacora = this.bitacoraEstado.asReadonly();

  readonly total = computed(() => this.estado().length);

  /** Conteo de atenciones por estado, usado por el dashboard y la reportería. */
  readonly porEstado = computed<Record<EstadoAtencion, number>>(() => {
    const base: Record<EstadoAtencion, number> = {
      SOLICITADA: 0,
      CONFIRMADA: 0,
      EN_ESPERA: 0,
      EN_ATENCION: 0,
      CERRADA: 0,
      CANCELADA: 0,
    };
    for (const atencion of this.estado()) {
      base[atencion.estado] += 1;
    }
    return base;
  });

  obtener(id: string): Atencion | undefined {
    return this.estado().find((a) => a.id === id);
  }

  /** Crea una atención en estado SOLICITADA y publica el evento CREATED. */
  crear(dto: NuevaAtencion, solicitanteEmail: string | null = null): Atencion {
    const ahora = new Date().toISOString();
    const atencion: Atencion = {
      id: this.generarId(),
      patientId: `PAT-${Math.floor(Math.random() * 9000) + 1000}`,
      paciente: dto.paciente,
      rut: dto.rut,
      practitionerId: `PRO-${Math.floor(Math.random() * 900) + 100}`,
      profesional: dto.profesional,
      especialidad: dto.especialidad,
      prestacionId: dto.prestacionId,
      prestacion: dto.prestacionId,
      box: dto.box,
      fecha: new Date(dto.fecha).toISOString(),
      estado: 'SOLICITADA',
      prioridad: dto.prioridad,
      observaciones: dto.observaciones,
      enEsperaAt: null,
      enAtencionAt: null,
      solicitanteEmail,
      creadaAt: ahora,
      actualizadaAt: ahora,
    };

    this.estado.update((lista) => [atencion, ...lista]);

    this.publicarEvento(atencion, ESTADO_EVENT_TYPE[atencion.estado]);
    this.audit.registrar({
      entityType: 'APPOINTMENT',
      entityId: atencion.id,
      action: 'CREADA',
      performedBy: atencion.paciente,
      details: `Atención solicitada para ${atencion.especialidad}`,
    });

    return atencion;
  }

  /** Cambia el estado de una atención respetando las transiciones permitidas. */
  cambiarEstado(id: string, nuevoEstado: EstadoAtencion): boolean {
    const actual = this.obtener(id);
    if (!actual || !TRANSICIONES_ESTADO[actual.estado].includes(nuevoEstado)) {
      return false;
    }

    const ahora = new Date().toISOString();
    const actualizada: Atencion = {
      ...actual,
      estado: nuevoEstado,
      actualizadaAt: ahora,
      enEsperaAt: nuevoEstado === 'EN_ESPERA' ? ahora : actual.enEsperaAt,
      enAtencionAt: nuevoEstado === 'EN_ATENCION' ? ahora : actual.enAtencionAt,
    };

    this.estado.update((lista) => lista.map((a) => (a.id === id ? actualizada : a)));

    this.publicarEvento(actualizada, ESTADO_EVENT_TYPE[nuevoEstado]);
    this.audit.registrar({
      entityType: 'APPOINTMENT',
      entityId: actualizada.id,
      action: nuevoEstado,
      performedBy: actualizada.paciente,
      details: `Estado actualizado a ${nuevoEstado}`,
    });

    if (nuevoEstado === 'CONFIRMADA' || nuevoEstado === 'CANCELADA') {
      this.enviarNotificacion(
        actualizada,
        `Atención ${actualizada.id} en estado ${nuevoEstado}`
      );
    }

    return true;
  }

  /** Dispara manualmente una notificación por RabbitMQ para la atención indicada. */
  notificar(id: string, mensaje?: string): void {
    const atencion = this.obtener(id);
    if (!atencion) {
      return;
    }
    this.enviarNotificacion(
      atencion,
      mensaje ?? `Recordatorio de atención ${atencion.id} (${atencion.especialidad})`
    );
  }

  /**
   * Elimina una atención del frontend y publica el evento CANCELLED para
   * mantener el backend sincronizado con la anulación del registro.
   */
  eliminar(id: string): boolean {
    const actual = this.obtener(id);
    if (!actual) {
      return false;
    }

    this.estado.update((lista) => lista.filter((a) => a.id !== id));

    this.publicarEvento(actual, 'CANCELLED');
    this.audit.registrar({
      entityType: 'APPOINTMENT',
      entityId: id,
      action: 'ELIMINADA',
      performedBy: actual.paciente,
      details: `Atención ${id} eliminada de la agenda`,
    });

    return true;
  }

  limpiarBitacora(): void {
    this.bitacoraEstado.set([]);
  }

  private publicarEvento(atencion: Atencion, eventType: string): void {
    const payload = {
      appointmentId: atencion.id,
      patientId: atencion.patientId,
      practitionerId: atencion.practitionerId,
      eventType,
      status: atencion.estado,
      scheduledAt: atencion.fecha,
      source: 'frontend-vidasalud',
    };

    this.enviar(
      'Evento de cita',
      `POST ${this.baseUrl}events/appointments`,
      `${this.baseUrl}events/appointments`,
      payload
    );
  }

  private enviarNotificacion(atencion: Atencion, mensaje: string): void {
    const payload = {
      type: 'NOTIFICACION',
      recipient: atencion.paciente,
      message: mensaje,
      simulateFailure: false,
    };

    this.enviar(
      'Notificación',
      `POST ${this.baseUrl}notifications`,
      `${this.baseUrl}notifications`,
      payload
    );
  }

  /**
   * Ejecuta la llamada HTTP al backend y registra el resultado en la bitácora.
   * La suscripción es intencionalmente "fire and forget": el estado local no
   * depende del backend para mantener la interfaz operativa en la demo.
   */
  private enviar(etiqueta: string, descripcion: string, url: string, body: unknown): void {
    const id = crypto.randomUUID();
    const item: BitacoraItem = {
      id,
      fecha: new Date().toISOString(),
      etiqueta,
      endpoint: descripcion,
      estado: 'ENVIANDO',
      detalle: 'Enviando...',
    };
    this.bitacoraEstado.update((lista) => [item, ...lista].slice(0, 12));

    this.http.post(url, body, { responseType: 'text' }).subscribe({
      next: (respuesta) => this.actualizarBitacora(id, 'OK', respuesta || 'Aceptado (202)'),
      error: (error: { status?: number; message?: string }) =>
        this.actualizarBitacora(
          id,
          'ERROR',
          error.status ? `HTTP ${error.status}` : (error.message ?? 'Error de conexión')
        ),
    });
  }

  private actualizarBitacora(id: string, estado: BitacoraEstado, detalle: string): void {
    this.bitacoraEstado.update((lista) =>
      lista.map((item) => (item.id === id ? { ...item, estado, detalle } : item))
    );
  }

  private generarId(): string {
    return `APT-${Math.floor(Math.random() * 9000) + 1000}`;
  }

  private semilla(): Atencion[] {
    const hoy = new Date();
    const aHora = (hora: number, minuto = 0) => {
      const d = new Date(hoy);
      d.setHours(hora, minuto, 0, 0);
      return d.toISOString();
    };

    const base: Partial<Atencion> = {
      enEsperaAt: null,
      enAtencionAt: null,
      solicitanteEmail: null,
    };

    const crear = (
      id: string,
      paciente: string,
      rut: string,
      especialidad: string,
      prestacionId: string,
      profesional: string,
      box: string,
      hora: number,
      estado: EstadoAtencion,
      prioridad: Atencion['prioridad'],
      esperaMin?: number
    ): Atencion => {
      const fecha = aHora(hora);
      const enEspera = esperaMin !== undefined ? aHora(hora) : null;
      const enAtencion =
        esperaMin !== undefined ? new Date(new Date(fecha).getTime() + esperaMin * 60_000).toISOString() : null;

      return {
        ...base,
        id,
        patientId: `PAT-${id.slice(-4)}`,
        paciente,
        rut,
        practitionerId: `PRO-${id.slice(-3)}`,
        profesional,
        especialidad,
        prestacionId,
        prestacion: prestacionId,
        box,
        fecha,
        estado,
        prioridad,
        observaciones: '',
        enEsperaAt: enEspera,
        enAtencionAt: enAtencion,
        creadaAt: fecha,
        actualizadaAt: fecha,
      } as Atencion;
    };

    return [
      crear('APT-1001', 'María González', '12.345.678-9', 'Medicina General', 'CONS-MG', 'Dra. Paula Rojas', 'Box 1', 8, 'CERRADA', 'NORMAL', 12),
      crear('APT-1002', 'Pedro Soto', '13.456.789-0', 'Kinesiología', 'KIN-01', 'Klgo. Iván Pérez', 'Box 4', 9, 'CERRADA', 'NORMAL', 18),
      crear('APT-1003', 'Ana Muñoz', '14.567.890-1', 'Odontología', 'ODO-02', 'Dr. Luis Tapia', 'Box 2', 9, 'CERRADA', 'ALTA', 8),
      crear('APT-1004', 'Jorge Díaz', '15.678.901-2', 'Medicina General', 'CONS-MG', 'Dra. Paula Rojas', 'Box 1', 10, 'EN_ATENCION', 'NORMAL', 22),
      crear('APT-1005', 'Carla Fuentes', '16.789.012-3', 'Examen', 'EXA-HEM', 'Tec. Sara Núñez', 'Box 6', 10, 'EN_ESPERA', 'ALTA'),
      crear('APT-1006', 'Diego Vera', '17.890.123-4', 'Odontología', 'ODO-02', 'Dr. Luis Tapia', 'Box 2', 11, 'CONFIRMADA', 'NORMAL'),
      crear('APT-1007', 'Paula Herrera', '18.901.234-5', 'Kinesiología', 'KIN-01', 'Klgo. Iván Pérez', 'Box 4', 11, 'CONFIRMADA', 'NORMAL'),
      crear('APT-1008', 'Andrés Molina', '19.012.345-6', 'Procedimiento', 'PRO-ECG', 'Dr. Marco Silva', 'Box 3', 12, 'SOLICITADA', 'URGENTE'),
      crear('APT-1009', 'Valentina Ríos', '20.123.456-7', 'Medicina General', 'CONS-MG', 'Dra. Paula Rojas', 'Box 1', 12, 'SOLICITADA', 'NORMAL'),
      crear('APT-1010', 'Sebastián Castro', '11.234.567-8', 'Examen', 'EXA-HEM', 'Tec. Sara Núñez', 'Box 6', 15, 'CANCELADA', 'NORMAL'),
      crear('APT-1011', 'Isidora Núñez', '10.345.678-9', 'Odontología', 'ODO-02', 'Dr. Luis Tapia', 'Box 2', 16, 'EN_ESPERA', 'NORMAL'),
      crear('APT-1012', 'Tomás Aguilar', '21.456.789-0', 'Medicina General', 'CONS-MG', 'Dra. Paula Rojas', 'Box 1', 17, 'CONFIRMADA', 'ALTA'),
    ];
  }
}
