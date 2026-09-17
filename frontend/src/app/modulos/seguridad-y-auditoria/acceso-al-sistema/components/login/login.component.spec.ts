import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { Router, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { LoginComponent } from './login.component';
import { AuthService } from '../../services/auth.service';
import { ThemeService } from '../../../../../core/services/theme.service';

describe('CU-01 login screen', () => {
  const auth = { login: vi.fn() };
  beforeEach(() => {
    auth.login.mockReset();
    TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: auth },
        { provide: ThemeService, useValue: { isDark: () => false, toggle: () => {} } },
      ],
    });
  });
  function component() {
    const component = TestBed.createComponent(LoginComponent).componentInstance;
    component.form.setValue({
      usernameOrEmail: ' admin ',
      password: ' password ',
      rememberMe: true,
    });
    return component;
  }
  it('passes remember me and trims the identifier without changing the password', () => {
    auth.login.mockReturnValue(of({}));
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    const login = component();
    login.submit();
    expect(auth.login).toHaveBeenCalledWith(
      { usernameOrEmail: 'admin', password: ' password ' },
      true,
    );
    expect(navigate).toHaveBeenCalledWith(['/app']);
    expect(login.loading()).toBe(false);
  });
  it('shows a connection error instead of invalid credentials on network failure', () => {
    auth.login.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 0 })));
    const login = component();
    login.submit();
    expect(login.errorMessage()).toContain('conectar con el servidor');
    expect(login.loading()).toBe(false);
  });
  it('shows the account lock message returned by the backend', () => {
    auth.login.mockReturnValue(
      throwError(
        () =>
          new HttpErrorResponse({
            status: 423,
            error: { error: 'ACCOUNT_LOCKED', message: 'Cuenta bloqueada por 15 minutos' },
          }),
      ),
    );
    const login = component();
    login.submit();
    expect(login.accountLocked()).toBe(true);
    expect(login.errorMessage()).toBe('Cuenta bloqueada por 15 minutos');
  });
  it('does not submit an empty form', () => {
    const login = TestBed.createComponent(LoginComponent).componentInstance;
    login.submit();
    expect(auth.login).not.toHaveBeenCalled();
  });
});
