import { Injectable, computed, inject } from '@angular/core';
import { ESTADO_LABELS, EstadoAtencion } from '../models/appointment.model';
import { AppointmentsService } from './appointments.service';

export interface SerieItem {
  nombre: string;
  total: number;
}

export interface BarraHora {
  hora: string;
  total: number;
  porcentaje: number;
}

export interface ResumenKpi {
  total: number;
  cerradas: number;
  canceladas: number;
  enCurso: number;
  esperaPromedioMin: number;
  porEstado: SerieItem[];
  porHora: BarraHora[];
  prestacionesDemandadas: SerieItem[];
  porEspecialidad: SerieItem[];
  tasaCancelacion: number;
}

/**
 * Métricas y KPIs calculados a partir de las atenciones.
 * Se exponen como señales derivadas para que reportería y dashboard se
 * actualicen automáticamente ante cualquier cambio en la agenda.
 */
@Injectable({ providedIn: 'root' })
export class MetricsService {
  private readonly appointments = inject(AppointmentsService);

  readonly resumen = computed<ResumenKpi>(() => {
    const atenciones = this.appointments.atenciones();
    const total = atenciones.length;

    const porEstado = (Object.keys(ESTADO_LABELS) as EstadoAtencion[]).map((estado) => ({
      nombre: ESTADO_LABELS[estado],
      total: atenciones.filter((a) => a.estado === estado).length,
    }));

    const conteoHoras = new Map<number, number>();
    for (const atencion of atenciones) {
      const hora = new Date(atencion.fecha).getHours();
      conteoHoras.set(hora, (conteoHoras.get(hora) ?? 0) + 1);
    }
    const maxHora = Math.max(1, ...conteoHoras.values());
    const porHora: BarraHora[] = [...conteoHoras.entries()]
      .sort((a, b) => a[0] - b[0])
      .map(([hora, cantidad]) => ({
        hora: `${String(hora).padStart(2, '0')}:00`,
        total: cantidad,
        porcentaje: Math.round((cantidad / maxHora) * 100),
      }));

    const demandadas = new Map<string, number>();
    for (const atencion of atenciones) {
      demandadas.set(atencion.prestacionId, (demandadas.get(atencion.prestacionId) ?? 0) + 1);
    }
    const prestacionesDemandadas: SerieItem[] = [...demandadas.entries()]
      .map(([nombre, cantidad]) => ({ nombre, total: cantidad }))
      .sort((a, b) => b.total - a.total);

    const especialidades = new Map<string, number>();
    for (const atencion of atenciones) {
      especialidades.set(atencion.especialidad, (especialidades.get(atencion.especialidad) ?? 0) + 1);
    }
    const porEspecialidad: SerieItem[] = [...especialidades.entries()]
      .map(([nombre, cantidad]) => ({ nombre, total: cantidad }))
      .sort((a, b) => b.total - a.total);

    const esperas = atenciones
      .filter((a) => a.enEsperaAt && a.enAtencionAt)
      .map(
        (a) =>
          (new Date(a.enAtencionAt as string).getTime() - new Date(a.enEsperaAt as string).getTime()) /
          60_000
      );
    const esperaPromedioMin =
      esperas.length > 0 ? Math.round(esperas.reduce((s, v) => s + v, 0) / esperas.length) : 0;

    const canceladas = atenciones.filter((a) => a.estado === 'CANCELADA').length;
    const cerradas = atenciones.filter((a) => a.estado === 'CERRADA').length;
    const enCurso = atenciones.filter(
      (a) => a.estado === 'EN_ESPERA' || a.estado === 'EN_ATENCION'
    ).length;

    return {
      total,
      cerradas,
      canceladas,
      enCurso,
      esperaPromedioMin,
      porEstado,
      porHora,
      prestacionesDemandadas,
      porEspecialidad,
      tasaCancelacion: total > 0 ? Math.round((canceladas / total) * 100) : 0,
    };
  });
}
