import { Component, inject } from '@angular/core';
import { fechaHoraAudit } from '../../core/models/audit.model';
import { AuditService } from '../../core/services/audit.service';

@Component({
  selector: 'app-audit-timeline',
  templateUrl: './audit-timeline.html',
})
export class AuditTimeline {
  private readonly audit = inject(AuditService);

  protected readonly entradas = this.audit.filtradas;
  protected readonly entidades = this.audit.entidades;
  protected readonly acciones = this.audit.acciones;
  protected readonly filtro = this.audit.filtroActual;
  protected readonly total = this.audit.total;
  protected readonly fechaHora = fechaHoraAudit;

  protected cambiarTexto(valor: string): void {
    this.audit.actualizarFiltro({ texto: valor });
  }

  protected cambiarEntidad(valor: string): void {
    this.audit.actualizarFiltro({ entityType: valor });
  }

  protected cambiarAccion(valor: string): void {
    this.audit.actualizarFiltro({ action: valor });
  }

  protected cambiarDesde(valor: string): void {
    this.audit.actualizarFiltro({ desde: valor });
  }

  protected cambiarHasta(valor: string): void {
    this.audit.actualizarFiltro({ hasta: valor });
  }

  protected limpiar(): void {
    this.audit.limpiarFiltro();
  }
}
