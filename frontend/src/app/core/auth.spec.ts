import { TestBed } from '@angular/core/testing';
import { HttpClient, HttpErrorResponse, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, Router, RouterStateSnapshot, ActivatedRouteSnapshot } from '@angular/router';
import { Auth, authGuard, authInterceptor, safeReturnUrl } from './auth';
import { errorMessage } from './api';

describe('Visitor session and backend boundary',()=>{
  let http:HttpClient; let requests:HttpTestingController; let auth:Auth;
  beforeEach(()=>{
    sessionStorage.clear();
    TestBed.configureTestingModule({providers:[provideRouter([]),provideHttpClient(withInterceptors([authInterceptor])),provideHttpClientTesting()]});
    http=TestBed.inject(HttpClient);requests=TestBed.inject(HttpTestingController);auth=TestBed.inject(Auth);
  });
  afterEach(()=>{requests.verify();sessionStorage.clear();});
  async function login(){const result=auth.login('visitor@example.test','example-test-password');requests.expectOne('/api/auth/login').flush({accessToken:'test-jwt',expiresIn:1800,tokenType:'Bearer'});await result;}
  it('stores login token and attaches it to a protected API request',async()=>{
    await login();expect(JSON.parse(sessionStorage.getItem('museum.session')!).token).toBe('test-jwt');
    http.get('/api/visitors/me').subscribe();const request=requests.expectOne('/api/visitors/me');expect(request.request.headers.get('Authorization')).toBe('Bearer test-jwt');request.flush({});
  });
  it('does not send the token to external services or login',async()=>{
    await login();http.get('https://example.test/content').subscribe();const external=requests.expectOne('https://example.test/content');expect(external.request.headers.has('Authorization')).toBe(false);external.flush({});
    const result=auth.login('visitor@example.test','example-test-password');const request=requests.expectOne('/api/auth/login');expect(request.request.headers.has('Authorization')).toBe(false);request.flush({accessToken:'new',expiresIn:1800});await result;
  });
  it('rejects an expired session and redirects guarded navigation',()=>{
    auth.session.set({token:'expired',expiresAt:Date.now()-1});
    const result=TestBed.runInInjectionContext(()=>authGuard({} as ActivatedRouteSnapshot,{url:'/tickets'} as RouterStateSnapshot));
    expect(TestBed.inject(Router).serializeUrl(result as import('@angular/router').UrlTree)).toBe('/login?returnUrl=%2Ftickets');expect(auth.session()).toBeNull();
  });
  it('clears rejected JWT and sends the user to login',async()=>{
    await login();const navigate=vi.spyOn(TestBed.inject(Router),'navigate').mockResolvedValue(true);
    http.get('/api/tickets/me').subscribe({error:()=>{}});requests.expectOne('/api/tickets/me').flush(null,{status:401,statusText:'Unauthorized'});
    expect(auth.session()).toBeNull();expect(navigate).toHaveBeenCalled();
  });
  it('keeps the session when role authorization returns 403',async()=>{
    await login();http.get('/api/tickets/me').subscribe({error:()=>{}});requests.expectOne('/api/tickets/me').flush(null,{status:403,statusText:'Forbidden'});expect(auth.token()).toBe('test-jwt');
  });
  it('logout removes stored token',async()=>{await login();vi.spyOn(TestBed.inject(Router),'navigateByUrl').mockResolvedValue(true);auth.logout();expect(sessionStorage.getItem('museum.session')).toBeNull();expect(auth.token()).toBeNull();});
  it('keeps return URLs within the application',()=>{expect(safeReturnUrl('//example.test')).toBe('/profile');expect(safeReturnUrl('/\\example.test')).toBe('/profile');expect(safeReturnUrl('/exhibitions/12')).toBe('/exhibitions/12');});
  it('preserves field validation details from backend ProblemDetail',()=>{expect(errorMessage(new HttpErrorResponse({status:400,error:{detail:'Request validation failed',errors:[{field:'email',message:'must be valid'}]}}))).toContain('email: must be valid');});
});
