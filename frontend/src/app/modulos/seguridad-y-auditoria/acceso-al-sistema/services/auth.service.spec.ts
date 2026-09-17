import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { AuthService } from './auth.service';
import { authInterceptor } from '../interceptors/auth.interceptor';
import { errorInterceptor } from '../../../../core/interceptors/error.interceptor';
import { environment } from '../../../../../environments/environment';
import { TokenResponse } from '../models/auth.model';

describe('CU-01 authentication flow', () => {
  let http: HttpTestingController;
  let client: HttpClient;
  const api = environment.apiUrl;
  const router = { navigate: vi.fn().mockResolvedValue(true) };
  const response = (seconds = 900): TokenResponse => ({
    accessToken: `header.${btoa(JSON.stringify({ exp: Math.floor(Date.now() / 1000) + seconds,
      authorities: ['ACCESO:USUARIOS:LECTURA'] }))}.signature`,
    refreshToken: 'refresh-token', tokenType: 'Bearer', expiresIn: seconds,
    usuario: { id: 1, username: 'admin', email: 'admin@erp.com', fullname: 'Administrador' }
  });
  const service = () => TestBed.inject(AuthService);
  function login(rememberMe = false, seconds = 900) {
    service().login({ usernameOrEmail: 'admin', password: 'test-password' }, rememberMe).subscribe();
    const req = http.expectOne(`${api}/auth/login`);
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush(response(seconds));
  }
  beforeEach(() => {
    localStorage.clear();
    sessionStorage.clear();
    router.navigate.mockClear();
    TestBed.configureTestingModule({ providers: [
      provideHttpClient(withInterceptors([authInterceptor, errorInterceptor])),
      provideHttpClientTesting(), { provide: Router, useValue: router }
    ] });
    http = TestBed.inject(HttpTestingController);
    client = TestBed.inject(HttpClient);
  });
  afterEach(() => {
    http.verify();
    localStorage.clear();
    sessionStorage.clear();
  });
  it('uses sessionStorage unless remember me is selected', () => {
    login();
    expect(sessionStorage.getItem('erp_session')).not.toBeNull();
    expect(localStorage.getItem('erp_session')).toBeNull();
    login(true);
    expect(localStorage.getItem('erp_session')).not.toBeNull();
    expect(sessionStorage.getItem('erp_session')).toBeNull();
  });
  it('checks the actual authority instead of allowing every logged-in user', () => {
    login();
    expect(service().hasPermission('ACCESO:USUARIOS:LECTURA')).toBe(true);
    expect(service().hasPermission('ACCESO:USUARIOS:ESCRITURA')).toBe(false);
  });
  it('does not consider expired access tokens authenticated', () => {
    login(false, -1);
    expect(service().isAuthenticated()).toBe(false);
    expect(service().hasPermission('ACCESO:USUARIOS:LECTURA')).toBe(false);
  });
  it('refreshes expired tokens once for concurrent API requests', () => {
    login(false, -1);
    client.get(`${api}/first`).subscribe();
    client.get(`${api}/second`).subscribe();
    const refresh = http.expectOne(`${api}/auth/refresh`);
    expect(refresh.request.body).toEqual({ refreshToken: 'refresh-token' });
    expect(refresh.request.headers.has('Authorization')).toBe(false);
    const renewed = response();
    refresh.flush(renewed);
    for (const path of ['first', 'second']) {
      const req = http.expectOne(`${api}/${path}`);
      expect(req.request.headers.get('Authorization')).toBe(`Bearer ${renewed.accessToken}`);
      req.flush({});
    }
    expect(service().isAuthenticated()).toBe(true);
  });
  it('retries an API 401 once with a refreshed token', () => {
    login();
    client.get(`${api}/protected`).subscribe();
    http.expectOne(`${api}/protected`).flush({}, { status: 401, statusText: 'Unauthorized' });
    const renewed = response(1200);
    http.expectOne(`${api}/auth/refresh`).flush(renewed);
    const retry = http.expectOne(`${api}/protected`);
    expect(retry.request.headers.get('Authorization')).toBe(`Bearer ${renewed.accessToken}`);
    retry.flush({});
    expect(router.navigate).not.toHaveBeenCalled();
  });
  it('clears session when refresh is rejected', () => {
    login();
    client.get(`${api}/protected`).subscribe({ error: () => {} });
    http.expectOne(`${api}/protected`).flush({}, { status: 401, statusText: 'Unauthorized' });
    http.expectOne(`${api}/auth/refresh`).flush({}, { status: 401, statusText: 'Unauthorized' });
    expect(service().getAccessToken()).toBeNull();
    expect(sessionStorage.getItem('erp_session')).toBeNull();
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });
  it('does not logout on an authorization 403 after refresh', () => {
    login();
    client.get(`${api}/protected`).subscribe({ error: () => {} });
    http.expectOne(`${api}/protected`).flush({}, { status: 401, statusText: 'Unauthorized' });
    http.expectOne(`${api}/auth/refresh`).flush(response());
    http.expectOne(`${api}/protected`).flush({}, { status: 403, statusText: 'Forbidden' });
    expect(service().isAuthenticated()).toBe(true);
    expect(router.navigate).not.toHaveBeenCalled();
  });
  it('does not attach tokens or handle third party 401s as session failures', () => {
    login();
    client.get('https://another.example/api/data').subscribe({ error: () => {} });
    const req = http.expectOne('https://another.example/api/data');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush({}, { status: 401, statusText: 'Unauthorized' });
    http.expectNone(`${api}/auth/refresh`);
    expect(service().isAuthenticated()).toBe(true);
  });
  it('does not revive a logged-out session with a late refresh response', () => {
    login(true);
    service().refresh().subscribe({ error: () => {} });
    const refresh = http.expectOne(`${api}/auth/refresh`);
    service().logout();
    const logout = http.expectOne(`${api}/auth/logout`);
    expect(logout.request.body).toEqual({ refreshToken: 'refresh-token' });
    logout.flush(null);
    refresh.flush(response());
    expect(service().getAccessToken()).toBeNull();
    expect(localStorage.getItem('erp_session')).toBeNull();
  });
  it('ignores malformed stored sessions', () => {
    sessionStorage.setItem('erp_session', 'null');
    expect(service().isAuthenticated()).toBe(false);
    expect(service().getRefreshToken()).toBeNull();
  });
  it('keeps the session on temporary refresh network failures', () => {
    login(false, -1);
    service().refresh().subscribe({ error: () => {} });
    http.expectOne(`${api}/auth/refresh`).error(new ProgressEvent('error'));
    expect(service().getRefreshToken()).toBe('refresh-token');
    expect(router.navigate).not.toHaveBeenCalled();
  });
  it('redirects to login if proactive refresh is rejected', () => {
    login(false, -1);
    client.get(`${api}/protected`).subscribe({ error: () => {} });
    http.expectOne(`${api}/auth/refresh`).flush({}, { status: 401, statusText: 'Unauthorized' });
    http.expectNone(`${api}/protected`);
    expect(service().getAccessToken()).toBeNull();
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });
  it('does not enter an infinite retry loop on repeated API 401s', () => {
    login();
    client.get(`${api}/protected`).subscribe({ error: () => {} });
    http.expectOne(`${api}/protected`).flush({}, { status: 401, statusText: 'Unauthorized' });
    http.expectOne(`${api}/auth/refresh`).flush(response());
    http.expectOne(`${api}/protected`).flush({}, { status: 401, statusText: 'Unauthorized' });
    http.expectOne(`${api}/auth/logout`).flush(null);
    http.expectNone(`${api}/auth/refresh`);
    expect(service().getAccessToken()).toBeNull();
  });
  it('does not clear a newer login when an older refresh fails', () => {
    login();
    client.get(`${api}/protected`).subscribe({ error: () => {} });
    http.expectOne(`${api}/protected`).flush({}, { status: 401, statusText: 'Unauthorized' });
    const pending = http.expectOne(`${api}/auth/refresh`);
    service().login({ usernameOrEmail: 'other', password: 'test' }).subscribe();
    const newer = { ...response(), refreshToken: 'new-session-refresh-token' };
    http.expectOne(`${api}/auth/login`).flush(newer);
    pending.flush({}, { status: 401, statusText: 'Unauthorized' });
    expect(service().getRefreshToken()).toBe(newer.refreshToken);
    expect(router.navigate).not.toHaveBeenCalled();
  });
});
