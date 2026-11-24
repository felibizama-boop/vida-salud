import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { environment } from '../../../environments/environment';
import {
  AUDIT_FILTRO_VACIO,
  AuditEntry,
  AuditFiltro,
  NuevoAuditEntry,
  fechaLocal,
} from '../models/audit.model';

/**
 * Timeline de auditoría del frontend.
 * <p>
 * Cada acción relevante (creación de atención, cambio de estado, gestión de
 * catálogo) se registra localmente y además se publica en el tópico Kafka
 * `audit.timeline` mediante el endpoint `/api/v1/events/audit`.
 */
@Injectable({ providedIn: 'root' })
export class AuditService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = environment.apiConfig.uri;

  private readonly estado = signal<AuditEntry[]>(this.semilla());
  private readonly filtro = signal<AuditFiltro>(AUDIT_FILTRO_VACIO);

  readonly entradas = this.estado.asReadonly();
  readonly filtroActual = this.filtro.asReadonly();

  /** Entidades y acciones presentes, para poblar los filtros. */
  readonly entidades = computed(() =>
    [...new Set(this.estado().map((e) => e.entityType))].sort()
  );

  readonly acciones = computed(() =>
    [...new Set(this.estado().map((e) => e.action))].sort()
  );

  /** Entradas del timeline que cumplen el filtro activo. */
  readonly filtradas = computed<AuditEntry[]>(() => {
    const f = this.filtro();
    const texto = f.texto.trim().toLowerCase();

    return this.estado().filter((entry) => {
      if (f.entityType && entry.entityType !== f.entityType) {
        return false;
      }
      if (f.action && entry.action !== f.action) {
        return false;
      }

      const fecha = fechaLocal(entry.occurredAt);
      if (f.desde && fecha < f.desde) {
        return false;
      }
      if (f.hasta && fecha > f.hasta) {
        return false;
      }

      if (texto) {
        const blob = `${entry.entityId} ${entry.action} ${entry.performedBy} ${entry.details}`.toLowerCase();
        if (!blob.includes(texto)) {
          return false;
        }
      }
      return true;
    });
  });

  readonly total = computed(() => this.estado().length);

  /**
   * Registra una entrada de auditoría localmente y la publica en Kafka.
   * La publicación al backend es tolerante a fallos: si el backend no está
   * disponible la entrada permanece visible en el timeline local.
   */
  registrar(entry: NuevoAuditEntry): AuditEntry {
    const completa: AuditEntry = {
      ...entry,
      id: crypto.randomUUID(),
      occurredAt: new Date().toISOString(),
      origin: 'FRONTEND',
    };

    this.estado.update((lista) => [completa, ...lista]);
    this.publicarEnKafka(entry);
    return completa;
  }

  actualizarFiltro(parcial: Partial<AuditFiltro>): void {
    this.filtro.update((actual) => ({ ...actual, ...parcial }));
  }

  limpiarFiltro(): void {
    this.filtro.set(AUDIT_FILTRO_VACIO);
  }

  private publicarEnKafka(entry: NuevoAuditEntry): void {
    this.http
      .post(`${this.baseUrl}events/audit`, {
        entityType: entry.entityType,
        entityId: entry.entityId,
        action: entry.action,
        performedBy: entry.performedBy,
        details: entry.details,
      })
      .subscribe({ error: () => undefined });
  }

  private semilla(): AuditEntry[] {
    const ahora = Date.now();
    const hace = (minutos: number) => new Date(ahora - minutos * 60_000).toISOString();

    return [
      {
        id: crypto.randomUUID(),
        entityType: 'APPOINTMENT',
        entityId: 'APT-1002',
        action: 'CONFIRMADA',
        performedBy: 'recepcion@vidasalud.cl',
        details: 'Confirmación telefónica del paciente',
        occurredAt: hace(25),
        origin: 'FRONTEND',
      },
      {
        id: crypto.randomUUID(),
        entityType: 'CATALOG',
        entityId: 'PREST-002',
        action: 'PRECIO_ACTUALIZADO',
        performedBy: 'admin@vidasalud.cl',
        details: 'Precio de Kinesiología respiratoria actualizado',
        occurredAt: hace(90),
        origin: 'FRONTEND',
      },
      {
        id: crypto.randomUUID(),
        entityType: 'APPOINTMENT',
        entityId: 'APT-1001',
        action: 'CANCELADA',
        performedBy: 'paciente@correo.cl',
        details: 'Cancelación solicitada por el paciente',
        occurredAt: hace(150),
        origin: 'KAFKA',
      },
    ];
  }
}
