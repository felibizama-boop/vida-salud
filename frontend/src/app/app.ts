import { Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { EventMessage, EventType, InteractionStatus } from '@azure/msal-browser';
import { MsalBroadcastService, MsalService } from '@azure/msal-angular';
import { Subject, filter, takeUntil } from 'rxjs';

@Component({
  imports: [RouterOutlet],
  selector: 'app-root',
  styleUrl: './app.scss',
  templateUrl: './app.html',
})
export class App implements OnInit, OnDestroy {
  private readonly msalService = inject(MsalService);
  private readonly msalBroadcastService = inject(MsalBroadcastService);
  private readonly destroying$ = new Subject<void>();

  protected readonly title = signal('VidaSalud');
  protected readonly interactionStatus = signal<InteractionStatus>(InteractionStatus.Startup);
  protected readonly isAuthenticated = signal(false);
  protected readonly userName = signal<string | null>(null);
  protected readonly ready = computed(() => this.interactionStatus() === InteractionStatus.None);

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
  }
}
