import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRouteSnapshot, provideRouter, Router, RouterStateSnapshot, UrlTree } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { NgForm } from '@angular/forms';
import { Auth, authInterceptor, guideGuard } from '../core/auth';
import { routes } from '../routes';
import { recordRouteReuseProvider } from '../core/route-reuse';
import { EventState, MuseumEvent, registrationOpen } from '../core/events';
import { App } from '../app';
import { GuideEventEditor } from './guide-event-editor';
import { GuideRegistrations } from './guide-registrations';
import { EventDetail } from './event-detail';
import { EventsPage } from './events';
import { AuthPage } from './auth-page';

describe('Guide and visitor events',()=>{
  let http:HttpTestingController;
  const form={invalid:false} as NgForm;
  const event:MuseumEvent={id:7,exhibitionId:2,guideId:8,name:'Razgovor o umetnosti',type:'PREDAVANJE',status:'PLANIRAN',start:'2099-01-01T10:00:00Z',end:'2099-01-01T11:00:00Z',capacity:2,registeredCount:1,language:'sr'};
  const guide={id:8,firstName:'Demo',lastName:'Vodič',primaryLanguage:'sr'};
  beforeEach(()=>{sessionStorage.clear();TestBed.configureTestingModule({providers:[recordRouteReuseProvider,provideRouter(routes),provideHttpClient(withInterceptors([authInterceptor])),provideHttpClientTesting()]});http=TestBed.inject(HttpTestingController);});
  afterEach(()=>{http.verify();sessionStorage.clear();});
  function role(value:string){const a=TestBed.inject(Auth);a.session.set({token:'test-only',expiresAt:Date.now()+60000});a.currentAccount.set({id:1,email:'guide@example.test',roles:[value],active:true,registeredAt:'',lastLogin:null});}
  async function settle(h:RouterTestingHarness){await vi.waitFor(()=>{for(const node of h.fixture.debugElement.queryAll(n=>n.componentInstance instanceof EventState||n.componentInstance instanceof AuthPage))expect(node.componentInstance.busy()).toBe(false);});await h.fixture.whenStable();h.detectChanges();}
  function component<T>(h:RouterTestingHarness,type:new (...args:any[])=>T):T{return h.fixture.debugElement.query(n=>n.componentInstance instanceof type).componentInstance;}
  function detailResponse(registrations:unknown[]=[]){http.expectOne('/api/events/7').flush(event);http.expectOne('/api/exhibitions').flush([{id:2,name:'Izložba'}]);if(TestBed.inject(Auth).isVisitor())http.expectOne('/api/visitors/me/registrations').flush(registrations);}
  for(const denied of ['VISITOR','CASHIER','CURATOR','ADMINISTRATOR'])it('denies guide children to '+denied,async()=>{
    role(denied);const h=await RouterTestingHarness.create();for(const url of ['/guide','/guide/events','/guide/events/new','/guide/events/7/edit','/guide/events/7/registrations','/guide/registrations','/guide/profile']){await h.navigateByUrl(url);expect(TestBed.inject(Router).url).toBe('/forbidden');}
  });
  it('redirects anonymous guides to login and restores authorized sessions',async()=>{
    const snapshot={} as ActivatedRouteSnapshot,state={url:'/guide'} as RouterStateSnapshot;
    const denied=await TestBed.runInInjectionContext(()=>guideGuard(snapshot,state));expect(TestBed.inject(Router).serializeUrl(denied as UrlTree)).toContain('/login?returnUrl=');
    TestBed.inject(Auth).session.set({token:'test-only',expiresAt:Date.now()+60000});
    const result=TestBed.runInInjectionContext(()=>guideGuard(snapshot,state));http.expectOne('/api/accounts/me').flush({roles:['ROLE_GUIDE']});expect(await result).toBe(true);
  });
  it('shows only guide navigation and keeps cashier priority for the old demo account',()=>{
    role('GUIDE');const f=TestBed.createComponent(App);f.detectChanges();const nav=f.nativeElement.querySelector('nav').textContent;expect(nav).toContain('Događaji');expect(nav).toContain('Prijave');expect(nav).not.toContain('Ulaznice');expect(nav).not.toContain('Eksponati');expect(nav).not.toContain('Validacija');
    TestBed.inject(Auth).currentAccount.update(a=>({...a!,roles:['CASHIER','GUIDE']}));expect(TestBed.inject(Auth).isGuide()).toBe(false);expect(TestBed.inject(Auth).isCashier()).toBe(true);
  });
  it('redirects guide login to the guide dashboard',async()=>{
    const h=await RouterTestingHarness.create();await h.navigateByUrl('/login');const p=component(h,AuthPage);p.email='guide@example.test';p.password='test-password';const navigate=vi.spyOn(TestBed.inject(Router),'navigateByUrl').mockResolvedValue(true);p.submit(form);
    http.expectOne('/api/auth/login').flush({accessToken:'test-only',expiresIn:600});await vi.waitFor(()=>http.expectOne('/api/accounts/me').flush({roles:['GUIDE']}));await settle(h);expect(navigate).toHaveBeenCalledWith('/guide');
  });
  it('calculates dashboard totals using the guide employee id',async()=>{
    role('GUIDE');const h=await RouterTestingHarness.create();await h.navigateByUrl('/guide');http.expectOne('/api/events').flush([event,{...event,id:9,status:'OTKAZAN'}]);http.expectOne('/api/guides/me').flush(guide);await settle(h);expect(h.routeNativeElement?.textContent).toContain('Događaja dodeljenih meni');expect(h.routeNativeElement?.textContent).toContain(event.name);
  });
  it('validates dates and capacity and suppresses duplicate event submission',async()=>{
    role('GUIDE');const h=await RouterTestingHarness.create();await h.navigateByUrl('/guide/events/new');http.expectOne('/api/exhibitions').flush([{id:2,name:'Izložba'}]);http.expectOne('/api/guides/me').flush(guide);await settle(h);const p=component(h,GuideEventEditor);
    p.name='Događaj';p.exhibitionId=2;p.start='2099-01-01T12:00';p.end='2099-01-01T11:00';p.save(form);expect(p.error()).toContain('Završetak');http.expectNone('/api/events');p.end='2099-01-01T13:00';p.capacity=1.5;p.save(form);http.expectNone('/api/events');p.capacity=2;
    p.save(form);p.save(form);const r=http.expectOne('/api/events');expect(r.request.body.guideId).toBe(8);expect(r.request.body.start).toBe(new Date(p.start).toISOString());r.flush({private:'do not echo'},{status:409,statusText:'Conflict'});await settle(h);expect(p.error()).toContain('Kapacitet');expect(p.error()).not.toContain('private');expect(p.busy()).toBe(false);
  });
  it('loads a direct edit URL and preserves the assigned guide and exhibition',async()=>{
    role('GUIDE');const h=await RouterTestingHarness.create();await h.navigateByUrl('/guide/events/7/edit');http.expectOne('/api/exhibitions').flush([]);http.expectOne('/api/guides/me').flush(guide);http.expectOne('/api/events/7').flush({...event,guideId:99});await settle(h);const p=component(h,GuideEventEditor);expect(p.missingExhibition()).toBe(true);p.save(form);const r=http.expectOne('/api/events/7');expect(r.request.method).toBe('PUT');expect(r.request.body.guideId).toBe(99);expect(r.request.body.exhibitionId).toBe(2);r.flush(event);await settle(h);expect(p.notice()).toContain('sačuvan');
  });
  it('requires deletion confirmation and retains events on 409',async()=>{
    role('GUIDE');const h=await RouterTestingHarness.create();await h.navigateByUrl('/guide/events');http.expectOne('/api/events').flush([event]);await settle(h);const p=component(h,EventsPage);p.remove(7);http.expectNone('/api/events/7');p.pending.set(7);p.remove(7);p.remove(7);http.expectOne('/api/events/7').flush(null,{status:409,statusText:'Conflict'});await settle(h);expect(p.events().length).toBe(1);expect(p.error()).toContain('povezane prijave');
  });
  it('loads a private registration list and updates only status',async()=>{
    role('GUIDE');const h=await RouterTestingHarness.create();await h.navigateByUrl('/guide/events/7/registrations');http.expectOne('/api/events').flush([event]);await vi.waitFor(()=>{http.expectOne('/api/events/7').flush(event);http.expectOne('/api/events/7/registrations').flush([{visitorId:4,eventId:7,status:'PRIJAVLJEN',registeredAt:event.start}]);});await settle(h);
    const p=component(h,GuideRegistrations);p.draft[4]='PRISUSTVOVAO';p.save(p.rows()[0]);p.save(p.rows()[0]);const r=http.expectOne('/api/events/7/registrations/4');expect(r.request.body).toEqual({status:'PRISUSTVOVAO'});r.flush(null,{status:409,statusText:'Conflict'});await settle(h);expect(p.rows()[0].status).toBe('PRIJAVLJEN');expect(p.error()).toContain('Izmena nije moguća');
  });
  it('lets anonymous visitors read events without loading private registrations',async()=>{
    const h=await RouterTestingHarness.create();await h.navigateByUrl('/events/7');detailResponse();await settle(h);expect(h.routeNativeElement?.textContent).toContain('Prijavi se na nalog');http.expectNone('/api/visitors/me/registrations');
  });
  it('registers the current visitor only once and refreshes status',async()=>{
    role('VISITOR');const h=await RouterTestingHarness.create();await h.navigateByUrl('/events/7');detailResponse();await settle(h);const p=component(h,EventDetail);p.register();p.register();const r=http.expectOne('/api/events/7/registrations');expect(r.request.body).toEqual({});const saved={visitorId:4,eventId:7,status:'PRIJAVLJEN',registeredAt:event.start};r.flush(saved);await vi.waitFor(()=>detailResponse([saved]));await settle(h);expect(p.registration()?.status).toBe('PRIJAVLJEN');expect(p.notice()).toContain('Uspešno');
  });
  it('cancels only the current registration after confirmation',async()=>{
    role('VISITOR');const h=await RouterTestingHarness.create();await h.navigateByUrl('/events/7');const r={visitorId:4,eventId:7,status:'PRIJAVLJEN',registeredAt:event.start};detailResponse([r]);await settle(h);const p=component(h,EventDetail);p.cancel();http.expectNone('/api/events/7/registrations/me');p.confirmCancel.set(true);p.cancel();p.cancel();const request=http.expectOne('/api/events/7/registrations/me');expect(request.request.method).toBe('DELETE');request.flush(null);await vi.waitFor(()=>detailResponse([{...r,status:'OTKAZAN'}]));await settle(h);expect(p.registration()?.status).toBe('OTKAZAN');
  });
  it('does not offer cancellation of recorded attendance',async()=>{
    role('VISITOR');const h=await RouterTestingHarness.create();await h.navigateByUrl('/events/7');detailResponse([{visitorId:4,eventId:7,status:'PRISUSTVOVAO',registeredAt:event.start}]);await settle(h);expect(h.routeNativeElement?.textContent).toContain('Prisustvo je evidentirano');const p=component(h,EventDetail);p.confirmCancel.set(true);p.cancel();http.expectNone('/api/events/7/registrations/me');
  });
  it('fails closed when private status cannot load',async()=>{
    role('VISITOR');const h=await RouterTestingHarness.create();await h.navigateByUrl('/events/7');http.expectOne('/api/events/7').flush(event);http.expectOne('/api/exhibitions').flush([]);http.expectOne('/api/visitors/me/registrations').flush(null,{status:503,statusText:'Unavailable'});await settle(h);const p=component(h,EventDetail);p.register();http.expectNone('/api/events/7/registrations');expect(p.registrationLoaded()).toBe(false);
  });
  it('closes registration for full, cancelled, finished and started events',()=>{
    expect(registrationOpen(event)).toBe(true);for(const status of ['POPUNJEN','OTKAZAN','ZAVRSEN'] as const)expect(registrationOpen({...event,status})).toBe(false);expect(registrationOpen({...event,registeredCount:2})).toBe(false);expect(registrationOpen({...event,start:'2000-01-01T00:00:00Z'})).toBe(false);expect(registrationOpen(event,Date.parse(event.start))).toBe(false);
  });
  it('replaces pending detail requests and cancellation state when the event URL changes',async()=>{
    role('VISITOR');const h=await RouterTestingHarness.create();await h.navigateByUrl('/events/7');
    const oldEvent=http.expectOne('/api/events/7'),oldExhibitions=http.expectOne('/api/exhibitions'),oldRegistrations=http.expectOne('/api/visitors/me/registrations');
    const previous=component(h,EventDetail);previous.confirmCancel.set(true);
    await h.navigateByUrl('/events/9');
    http.expectOne('/api/events/9').flush({...event,id:9,name:'Drugi događaj'});http.expectOne('/api/exhibitions').flush([]);http.expectOne('/api/visitors/me/registrations').flush([]);
    await settle(h);const current=component(h,EventDetail);expect(current).not.toBe(previous);expect(current.confirmCancel()).toBe(false);
    oldEvent.flush(event);oldExhibitions.flush([]);oldRegistrations.flush([]);await settle(h);
    expect(current.event()?.id).toBe(9);expect(h.routeNativeElement?.textContent).toContain('Drugi događaj');
    current.register();http.expectOne('/api/events/9/registrations').flush(null,{status:409,statusText:'Conflict'});await settle(h);
  });
  it('loads a fresh guide draft after changing the edit URL',async()=>{
    role('GUIDE');const h=await RouterTestingHarness.create();
    for(const id of [7,9]){
      await h.navigateByUrl('/guide/events/'+id+'/edit');http.expectOne('/api/exhibitions').flush([{id:2,name:'Izložba'}]);http.expectOne('/api/guides/me').flush(guide);http.expectOne('/api/events/'+id).flush({...event,id,name:'Događaj '+id});await settle(h);
    }
    const p=component(h,GuideEventEditor);expect(p.id).toBe(9);expect(p.name).toBe('Događaj 9');p.save(form);http.expectOne('/api/events/9').flush({...event,id:9});await settle(h);
  });
  it('does not turn an invalid edit ID into a new event form',async()=>{
    role('GUIDE');const h=await RouterTestingHarness.create();await h.navigateByUrl('/guide/events/invalid/edit');await settle(h);
    const p=component(h,GuideEventEditor);expect(p.ready()).toBe(false);expect(p.error()).toContain('nije pronađen');p.save(form);http.expectNone('/api/events');http.expectNone('/api/guides/me');
  });
  it('shows attendee names only in the protected guide list',async()=>{
    role('GUIDE');const h=await RouterTestingHarness.create();await h.navigateByUrl('/guide/events/7/registrations');http.expectOne('/api/events').flush([event]);
    await vi.waitFor(()=>{http.expectOne('/api/events/7').flush(event);http.expectOne('/api/events/7/registrations').flush([{visitorId:4,eventId:7,status:'PRIJAVLJEN',registeredAt:event.start,firstName:'Ana',lastName:'Petrović'}]);});await settle(h);
    expect(h.routeNativeElement?.textContent).toContain('Ana Petrović');
  });
});
