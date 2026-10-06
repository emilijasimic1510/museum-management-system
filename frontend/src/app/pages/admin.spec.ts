import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute, ActivatedRouteSnapshot, convertToParamMap, provideRouter, Router, RouterStateSnapshot } from '@angular/router';
import { adminGuard } from '../core/admin';
import { Auth, visitorGuard } from '../core/auth';
import { Account } from '../core/models';
import { AdminAccount } from './admin-account';
import { AdminOverview } from './admin-overview';
import { AdminLists } from './admin-lists';
import { AdminEmployee } from './admin-employee';

const account:Account={id:3,email:'user@museum.test',active:true,roles:['VISITOR'],registeredAt:'2026-01-01T00:00:00Z',lastLogin:null};
const roles=[{id:1,name:'VISITOR',description:'Visitor'},{id:2,name:'MANAGER',description:'Manager'},{id:3,name:'ADMINISTRATOR',description:'Administrator'}];
describe('Administrator workspace',()=>{
  let http:HttpTestingController;
  function setup(data:Record<string,unknown>={},id='3',query:Record<string,string>={}){
    TestBed.configureTestingModule({providers:[provideHttpClient(),provideHttpClientTesting(),provideRouter([]),{provide:ActivatedRoute,useValue:{snapshot:{data,paramMap:convertToParamMap({id}),queryParamMap:convertToParamMap(query)}}}]});http=TestBed.inject(HttpTestingController);
  }
  beforeEach(()=>{vi.useFakeTimers();sessionStorage.clear();});afterEach(()=>{http?.verify();vi.useRealTimers();sessionStorage.clear();});
  async function render<T>(f:ComponentFixture<T>){await vi.advanceTimersByTimeAsync(0);f.detectChanges();await vi.advanceTimersByTimeAsync(0);f.detectChanges();}
  function input<T>(f:ComponentFixture<T>,name:string,value:string){const el:HTMLInputElement=f.nativeElement.querySelector(`[name="${name}"]`);el.value=value;el.dispatchEvent(new Event('input',{bubbles:true}));}
  function select<T>(f:ComponentFixture<T>,name:string,index:number){const el:HTMLSelectElement=f.nativeElement.querySelector(`[name="${name}"]`);el.selectedIndex=index;el.dispatchEvent(new Event('change',{bubbles:true}));}
  function submit<T>(f:ComponentFixture<T>,index=0){f.nativeElement.querySelectorAll('form')[index].dispatchEvent(new Event('submit',{bubbles:true,cancelable:true}));}
  function loadAccount(a:Account=account){http.expectOne('/api/admin/accounts/3').flush(a);http.expectOne('/api/admin/roles').flush(roles);}
  function login(role:string,id=99){const auth=TestBed.inject(Auth);auth.session.set({token:'test',expiresAt:Date.now()+60000});auth.currentAccount.set({...account,id,roles:[role]});return auth;}

  it('renders dashboard counts from actual accounts and employees',async()=>{
    setup();const f=TestBed.createComponent(AdminOverview);
    http.expectOne('/api/admin/accounts').flush([account,{...account,id:4,active:false,roles:['MANAGER','VISITOR']}]);http.expectOne('/api/employees').flush([{id:7}]);http.expectOne('/api/admin/roles').flush(roles);http.expectOne('/api/health').flush({status:'UP',application:'museum-backend'});await render(f);
    expect(f.componentInstance.activeCount()).toBe(1);expect(f.componentInstance.roleCount('VISITOR')).toBe(2);expect(f.componentInstance.employees().length).toBe(1);expect(f.nativeElement.textContent).toContain('museum-backend');expect(f.nativeElement.querySelector('a[href="/collection"]')).not.toBeNull();
  });
  it('searches accounts case-insensitively and shows empty state',async()=>{
    setup({kind:'accounts'});const f=TestBed.createComponent(AdminLists);http.expectOne('/api/admin/accounts').flush([account]);await render(f);
    input(f,'search','USER@MUSEUM');await render(f);expect(f.nativeElement.textContent).toContain(account.email);
    input(f,'search','missing');await render(f);expect(f.nativeElement.textContent).toContain('Nema naloga za zadatu pretragu');
  });
  it('edits email and requires confirmation before deactivation, preserving unsaved email',async()=>{
    setup({mode:'edit'});const f=TestBed.createComponent(AdminAccount);loadAccount();await render(f);
    input(f,'email','new@museum.test');await render(f);submit(f);let req=http.expectOne('/api/admin/accounts/3');expect(req.request.method).toBe('PUT');expect(req.request.body).toEqual({email:'new@museum.test',active:true});req.flush({...account,email:'new@museum.test'});await render(f);
    input(f,'email','unsaved@museum.test');await render(f);const button=[...f.nativeElement.querySelectorAll('button')].find((b:any)=>b.textContent.includes('Deaktiviraj')) as HTMLButtonElement;button.click();await render(f);http.expectNone(r=>r.method==='PUT');expect(f.nativeElement.textContent).toContain('Potvrda izmene');
    f.componentInstance.applyConfirmation();req=http.expectOne('/api/admin/accounts/3');expect(req.request.body).toEqual({email:'new@museum.test',active:false});req.flush({...account,email:'new@museum.test',active:false});await render(f);expect(f.nativeElement.textContent).toContain('istorija je sačuvana');
  });
  it('assigns existing role and removes it only after confirmation',async()=>{
    setup({mode:'view'});const f=TestBed.createComponent(AdminAccount);loadAccount();await render(f);
    select(f,'roleId',1);await render(f);submit(f);const grant=http.expectOne('/api/admin/accounts/3/roles/2');expect(grant.request.method).toBe('POST');grant.flush({...account,roles:['VISITOR','MANAGER']});await render(f);expect(f.nativeElement.textContent).toContain('Uloga je dodeljena');
    f.componentInstance.askRevoke('MANAGER');await render(f);http.expectNone(r=>r.method==='DELETE');f.componentInstance.applyConfirmation();const revoke=http.expectOne('/api/admin/accounts/3/roles/2');expect(revoke.request.method).toBe('DELETE');revoke.flush(null);await render(f);http.expectOne('/api/admin/accounts/3').flush(account);await render(f);expect(f.nativeElement.textContent).toContain('Uloga je uklonjena');
    const last=[...f.nativeElement.querySelectorAll('button')].find((b:any)=>b.classList.contains('danger')&&b.textContent.includes('Posetilac')) as HTMLButtonElement;expect(last.disabled).toBe(true);f.componentInstance.askRevoke('VISITOR');f.componentInstance.applyConfirmation();http.expectNone(r=>r.method==='DELETE');
  });
  it('creates through the existing account API with a masked password and clears it after success',async()=>{
    setup({mode:'new'});const f=TestBed.createComponent(AdminAccount);const nav=vi.spyOn(TestBed.inject(Router),'navigate').mockResolvedValue(true);await render(f);
    expect(f.nativeElement.querySelector('[name="password"]').type).toBe('password');
    for(const [k,v] of Object.entries({email:'new@museum.test',password:'Initial-password-123',firstName:'Ana',lastName:'Anić'}))input(f,k,v);await render(f);submit(f);
    const req=http.expectOne('/api/admin/accounts');expect(req.request.method).toBe('POST');expect(req.request.body).toMatchObject({email:'new@museum.test',password:'Initial-password-123',recommendationsConsent:false});req.flush({...account,id:9});await render(f);expect(f.componentInstance.draft.password).toBe('');expect(nav).toHaveBeenCalledWith(['/admin/accounts',9],{queryParams:{created:1}});
  });
  it('shows safe conflict and retains data for retry',async()=>{
    setup({mode:'edit'});const f=TestBed.createComponent(AdminAccount);loadAccount();await render(f);input(f,'email','duplicate@museum.test');await render(f);submit(f);http.expectOne('/api/admin/accounts/3').flush({message:'PRIVATE DB EXCEPTION'},{status:409,statusText:'Conflict'});await render(f);
    expect(f.nativeElement.textContent).toContain('Izmena nije moguća');expect(f.nativeElement.textContent).not.toContain('PRIVATE DB');expect(f.componentInstance.email).toBe('duplicate@museum.test');expect(f.componentInstance.busy()).toBe(false);
  });
  it('clears own session on confirmed self-deactivation',async()=>{
    setup({mode:'view'});const auth=login('ADMINISTRATOR',3);const nav=vi.spyOn(TestBed.inject(Router),'navigateByUrl').mockResolvedValue(true);const f=TestBed.createComponent(AdminAccount);loadAccount({...account,roles:['ADMINISTRATOR']});await render(f);
    f.componentInstance.confirm.set('status');f.componentInstance.applyConfirmation();http.expectOne('/api/admin/accounts/3').flush({...account,active:false,roles:['ADMINISTRATOR']});await render(f);expect(auth.token()).toBeNull();expect(nav).toHaveBeenCalledWith('/login');
  });
  it('drops cached admin role immediately after self-revocation without forbidden follow-up GET',async()=>{
    setup({mode:'view'});const auth=login('ADMINISTRATOR',3);vi.spyOn(TestBed.inject(Router),'navigateByUrl').mockResolvedValue(true);const f=TestBed.createComponent(AdminAccount);loadAccount({...account,roles:['ADMINISTRATOR','VISITOR']});await render(f);
    f.componentInstance.askRevoke('ADMINISTRATOR');f.componentInstance.applyConfirmation();http.expectOne('/api/admin/accounts/3/roles/3').flush(null);await render(f);expect(auth.isAdmin()).toBe(false);http.expectNone('/api/admin/accounts/3');
  });
  it('creates one employee subtype without granting a system role',async()=>{
    setup({mode:'new'});const f=TestBed.createComponent(AdminEmployee);http.expectOne('/api/admin/accounts').flush([account]);http.expectOne('/api/museums').flush([{id:2,name:'Muzej',active:true}]);http.expectOne('/api/employees').flush([]);vi.spyOn(TestBed.inject(Router),'navigate').mockResolvedValue(true);await render(f);
    select(f,'accountId',1);select(f,'museumId',1);select(f,'type',1);input(f,'firstName','Ana');input(f,'lastName','Anić');input(f,'hiredOn','2026-01-01');input(f,'subtypeDetail','sr');await render(f);submit(f);
    const req=http.expectOne('/api/employees');expect(req.request.body).toEqual({accountId:3,museumId:2,firstName:'Ana',lastName:'Anić',phone:null,hiredOn:'2026-01-01',supervisorId:null,type:'GUIDE',primaryLanguage:'sr'});req.flush({id:7,...req.request.body});await render(f);http.expectNone(r=>r.url.includes('/roles/'));
  });
  it('locks existing employee account and subtype while permitting profile edits',async()=>{
    setup({mode:'edit'},'7');const f=TestBed.createComponent(AdminEmployee);const e={id:7,accountId:3,museumId:2,firstName:'Ana',lastName:'Anić',phone:null,hiredOn:'2026-01-01',supervisorId:null,type:'GUIDE',primaryLanguage:'sr'};
    http.expectOne('/api/admin/accounts').flush([account]);http.expectOne('/api/museums').flush([{id:2,name:'Muzej'}]);http.expectOne('/api/employees').flush([e]);http.expectOne('/api/employees/7').flush(e);await render(f);
    expect(f.nativeElement.querySelector('[name="type"]').disabled).toBe(true);expect(f.nativeElement.querySelector('[name="accountId"]').disabled).toBe(true);input(f,'phone','060123');await render(f);submit(f);const req=http.expectOne('/api/employees/7');expect(req.request.method).toBe('PUT');expect(req.request.body).toMatchObject({type:'GUIDE',accountId:3,phone:'060123'});req.flush({...e,phone:'060123'});await render(f);expect(f.nativeElement.textContent).toContain('Profil zaposlenog je sačuvan');
  });
  it('shows loading, error and allows reloading an empty list',async()=>{
    setup({kind:'accounts'});const f=TestBed.createComponent(AdminLists);await render(f);expect(f.nativeElement.textContent).toContain('Učitavanje');http.expectOne('/api/admin/accounts').flush(null,{status:500,statusText:'Error'});await render(f);expect(f.componentInstance.busy()).toBe(false);expect(f.componentInstance.error()).not.toBe('');f.componentInstance.load();http.expectOne('/api/admin/accounts').flush([]);await render(f);expect(f.nativeElement.textContent).toContain('Nema naloga');
  });
  for(const role of ['ADMINISTRATOR','MANAGER','CURATOR','CASHIER','GUIDE','VISITOR'])it('guards admin workspace for '+role,async()=>{
    setup();login(role);const result=await TestBed.runInInjectionContext(()=>adminGuard({} as ActivatedRouteSnapshot,{url:'/admin'} as RouterStateSnapshot));if(role==='ADMINISTRATOR')expect(result).toBe(true);else expect(TestBed.inject(Router).serializeUrl(result as any)).toBe('/forbidden');
  });
  it('redirects anonymous users to login and the administrator landing to /admin',async()=>{
    setup();const router=TestBed.inject(Router);const result=await TestBed.runInInjectionContext(()=>adminGuard({} as ActivatedRouteSnapshot,{url:'/admin'} as RouterStateSnapshot));expect(router.serializeUrl(result as any)).toContain('/login?returnUrl=%2Fadmin');login('ADMINISTRATOR');
    const landing=await TestBed.runInInjectionContext(()=>visitorGuard({data:{publicCatalog:true}} as unknown as ActivatedRouteSnapshot,{url:'/'} as RouterStateSnapshot));expect(router.serializeUrl(landing as any)).toBe('/admin');
  });
});
