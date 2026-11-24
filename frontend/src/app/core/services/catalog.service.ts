import { Injectable, computed, inject, signal } from '@angular/core';
import {
  Box,
  Cupo,
  NuevoBox,
  NuevoCupo,
  NuevaPrestacion,
  Prestacion,
} from '../models/catalog.model';
import { AuditService } from './audit.service';

/**
 * Catálogo clínico: prestaciones, boxes y cupos.
 * Mantiene el estado en memoria y registra en auditoría cada modificación.
 */
@Injectable({ providedIn: 'root' })
export class CatalogService {
  private readonly audit = inject(AuditService);

  private readonly prestacionesEstado = signal<Prestacion[]>(this.semillaPrestaciones());
  private readonly boxesEstado = signal<Box[]>(this.semillaBoxes());
  private readonly cuposEstado = signal<Cupo[]>(this.semillaCupos());
  private readonly profesionalesEstado = signal<string[]>(this.semillaProfesionales());

  readonly prestaciones = this.prestacionesEstado.asReadonly();
  readonly boxes = this.boxesEstado.asReadonly();
  readonly cupos = this.cuposEstado.asReadonly();
  /** Profesionales disponibles para asignar a cupos y atenciones. */
  readonly profesionales = this.profesionalesEstado.asReadonly();

  readonly prestacionesActivas = computed(
    () => this.prestacionesEstado().filter((p) => p.activa).length
  );

  readonly boxesActivos = computed(() => this.boxesEstado().filter((b) => b.activo).length);

  readonly cuposDisponibles = computed(() =>
    this.cuposEstado().reduce((total, cupo) => total + (cupo.capacidad - cupo.reservados), 0)
  );

  readonly precioPromedio = computed(() => {
    const activas = this.prestacionesEstado().filter((p) => p.activa);
    if (activas.length === 0) {
      return 0;
    }
    return Math.round(activas.reduce((suma, p) => suma + p.precio, 0) / activas.length);
  });

  agregarPrestacion(dto: NuevaPrestacion): void {
    const prestacion: Prestacion = {
      ...dto,
      id: `PREST-${Math.floor(Math.random() * 900) + 100}`,
      activa: true,
    };
    this.prestacionesEstado.update((lista) => [prestacion, ...lista]);
    this.audit.registrar({
      entityType: 'CATALOG',
      entityId: prestacion.id,
      action: 'PRESTACION_CREADA',
      performedBy: 'admin@vidasalud.cl',
      details: `Prestación ${prestacion.nombre} creada`,
    });
  }

  actualizarPrecio(id: string, precio: number): void {
    this.prestacionesEstado.update((lista) =>
      lista.map((p) => (p.id === id ? { ...p, precio } : p))
    );
    this.audit.registrar({
      entityType: 'CATALOG',
      entityId: id,
      action: 'PRECIO_ACTUALIZADO',
      performedBy: 'admin@vidasalud.cl',
      details: `Precio actualizado a ${precio}`,
    });
  }

  alternarPrestacion(id: string): void {
    this.prestacionesEstado.update((lista) =>
      lista.map((p) => (p.id === id ? { ...p, activa: !p.activa } : p))
    );
  }

  /** Elimina una prestación y, en cascada, sus cupos asociados. */
  eliminarPrestacion(id: string): void {
    const prestacion = this.prestacionesEstado().find((p) => p.id === id);
    if (!prestacion) {
      return;
    }
    this.prestacionesEstado.update((lista) => lista.filter((p) => p.id !== id));
    this.cuposEstado.update((lista) => lista.filter((c) => c.prestacionId !== id));
    this.audit.registrar({
      entityType: 'CATALOG',
      entityId: id,
      action: 'PRESTACION_ELIMINADA',
      performedBy: 'admin@vidasalud.cl',
      details: `Prestación ${prestacion.nombre} eliminada del catálogo`,
    });
  }

  agregarBox(dto: NuevoBox): void {
    const box: Box = {
      ...dto,
      id: `BOX-${Math.floor(Math.random() * 900) + 100}`,
      activo: true,
    };
    this.boxesEstado.update((lista) => [box, ...lista]);
    this.audit.registrar({
      entityType: 'CATALOG',
      entityId: box.id,
      action: 'BOX_CREADO',
      performedBy: 'admin@vidasalud.cl',
      details: `Box ${box.nombre} creado en ${box.ubicacion}`,
    });
  }

  alternarBox(id: string): void {
    this.boxesEstado.update((lista) =>
      lista.map((b) => (b.id === id ? { ...b, activo: !b.activo } : b))
    );
  }

  eliminarBox(id: string): void {
    const box = this.boxesEstado().find((b) => b.id === id);
    if (!box) {
      return;
    }
    this.boxesEstado.update((lista) => lista.filter((b) => b.id !== id));
    this.audit.registrar({
      entityType: 'CATALOG',
      entityId: id,
      action: 'BOX_ELIMINADO',
      performedBy: 'admin@vidasalud.cl',
      details: `Box ${box.nombre} eliminado de ${box.ubicacion}`,
    });
  }

  agregarCupo(dto: NuevoCupo): void {
    const cupo: Cupo = { ...dto, id: `CUPO-${Math.floor(Math.random() * 900) + 100}`, reservados: 0 };
    this.cuposEstado.update((lista) => [cupo, ...lista]);
    this.audit.registrar({
      entityType: 'CATALOG',
      entityId: cupo.id,
      action: 'CUPO_CREADO',
      performedBy: 'admin@vidasalud.cl',
      details: `Cupo ${cupo.dia} ${cupo.hora} con ${cupo.capacidad} espacios`,
    });
  }

  reservarCupo(id: string): void {
    this.cuposEstado.update((lista) =>
      lista.map((c) =>
        c.id === id && c.reservados < c.capacidad ? { ...c, reservados: c.reservados + 1 } : c
      )
    );
  }

  eliminarCupo(id: string): void {
    const cupo = this.cuposEstado().find((c) => c.id === id);
    if (!cupo) {
      return;
    }
    this.cuposEstado.update((lista) => lista.filter((c) => c.id !== id));
    this.audit.registrar({
      entityType: 'CATALOG',
      entityId: id,
      action: 'CUPO_ELIMINADO',
      performedBy: 'admin@vidasalud.cl',
      details: `Cupo ${cupo.dia} ${cupo.hora} eliminado`,
    });
  }

  private semillaPrestaciones(): Prestacion[] {
    return [
      { id: 'CONS-MG', nombre: 'Consulta Medicina General', categoria: 'Consulta', duracionMin: 20, precio: 25000, activa: true },
      { id: 'KIN-01', nombre: 'Kinesiología respiratoria', categoria: 'Kinesiología', duracionMin: 30, precio: 32000, activa: true },
      { id: 'ODO-02', nombre: 'Odontología general', categoria: 'Odontología', duracionMin: 40, precio: 45000, activa: true },
      { id: 'EXA-HEM', nombre: 'Hemograma completo', categoria: 'Examen', duracionMin: 15, precio: 18000, activa: true },
      { id: 'PRO-ECG', nombre: 'Electrocardiograma', categoria: 'Procedimiento', duracionMin: 25, precio: 38000, activa: true },
      { id: 'CONS-NUT', nombre: 'Consulta nutricional', categoria: 'Consulta', duracionMin: 30, precio: 28000, activa: false },
    ];
  }

  private semillaBoxes(): Box[] {
    return [
      { id: 'BOX-1', nombre: 'Box 1', ubicacion: 'Piso 1 - Ala Norte', tipo: 'Consulta', activo: true },
      { id: 'BOX-2', nombre: 'Box 2', ubicacion: 'Piso 1 - Ala Norte', tipo: 'Consulta', activo: true },
      { id: 'BOX-3', nombre: 'Box 3', ubicacion: 'Piso 1 - Ala Sur', tipo: 'Procedimiento', activo: true },
      { id: 'BOX-4', nombre: 'Box 4', ubicacion: 'Piso 2 - Kinesiología', tipo: 'Kinesiología', activo: true },
      { id: 'BOX-6', nombre: 'Box 6', ubicacion: 'Piso 1 - Toma de muestras', tipo: 'Toma de muestras', activo: true },
    ];
  }

  private semillaCupos(): Cupo[] {
    return [
      { id: 'CUPO-1', prestacionId: 'CONS-MG', profesional: 'Dra. Paula Rojas', dia: 'Lunes', hora: '09:00', capacidad: 8, reservados: 5 },
      { id: 'CUPO-2', prestacionId: 'CONS-MG', profesional: 'Dra. Paula Rojas', dia: 'Martes', hora: '10:00', capacidad: 8, reservados: 8 },
      { id: 'CUPO-3', prestacionId: 'KIN-01', profesional: 'Klgo. Iván Pérez', dia: 'Miércoles', hora: '11:00', capacidad: 6, reservados: 2 },
      { id: 'CUPO-4', prestacionId: 'ODO-02', profesional: 'Dr. Luis Tapia', dia: 'Jueves', hora: '15:00', capacidad: 5, reservados: 3 },
      { id: 'CUPO-5', prestacionId: 'EXA-HEM', profesional: 'Tec. Sara Núñez', dia: 'Viernes', hora: '08:30', capacidad: 12, reservados: 7 },
    ];
  }

  private semillaProfesionales(): string[] {
    return [
      'Dra. Paula Rojas',
      'Klgo. Iván Pérez',
      'Dr. Luis Tapia',
      'Tec. Sara Núñez',
      'Dr. Marco Silva',
    ];
  }
}
