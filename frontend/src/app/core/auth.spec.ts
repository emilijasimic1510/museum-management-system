import { TestBed } from '@angular/core/testing';
import { HttpClient, HttpErrorResponse, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, Router, RouterStateSnapshot, ActivatedRouteSnapshot } from '@angular/router';
import { Auth, authGuard, authInterceptor, safeReturnUrl, visitorGuard, cashierGuard } from './auth';
import { errorMessage } from './api';
import { I18n } from './i18n';

describe('Visitor session and backend boundary',()=>{
  let http:HttpClient; let requests:HttpTestingController; let auth:Auth;
  beforeEach(()=>{
    sessionStorage.clear();
    TestBed.configureTestingModule({providers:[provideRouter([]),provideHttpClient(withInterceptors([authInterceptor])),provideHttpClientTesting()]});
    http=TestBed.inject(HttpClient);requests=TestBed.inject(HttpTestingController);auth=TestBed.inject(Auth);
  });
  afterEach(()=>{requests.verify();sessionStorage.clear();});
  async function login(){const result=auth.login('visitor@example.test','example-test-password');requests.expectOne('/api/auth/login').flush({accessToken:'test-jwt',expiresIn:1800,tokenType:'Bearer'});await result;}
  it('sends the selected UI language on API calls including registration, but not external calls',()=>{
    const i18n=TestBed.inject(I18n);
    for(const language of ['en','sr'] as const){
      i18n.language.set(language);
      http.post('/api/auth/register',{}).subscribe();
      const request=requests.expectOne('/api/auth/register');
      expect(request.request.headers.get('X-Museum-Language')).toBe(language);
      request.flush({});
    }
    http.get('https://example.test/content').subscribe();
    const external=requests.expectOne('https://example.test/content');
    expect(external.request.headers.has('X-Museum-Language')).toBe(false);external.flush({});
  });
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
  it('shows Serbian validation text without echoing backend input',()=>{expect(errorMessage(new HttpErrorResponse({status:400,error:{detail:'Request validation failed',errors:[{field:'email',message:'must be valid'}]}}))).toBe('Proverite unete podatke.');});
});


describe('Role routing',()=>{
  beforeEach(()=>{sessionStorage.clear();TestBed.configureTestingModule({providers:[provideRouter([]),provideHttpClient(),provideHttpClientTesting()]});});
  afterEach(()=>sessionStorage.clear());
  const state={url:'/tickets'} as RouterStateSnapshot;
  const route={} as ActivatedRouteSnapshot;
  async function check(role:string,guard:typeof visitorGuard){
    const auth=TestBed.inject(Auth);auth.session.set({token:'test-only',expiresAt:Date.now()+60000});
    auth.currentAccount.set({id:1,email:'test@example.test',roles:[role],active:true,registeredAt:'',lastLogin:null});
    return await TestBed.runInInjectionContext(()=>guard(route,state));
  }
  it('blocks visitor access to cashier',async()=>{const result=await check('VISITOR',cashierGuard);expect(TestBed.inject(Router).serializeUrl(result as import('@angular/router').UrlTree)).toBe('/');});
  it('blocks cashier access to visitor pages',async()=>{const result=await check('ROLE_CASHIER',visitorGuard);expect(TestBed.inject(Router).serializeUrl(result as import('@angular/router').UrlTree)).toBe('/cashier');});
  it('allows the matching role',async()=>{expect(await check('VISITOR',visitorGuard)).toBe(true);expect(await check('CASHIER',cashierGuard)).toBe(true);});
  it('restores a valid session on refresh and resolves roles from API',async()=>{
    sessionStorage.setItem('museum.session',JSON.stringify({token:'test-only',expiresAt:Date.now()+60000}));
    const result=TestBed.runInInjectionContext(()=>visitorGuard(route,state));
    TestBed.inject(HttpTestingController).expectOne('/api/accounts/me').flush({roles:['VISITOR']});
    expect(await result).toBe(true);expect(TestBed.inject(Auth).isVisitor()).toBe(true);
  });
});
