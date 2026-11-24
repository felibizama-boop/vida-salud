import { Routes } from '@angular/router';
import { MsalGuard } from '@azure/msal-angular';
import { roleGuard } from './core/guards/role.guard';
import { UserRole } from './core/models/role.model';

const TODOS: UserRole[] = ['ADMIN', 'RECEPCIONISTA', 'PACIENTE', 'AUDITOR'];

export const routes: Routes = [
  {
    path: 'dashboard',
    canActivate: [MsalGuard, roleGuard],
    data: { roles: TODOS },
    loadComponent: () => import('./features/dashboard/dashboard').then((m) => m.Dashboard),
  },
  {
    path: 'appointments',
    canActivate: [MsalGuard, roleGuard],
    data: { roles: ['ADMIN', 'RECEPCIONISTA', 'PACIENTE'] },
    loadComponent: () =>
      import('./features/appointments/appointments').then((m) => m.Appointments),
  },
  {
    path: 'catalog',
    canActivate: [MsalGuard, roleGuard],
    data: { roles: ['ADMIN', 'RECEPCIONISTA'] },
    loadComponent: () => import('./features/catalog/catalog').then((m) => m.Catalog),
  },
  {
    path: 'reports',
    canActivate: [MsalGuard, roleGuard],
    data: { roles: ['ADMIN', 'AUDITOR'] },
    loadComponent: () => import('./features/reports/reports').then((m) => m.Reports),
  },
  {
    path: 'audit',
    canActivate: [MsalGuard, roleGuard],
    data: { roles: ['ADMIN', 'AUDITOR'] },
    loadComponent: () =>
      import('./features/audit/audit-timeline').then((m) => m.AuditTimeline),
  },
  {
    path: '',
    redirectTo: 'dashboard',
    pathMatch: 'full',
  },
  {
    path: '**',
    redirectTo: 'dashboard',
  },
];
