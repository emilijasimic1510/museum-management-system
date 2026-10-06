import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRouteSnapshot, provideRouter, Router, RouterStateSnapshot } from '@angular/router';
import { Auth, visitorGuard } from '../core/auth';
import { managerGuard, ManagerDashboard } from '../core/manager';
import { ManagerPage } from './manager';

function report():ManagerDashboard {return {
  from:'2035-01-01',to:'2035-01-31',zone:'Europe/Belgrade',generatedAt:'2035-02-01T12:00:00Z',
  attendance:{total:2,museums:[{key:'1',label:'Muzej test',count:2}],exhibitions:[],terms:[],days:[]},
  revenue:{total:16,paidTicketValue:16,exhibitions:[{key:'4',label:'Izložba test',amount:16}],categories:[],methods:[{key:'GOTOVINA',label:'GOTOVINA',amount:16}],days:[]},
  tickets:[{key:'KUPLJENA',label:'KUPLJENA',count:2}],popularity:[{participationId:8,exhibitId:5,exhibit:'Eksponat test',exhibition:'Izložba test',scans:3,visits:2,averageSeconds:null,measuredScans:0}],
  events:[{id:7,name:'Tura test',start:'2035-01-03T12:00Z',status:'PLANIRAN',capacity:4,registrations:3,registered:1,attended:1,cancelled:1,occupancyPercent:50}],
  ratings:[{exhibitionId:4,exhibition:'Izložba test',count:2,average:4.5}],comments:[{id:6,exhibition:'Izložba test',rating:4,comment:'<script>plain text</script>',ratedAt:'2035-01-02T10:00Z'}],commentsTruncated:false,
  transitions:[{key:'8>9',label:'Postavka 8 → 9',count:1}],contentInventory:[{participationId:8,exhibit:'Eksponat test',exhibition:'Izložba test',language:'sr',type:'TEKST',records:2}],limitations:['Nema evidencije play/view događaja.']
};}
describe('Manager dashboard',()=>{
  let http:HttpTestingController;
  beforeEach(()=>{vi.useFakeTimers();sessionStorage.clear();TestBed.configureTestingModule({providers:[provideHttpClient(),provideHttpClientTesting(),provideRouter([])]});http=TestBed.inject(HttpTestingController);});
  afterEach(()=>{http.verify();sessionStorage.clear();vi.useRealTimers();});
  async function render(f:ComponentFixture<ManagerPage>){await vi.advanceTimersByTimeAsync(0);f.detectChanges();await vi.advanceTimersByTimeAsync(0);f.detectChanges();}
  function pending(){return http.expectOne(r=>r.url.startsWith('/api/manager/dashboard?'));}
  it('renders real aggregates, sections and comments as text',async()=>{
    const f=TestBed.createComponent(ManagerPage);pending().flush(report());await render(f);
    expect(f.nativeElement.textContent).toContain('Muzej test');expect(f.nativeElement.textContent).toContain('16,00');
    expect(f.nativeElement.textContent).toContain('Tura test');expect(f.nativeElement.textContent).toContain('Popularnost postavki');expect(f.nativeElement.textContent).toContain('Nije evidentirano');
    expect(f.nativeElement.textContent).toContain('Postavka 8 → 9');expect(f.nativeElement.textContent).toContain('Digitalni sadržaj — inventar');expect(f.nativeElement.textContent).toContain('4,50');
    expect(f.nativeElement.textContent).toContain('<script>plain text</script>');expect(f.nativeElement.querySelector('script')).toBeNull();
    expect(f.componentInstance.ticketCount()).toBe(2);expect(f.componentInstance.empty()).toBe(false);
  });
  it('submits date filters and rejects reversed dates without a request',async()=>{
    const f=TestBed.createComponent(ManagerPage);pending().flush(report());await render(f);
    for(const [name,value] of [['from','2035-02-01'],['to','2035-02-28']]){const input:HTMLInputElement=f.nativeElement.querySelector(`[name="${name}"]`);input.value=value;input.dispatchEvent(new Event('input',{bubbles:true}));}
    await render(f);f.nativeElement.querySelector('form').dispatchEvent(new Event('submit',{bubbles:true,cancelable:true}));
    const req=http.expectOne('/api/manager/dashboard?from=2035-02-01&to=2035-02-28');expect(req.request.method).toBe('GET');req.flush({...report(),from:'2035-02-01',to:'2035-02-28'});await render(f);
    expect(f.nativeElement.textContent).toContain('28.02.2035.');f.componentInstance.from='2035-03-01';f.componentInstance.load();await render(f);
    expect(f.nativeElement.textContent).toContain('datum od ne sme biti posle');http.expectNone(r=>r.url.startsWith('/api/manager'));
  });
  it('shows loading, safe error, retry and empty state',async()=>{
    const f=TestBed.createComponent(ManagerPage);await render(f);expect(f.componentInstance.busy()).toBe(true);expect(f.nativeElement.querySelector('button').disabled).toBe(true);
    pending().flush({message:'SECRET SQL EXCEPTION'},{status:500,statusText:'Error'});await render(f);
    expect(f.componentInstance.busy()).toBe(false);expect(f.nativeElement.textContent).toContain('Pokušaj ponovo');expect(f.nativeElement.textContent).not.toContain('SECRET');
    const retry=[...f.nativeElement.querySelectorAll('button')].find((b:any)=>b.textContent.includes('Pokušaj ponovo')) as HTMLButtonElement;retry.click();
    const empty=report();empty.attendance={total:0,museums:[],exhibitions:[],terms:[],days:[]};empty.revenue={total:0,paidTicketValue:0,exhibitions:[],categories:[],methods:[],days:[]};empty.tickets=[];empty.events=[];empty.popularity=[];empty.ratings=[];empty.comments=[];empty.transitions=[];
    pending().flush(empty);await render(f);expect(f.nativeElement.textContent).toContain('Nema evidentirane aktivnosti');expect(f.nativeElement.textContent).toContain('Digitalni sadržaj — inventar');
  });
  it('ends loading on timeout',async()=>{
    const f=TestBed.createComponent(ManagerPage);const req=pending();await vi.advanceTimersByTimeAsync(15001);f.detectChanges();
    expect(req.cancelled).toBe(true);expect(f.componentInstance.busy()).toBe(false);expect(f.componentInstance.error()).not.toBe('');
  });
  for(const role of ['MANAGER','ADMINISTRATOR','CURATOR','GUIDE','CASHIER','VISITOR'])it('checks the actual role: '+role,async()=>{
    const auth=TestBed.inject(Auth);auth.session.set({token:'test',expiresAt:Date.now()+60000});
    auth.currentAccount.set({id:1,email:'test@example.test',active:true,roles:[role]} as any);
    const result=await TestBed.runInInjectionContext(()=>managerGuard({} as ActivatedRouteSnapshot,{url:'/manager'} as RouterStateSnapshot));
    if(role==='MANAGER')expect(result).toBe(true);else expect(TestBed.inject(Router).serializeUrl(result as any)).toBe('/forbidden');
  });
  it('redirects anonymous users to login and manager from visitor landing to dashboard',async()=>{
    const router=TestBed.inject(Router),auth=TestBed.inject(Auth);
    const anon=await TestBed.runInInjectionContext(()=>managerGuard({} as ActivatedRouteSnapshot,{url:'/manager'} as RouterStateSnapshot));expect(router.serializeUrl(anon as any)).toContain('/login?returnUrl=%2Fmanager');
    auth.session.set({token:'test',expiresAt:Date.now()+60000});auth.currentAccount.set({id:1,email:'manager@example.test',roles:['MANAGER']} as any);
    const landing=await TestBed.runInInjectionContext(()=>visitorGuard({data:{publicCatalog:true}} as unknown as ActivatedRouteSnapshot,{url:'/'} as RouterStateSnapshot));expect(router.serializeUrl(landing as any)).toBe('/manager');
  });
});
