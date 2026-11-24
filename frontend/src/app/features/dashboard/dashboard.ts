import { Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { fechaHoraAudit } from '../../core/models/audit.model';
import { ROLE_DESCRIPTIONS, ROLE_LABELS, UserRole } from '../../core/models/role.model';
import { AuditService } from '../../core/services/audit.service';
import { CatalogService } from '../../core/services/catalog.service';
import { MetricsService } from '../../core/services/metrics.service';
import { RoleService } from '../../core/services/role.service';

interface Acceso {
  label: string;
  path: string;
  descripcion: string;
  roles: UserRole[];
}

const ACCESOS: Acceso[] = [
  { label: 'Atenciones', path: '/appointments', descripcion: 'Agenda, estados y notificaciones', roles: ['ADMIN', 'RECEPCIONISTA', 'PACIENTE'] },
  { label: 'Catálogo', path: '/catalog', descripcion: 'Prestaciones, boxes y cupos', roles: ['ADMIN', 'RECEPCIONISTA'] },
  { label: 'Reportería', path: '/reports', descripcion: 'KPIs de demanda y tiempos', roles: ['ADMIN', 'AUDITOR'] },
  { label: 'Auditoría', path: '/audit', descripcion: 'Timeline de eventos y filtros', roles: ['ADMIN', 'AUDITOR'] },
];

@Component({
  imports: [RouterLink],
  selector: 'app-dashboard',
  templateUrl: './dashboard.html',
})
export class Dashboard {
  protected readonly roleService = inject(RoleService);
  private readonly metrics = inject(MetricsService);
  private readonly audit = inject(AuditService);
  private readonly catalog = inject(CatalogService);

  protected readonly roleLabels = ROLE_LABELS;
  protected readonly roleDescriptions = ROLE_DESCRIPTIONS;

  protected readonly kpi = computed(() => this.metrics.resumen());
  protected readonly auditoriaReciente = computed(() => this.audit.entradas().slice(0, 5));
  protected readonly prestacionesActivas = this.catalog.prestacionesActivas;
  protected readonly boxesActivos = this.catalog.boxesActivos;
  protected readonly fechaHora = fechaHoraAudit;

  protected readonly accesos = computed(() =>
    ACCESOS.filter((acceso) => this.roleService.hasRole(acceso.roles))
  );
}
