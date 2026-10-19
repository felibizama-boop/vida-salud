import { TestBed } from '@angular/core/testing';
import { InteractionStatus } from '@azure/msal-browser';
import { MsalBroadcastService, MsalService } from '@azure/msal-angular';
import { EMPTY, of } from 'rxjs';
import { App } from './app';

describe('App', () => {
  const msalServiceMock = {
    instance: {
      getActiveAccount: () => null,
      getAllAccounts: () => [],
      setActiveAccount: () => undefined,
    },
    handleRedirectObservable: () => EMPTY,
    loginRedirect: () => EMPTY,
    logoutRedirect: () => EMPTY,
  };

  const msalBroadcastServiceMock = {
    inProgress$: of(InteractionStatus.None),
    msalSubject$: EMPTY,
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [
        { provide: MsalService, useValue: msalServiceMock },
        { provide: MsalBroadcastService, useValue: msalBroadcastServiceMock },
      ],
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should render the application title', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('h1')?.textContent).toContain('VidaSalud');
  });
});
