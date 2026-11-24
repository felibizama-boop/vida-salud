import { Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { EventMessage, EventType, InteractionStatus } from '@azure/msal-browser';
import { MsalBroadcastService, MsalService } from '@azure/msal-angular';
import { Subject, filter, takeUntil } from 'rxjs';
import { ROLE_LABELS, USER_ROLES, UserRole } from './core/models/role.model';
import { RoleService } from './core/services/role.service';

interface NavItem {
  label: string;
  path: string;
  icon: string;
  roles: UserRole[];
}

/** Navegación principal; cada ítem declara los roles que pueden verlo. */
const NAV_ITEMS: NavItem[] = [
  { label: 'Dashboard', path: '/dashboard', icon: '▦', roles: ['ADMIN', 'RECEPCIONISTA', 'PACIENTE', 'AUDITOR'] },
  { label: 'Atenciones', path: '/appointments', icon: '✚', roles: ['ADMIN', 'RECEPCIONISTA', 'PACIENTE'] },
  { label: 'Catálogo', path: '/catalog', icon: '❐', roles: ['ADMIN', 'RECEPCIONISTA'] },
  { label: 'Reportería', path: '/reports', icon: '▤', roles: ['ADMIN', 'AUDITOR'] },
  { label: 'Auditoría', path: '/audit', icon: '◔', roles: ['ADMIN', 'AUDITOR'] },
];

@Component({
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  selector: 'app-root',
  styleUrl: './app.scss',
  templateUrl: './app.html',
})
export class App implements OnInit, OnDestroy {
  private readonly msalService = inject(MsalService);
  private readonly msalBroadcastService = inject(MsalBroadcastService);
  private readonly destroying$ = new Subject<void>();

  protected readonly roleService = inject(RoleService);
  protected readonly roleLabels = ROLE_LABELS;
  protected readonly roles = USER_ROLES;

  protected readonly title = signal('VidaSalud');
  protected readonly interactionStatus = signal<InteractionStatus>(InteractionStatus.Startup);
  protected readonly isAuthenticated = signal(false);
  protected readonly userName = signal<string | null>(null);
  protected readonly ready = computed(() => this.interactionStatus() === InteractionStatus.None);

  /** Ítems de navegación visibles según el rol efectivo. */
  protected readonly navVisible = computed(() =>
    NAV_ITEMS.filter((item) => this.roleService.hasRole(item.roles))
  );

  ngOnInit(): void {
    this.msalService.handleRedirectObservable().subscribe();

    this.msalBroadcastService.inProgress$
      .pipe(takeUntil(this.destroying$))
      .subscribe((status: InteractionStatus) => this.interactionStatus.set(status));

    this.msalBroadcastService.inProgress$
      .pipe(
        filter((status: InteractionStatus) => status === InteractionStatus.None),
        takeUntil(this.destroying$)
      )
      .subscribe(() => this.updateAuthState());

    this.msalBroadcastService.msalSubject$
      .pipe(
        filter(
          (message: EventMessage) =>
            message.eventType === EventType.LOGIN_SUCCESS ||
            message.eventType === EventType.LOGOUT_SUCCESS
        ),
        takeUntil(this.destroying$)
      )
      .subscribe(() => this.updateAuthState());

    this.updateAuthState();
  }

  login(): void {
    this.msalService.loginRedirect().subscribe();
  }

  logout(): void {
    this.msalService.logoutRedirect().subscribe();
  }

  /** Cambia el rol de demostración con el que se renderiza la interfaz. */
  cambiarRol(event: Event): void {
    const valor = (event.target as HTMLSelectElement).value;
    this.roleService.setOverride(valor ? (valor as UserRole) : null);
  }

  ngOnDestroy(): void {
    this.destroying$.next();
    this.destroying$.complete();
  }

  private updateAuthState(): void {
    const activeAccount = this.msalService.instance.getActiveAccount();
    const account = activeAccount ?? this.msalService.instance.getAllAccounts()[0] ?? null;

    if (!activeAccount && account) {
      this.msalService.instance.setActiveAccount(account);
    }

    this.isAuthenticated.set(account !== null);
    this.userName.set(account?.name ?? account?.username ?? null);
    this.roleService.refreshFromToken();
  }
}
