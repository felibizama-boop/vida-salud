import { Component, computed, inject } from '@angular/core';
import { formatoClp } from '../../core/models/catalog.model';
import { CatalogService } from '../../core/services/catalog.service';
import { MetricsService } from '../../core/services/metrics.service';

@Component({
  selector: 'app-reports',
  templateUrl: './reports.html',
})
export class Reports {
  private readonly metrics = inject(MetricsService);
  private readonly catalog = inject(CatalogService);

  protected readonly resumen = this.metrics.resumen;
  protected readonly clp = formatoClp;

  protected readonly cuposDisponibles = this.catalog.cuposDisponibles;
  protected readonly precioPromedio = this.catalog.precioPromedio;

  /** Máximo de atenciones por estado, para escalar las barras. */
  protected readonly maxEstado = computed(() =>
    Math.max(1, ...this.resumen().porEstado.map((e) => e.total))
  );

  /** Máximo de la demanda por prestación, para escalar las barras. */
  protected readonly maxDemanda = computed(() =>
    Math.max(1, ...this.resumen().prestacionesDemandadas.map((p) => p.total))
  );

  /** Máximo por especialidad, para escalar las barras. */
  protected readonly maxEspecialidad = computed(() =>
    Math.max(1, ...this.resumen().porEspecialidad.map((e) => e.total))
  );

  protected porcentaje(valor: number, maximo: number): number {
    return Math.round((valor / maximo) * 100);
  }
}
