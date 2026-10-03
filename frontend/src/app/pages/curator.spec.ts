import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRouteSnapshot, provideRouter, Router, RouterStateSnapshot, UrlTree } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { NgForm } from '@angular/forms';
import { Auth, authInterceptor, curatorGuard } from '../core/auth';
import { validReference } from '../core/curator';
import { routes } from '../routes';
import { recordRouteReuseProvider } from '../core/route-reuse';
import { CuratorEditor } from './curator-editor';
import { CuratorList } from './curator-list';
import { CuratorContent } from './curator-content';
import { CuratorAuthorships } from './curator-authorships';
import { App } from '../app';

describe('Curator module',()=>{
  let http:HttpTestingController;
  const validForm={invalid:false} as NgForm;
  beforeEach(()=>{sessionStorage.clear();TestBed.configureTestingModule({providers:[recordRouteReuseProvider,provideRouter(routes),provideHttpClient(withInterceptors([authInterceptor])),provideHttpClientTesting()]});http=TestBed.inject(HttpTestingController);});
  afterEach(()=>{http.verify();sessionStorage.clear();});
  function role(role:string){const a=TestBed.inject(Auth);a.session.set({token:'test-only',expiresAt:Date.now()+60000});a.currentAccount.set({id:1,email:'test@example.test',roles:[role],active:true,registeredAt:'',lastLogin:null});}
  it('does not save the previous author when the edit URL changes',async()=>{
    role('CURATOR');const h=await RouterTestingHarness.create();
    for(const id of [1,2]){await h.navigateByUrl('/curator/authors/'+id+'/edit');http.expectOne('/api/authors/'+id).flush({id,firstName:'Autor '+id,lastName:'Test',birthYear:null,deathYear:null,biography:''});await settle(h);}
    const editor=h.fixture.debugElement.query(n=>n.componentInstance instanceof CuratorEditor).componentInstance as CuratorEditor;
    expect(editor.id).toBe(2);editor.save(validForm);const request=http.expectOne('/api/authors/2');expect(request.request.body.firstName).toBe('Autor 2');request.flush({id:2,firstName:'Autor 2'});await settle(h);
  });
  async function settle(h:RouterTestingHarness){await Promise.resolve();await Promise.resolve();h.detectChanges();}
  for(const value of ['VISITOR','CASHIER','GUIDE'])it('denies all curator children to '+value,async()=>{
    role(value);const h=await RouterTestingHarness.create();
    for(const url of ['/curator','/curator/exhibits/new','/curator/authors/1/edit','/curator/participations','/curator/digital-content','/curator/digital-content/new','/curator/digital-content/8/edit','/curator/profile']){
      await h.navigateByUrl(url);expect(TestBed.inject(Router).url).toBe('/forbidden');
    }
  });
  it('sends anonymous users to login',async()=>{const result=await TestBed.runInInjectionContext(()=>curatorGuard({} as ActivatedRouteSnapshot,{url:'/curator/exhibits'} as RouterStateSnapshot));expect(TestBed.inject(Router).serializeUrl(result as UrlTree)).toBe('/login?returnUrl=%2Fcurator%2Fexhibits');});
  it('restores curator authorization from the account API on refresh',async()=>{
    sessionStorage.setItem('museum.session',JSON.stringify({token:'test-only',expiresAt:Date.now()+60000}));
    const result=TestBed.runInInjectionContext(()=>curatorGuard({} as ActivatedRouteSnapshot,{url:'/curator'} as RouterStateSnapshot));
    http.expectOne('/api/accounts/me').flush({id:1,roles:['CURATOR'],email:'test@example.test'});
    expect(await result).toBe(true);
  });
  for(const value of ['CURATOR','ADMINISTRATOR'])it('allows '+value+' and shows only management navigation',async()=>{
    role(value);const result=await TestBed.runInInjectionContext(()=>curatorGuard({} as ActivatedRouteSnapshot,{} as RouterStateSnapshot));expect(result).toBe(true);
    const f=TestBed.createComponent(App);f.detectChanges();const nav=f.nativeElement.querySelector('nav').textContent;
    // Admin has its own workspace menu; curatorGuard access remains a separate policy check.
    const links=Array.from(f.nativeElement.querySelectorAll('nav a')).map((a:any)=>a.getAttribute('href'));
    if(value==='CURATOR'){
      expect(nav).toContain('Postavke');expect(nav).toContain('Digitalni sadržaj');
      expect(links).toEqual(['/curator','/curator/exhibits','/curator/authors','/curator/participations','/curator/digital-content','/curator/profile','/collection']);
    }else{
      expect(nav).toContain('Korisnički nalozi');expect(nav).toContain('Uloge');expect(nav).toContain('Zaposleni');
      expect(links).toEqual(['/admin','/admin/accounts','/admin/roles','/admin/employees','/collection','/admin/profile']);
    }
    expect(nav).not.toContain('Ulaznice');expect(nav).not.toContain('Validacija');expect(nav).not.toContain('Posete');
    expect(links).not.toContain('/manager');
  });
  it('loads server counts without per-participation content requests',async()=>{
    role('CURATOR');const h=await RouterTestingHarness.create();await h.navigateByUrl('/curator');
    http.expectOne('/api/exhibits').flush([{id:1}]);http.expectOne('/api/authors').flush([]);
    http.expectOne('/api/exhibit-participations').flush([{id:1,exhibitionId:2,installedAt:'2020-01-01T00:00:00Z',removedAt:null}]);
    http.expectOne('/api/exhibitions').flush([{id:2,status:'AKTIVNA',start:'2020-01-01T00:00:00Z',end:'2100-01-01T00:00:00Z'}]);await settle(h);
    expect(h.routeNativeElement?.textContent).toContain('Aktivnih učešća');
  });
  it('suppresses double submission and reports conflicts without server details',async()=>{
    role('CURATOR');const h=await RouterTestingHarness.create();await h.navigateByUrl('/curator/exhibits/new');
    // The componentless parent outlet exposes the actual leaf through the DOM injector.
    const editor=h.fixture.debugElement.query(n=>n.componentInstance instanceof CuratorEditor).componentInstance as CuratorEditor;
    await settle(h);editor.model['name']='Predmet';editor.model['inventoryNumber']='TEST';editor.save(validForm);editor.save(validForm);
    const request=http.expectOne('/api/exhibits');expect(request.request.method).toBe('POST');
    expect(request.request.body.name).toBe('Predmet');request.flush({detail:'PRIVATE-STACK'},{status:409,statusText:'Conflict'});await settle(h);
    expect(editor.error()).toContain('povezane podatke');expect(editor.error()).not.toContain('PRIVATE');expect(editor.busy()).toBe(false);
  });
  it('omits QR fields on creation and leaves token generation to backend',async()=>{
    role('CURATOR');const h=await RouterTestingHarness.create();await h.navigateByUrl('/curator/participations/new');
    http.expectOne('/api/exhibits').flush([{id:1,name:'Predmet'}]);http.expectOne('/api/exhibitions').flush([{id:2,name:'Izložba'}]);await settle(h);
    const editor=h.fixture.debugElement.query(n=>n.componentInstance instanceof CuratorEditor).componentInstance as CuratorEditor;
    editor.model['exhibitId']=1;editor.model['exhibitionId']=2;editor.save(validForm);
    const request=http.expectOne('/api/exhibit-participations');expect(request.request.body.qrToken).toBeUndefined();expect(request.request.body.exhibitId).toBe(1);
    request.flush(null,{status:404,statusText:'Not Found'});await settle(h);expect(editor.error()).toContain('nije pronađen');
  });
  it('deletes only after confirmation and keeps the row on conflict',async()=>{
    role('CURATOR');const h=await RouterTestingHarness.create();await h.navigateByUrl('/curator/authors');
    http.expectOne('/api/authors').flush([{id:1,firstName:'Ana'}]);await settle(h);
    const page=h.fixture.debugElement.query(n=>n.componentInstance instanceof CuratorList).componentInstance as CuratorList;
    page.remove(1);http.expectNone('/api/authors/1');page.pending.set(1);page.remove(1);
    http.expectOne('/api/authors/1').flush(null,{status:409,statusText:'Conflict'});await settle(h);expect(page.rows().length).toBe(1);
  });
  it('uses author and exhibit composite key and does not delete either parent',async()=>{
    role('CURATOR');const f=TestBed.createComponent(CuratorAuthorships);f.componentRef.setInput('exhibitId',4);f.detectChanges();
    http.expectOne('/api/authors').flush([{id:7,firstName:'Ana'}]);http.expectOne('/api/exhibits/4/authorships/management').flush([]);await f.whenStable();
    const p=f.componentInstance;await vi.waitFor(()=>expect(p.busy()).toBe(false));p.authorId=7;p.note='Napomena';p.save(validForm);
    const put=http.expectOne('/api/exhibits/4/authorships/7');expect(put.request.method).toBe('PUT');expect(put.request.body.note).toBe('Napomena');put.flush({authorId:7,exhibitId:4,note:'Napomena'});await f.whenStable();
    await vi.waitFor(()=>expect(p.busy()).toBe(false));p.pending.set(7);p.remove(7);const deletion=http.expectOne('/api/exhibits/4/authorships/7');expect(deletion.request.method).toBe('DELETE');deletion.flush(null);await vi.waitFor(()=>expect(p.busy()).toBe(false));
    expect(p.links()).toEqual([]);
  });
  it('validates content references and links content to selected participation',async()=>{
    role('CURATOR');const h=await RouterTestingHarness.create();await h.navigateByUrl('/curator/digital-content/new');
    http.expectOne('/api/exhibit-participations').flush([{id:3,exhibitionId:2,exhibitId:1}]);http.expectOne('/api/exhibits').flush([{id:1,name:'Predmet'}]);http.expectOne('/api/exhibitions').flush([{id:2,name:'Izložba'}]);await settle(h);
    const p=h.fixture.debugElement.query(n=>n.componentInstance instanceof CuratorContent).componentInstance as CuratorContent;
    p.select(3);await settle(h);
    p.path='javascript:alert(1)';p.save(validForm);http.expectNone('/api/digital-content');
    const navigate=vi.spyOn(TestBed.inject(Router),'navigate').mockResolvedValue(true);
    p.path='demo/grad-001.txt';p.save(validForm);p.save(validForm);const req=http.expectOne('/api/digital-content');expect(req.request.body.participationId).toBe(3);expect(req.request.body.type).toBe('TEKST');req.flush({id:8,...req.request.body});await settle(h);expect(p.notice()).toContain('sačuvan');
    expect(navigate).toHaveBeenCalledWith(['/curator/digital-content',8,'edit'],{queryParams:{saved:1},replaceUrl:true});
  });
  it('loads direct content edit route and preserves its participation on update',async()=>{
    role('CURATOR');const h=await RouterTestingHarness.create();await h.navigateByUrl('/curator/digital-content/8/edit');
    http.expectOne('/api/exhibit-participations').flush([{id:3,exhibitionId:2,exhibitId:1}]);http.expectOne('/api/exhibits').flush([{id:1,name:'Predmet'}]);http.expectOne('/api/exhibitions').flush([{id:2,name:'Izložba'}]);
    http.expectOne('/api/digital-content/8').flush({id:8,participationId:3,type:'TEKST',path:'demo/grad-001.txt',language:'sr',version:1,automaticallyGenerated:false});
    await settle(h);const p=h.fixture.debugElement.query(n=>n.componentInstance instanceof CuratorContent).componentInstance as CuratorContent;
    expect(p.editId).toBe(8);p.select(9);expect(p.participationId).toBe(3);
    p.version=2;p.save(validForm);const req=http.expectOne('/api/digital-content/8');expect(req.request.method).toBe('PUT');expect(req.request.body.participationId).toBe(3);
    req.flush({id:8,...req.request.body});await settle(h);expect(p.notice()).toContain('sačuvan');
  });
  it('shows missing content without an editable form',async()=>{
    role('CURATOR');const h=await RouterTestingHarness.create();await h.navigateByUrl('/curator/digital-content/999/edit');
    http.expectOne('/api/exhibit-participations').flush([]);http.expectOne('/api/exhibits').flush([]);http.expectOne('/api/exhibitions').flush([]);
    http.expectOne('/api/digital-content/999').flush(null,{status:404,statusText:'Not Found'});await settle(h);
    const p=h.fixture.debugElement.query(n=>n.componentInstance instanceof CuratorContent).componentInstance as CuratorContent;
    await vi.waitFor(()=>expect(p.busy()).toBe(false));h.detectChanges();
    expect(h.routeNativeElement?.textContent).toContain('nije pronađen');
    expect(h.routeNativeElement?.querySelector('form')).toBeNull();
  });
  it('rejects unsafe references',()=>{
    for(const value of ['javascript:alert(1)','file:///secret','//evil.test/a','../private','https://user:pass@example.test/a','%2e%2e/private'])expect(validReference(value)).toBe(false);
    expect(validReference('demo/grad-001.txt')).toBe(true);expect(validReference('https://example.test/a.mp3')).toBe(true);
  });
});
