import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute, ActivatedRouteSnapshot, convertToParamMap, provideRouter, Router, RouterStateSnapshot } from '@angular/router';
import { of } from 'rxjs';
import { Auth } from '../core/auth';
import { collectionGuard, localTimestamp } from '../core/collection-management';
import { CollectionEditor } from './collection-editor';
import { CollectionList } from './collection-list';
import { CollectionPrices } from './collection-prices';

describe('Spaces, exhibitions and prices management',()=>{
  let http:HttpTestingController;
  function setup(kind='museums',mode='edit',id='3',query:Record<string,string>={}){
    TestBed.configureTestingModule({providers:[provideHttpClient(),provideHttpClientTesting(),provideRouter([]),
      {provide:ActivatedRoute,useValue:{snapshot:{data:{kind,mode},paramMap:convertToParamMap({id}),queryParamMap:convertToParamMap(query)},queryParamMap:of(convertToParamMap(query))}}]});
    http=TestBed.inject(HttpTestingController);
  }
  beforeEach(()=>{vi.useFakeTimers();sessionStorage.clear();});
  afterEach(()=>{http?.verify();vi.useRealTimers();sessionStorage.clear();});
  async function render<T>(f:ComponentFixture<T>){await vi.advanceTimersByTimeAsync(0);f.detectChanges();await vi.advanceTimersByTimeAsync(0);f.detectChanges();}
  function input<T>(f:ComponentFixture<T>,name:string,value:string){const el:HTMLInputElement=f.nativeElement.querySelector(`[name="${name}"]`);el.value=value;el.dispatchEvent(new Event('input',{bubbles:true}));}
  function select<T>(f:ComponentFixture<T>,name:string,index:number){const el:HTMLSelectElement=f.nativeElement.querySelector(`[name="${name}"]`);el.selectedIndex=index;el.dispatchEvent(new Event('change',{bubbles:true}));}
  function submit<T>(f:ComponentFixture<T>){f.nativeElement.querySelector('form').dispatchEvent(new Event('submit',{bubbles:true,cancelable:true}));}

  it('edits a museum and deactivates through PUT while preserving its history',async()=>{
    setup();const f=TestBed.createComponent(CollectionEditor);
    const museum={id:3,name:'Muzej',address:'Adresa',foundationYear:1900,description:'Opis',active:true};
    http.expectOne('/api/museums/3').flush(museum);await render(f);
    input(f,'name','Novo ime');await render(f);submit(f);
    const put=http.expectOne('/api/museums/3');expect(put.request.method).toBe('PUT');expect(put.request.body.name).toBe('Novo ime');put.flush({...museum,name:'Novo ime'});await render(f);
    f.componentInstance.confirm.set(true);f.componentInstance.remove();
    const deactivate=http.expectOne('/api/museums/3');expect(deactivate.request.method).toBe('PUT');expect(deactivate.request.body.active).toBe(false);
    deactivate.flush({...museum,name:'Novo ime',active:false});await render(f);expect(f.nativeElement.textContent).toContain('Istorijski podaci su sačuvani');
  });
  it('requires positive hall dimensions and keeps the museum relationship',async()=>{
    setup('halls');const f=TestBed.createComponent(CollectionEditor);
    http.expectOne('/api/halls/3').flush({id:3,museumId:1,name:'Sala',area:40,floor:1,capacity:20});http.expectOne('/api/museums').flush([{id:1,name:'Muzej'}]);await render(f);
    input(f,'area','0');input(f,'capacity','-1');await render(f);submit(f);http.expectNone(r=>r.method==='PUT');
    input(f,'area','55.5');input(f,'capacity','25');await render(f);submit(f);
    const req=http.expectOne('/api/halls/3');expect(req.request.body).toMatchObject({museumId:1,area:55.5,capacity:25});req.flush({id:3,...req.request.body});await render(f);
  });
  it('creates an exhibition with its required first term through the existing endpoint',async()=>{
    setup('exhibitions','new');const f=TestBed.createComponent(CollectionEditor);http.expectOne('/api/halls').flush([{id:2,name:'Sala',museumId:1}]);await render(f);
    const navigation=vi.spyOn(TestBed.inject(Router),'navigate').mockResolvedValue(true);
    select(f,'hallId',1);input(f,'name','Nova izložba');input(f,'start','2030-01-01T10:00');input(f,'end','2030-01-02T10:00');
    input(f,'initialStart','2030-01-01T11:00');input(f,'initialEnd','2030-01-01T12:00');input(f,'initialCapacity','10');await render(f);submit(f);
    const req=http.expectOne('/api/exhibitions');expect(req.request.method).toBe('POST');expect(req.request.body).toMatchObject({hallId:2,name:'Nova izložba',status:'PLANIRANA',terms:[{capacity:10,status:'OTVOREN'}]});
    expect(req.request.body.terms[0].start).toBe(new Date('2030-01-01T11:00').toISOString());req.flush({id:8});await render(f);expect(navigation).toHaveBeenCalled();
  });
  it('preserves exact server timestamps when changing a sold term capacity or status',async()=>{
    setup('exhibition-terms');const f=TestBed.createComponent(CollectionEditor);const term={id:3,exhibitionId:5,start:'2030-01-01T10:00:12.123456Z',end:'2030-01-01T12:00:12.123456Z',capacity:20,status:'OTVOREN'};
    http.expectOne('/api/exhibition-terms/3').flush(term);http.expectOne('/api/exhibitions').flush([{id:5,name:'Izložba'}]);await render(f);
    input(f,'capacity','30');await render(f);submit(f);const req=http.expectOne('/api/exhibition-terms/3');expect(req.request.body.start).toBe(term.start);expect(req.request.body.end).toBe(term.end);req.flush({...term,capacity:30,status:'POPUNJEN'});await render(f);
    expect(f.componentInstance.model['status']).toBe('POPUNJEN');
  });
  it('displays scoped hall lists and navigates to detail and exhibitions',async()=>{
    setup('halls','view','3',{museumId:'1'});const f=TestBed.createComponent(CollectionList);http.expectOne('/api/halls').flush([{id:3,museumId:1,name:'Prva'},{id:4,museumId:2,name:'Druga'}]);await render(f);
    expect(f.nativeElement.textContent).toContain('Prva');expect(f.nativeElement.textContent).not.toContain('Druga');expect(f.nativeElement.querySelector('a[href="/collection/halls/3"]')).not.toBeNull();
  });
  it('adds a price and edits its end using the immutable encoded composite key',async()=>{
    setup();const f=TestBed.createComponent(CollectionPrices);http.expectOne('/api/exhibitions/3').flush({id:3,name:'Izložba'});http.expectOne('/api/exhibitions/3/prices/management').flush([]);await render(f);
    input(f,'category','Đaci / studenti');input(f,'validFrom','2030-01-01T10:00');input(f,'price','0');input(f,'note','Besplatan ulaz');await render(f);submit(f);
    const req=http.expectOne('/api/exhibitions/3/prices/management');expect(req.request.method).toBe('POST');expect(req.request.body.price).toBe(0);
    const saved={exhibitionId:3,...req.request.body};req.flush(saved);await render(f);
    f.componentInstance.edit(saved);await render(f);input(f,'validTo','2030-01-02T10:00');await render(f);submit(f);
    const update=http.expectOne(r=>r.method==='PUT');expect(update.request.url).toContain('category='+encodeURIComponent(saved.category));expect(update.request.url).toContain('validFrom='+encodeURIComponent(saved.validFrom));
    expect(update.request.body.category).toBeUndefined();update.flush({...saved,...update.request.body});await render(f);expect(f.nativeElement.textContent).toContain('Cenovni period je sačuvan');
  });
  it('keeps a price draft after a conflict without exposing server details',async()=>{
    setup();const f=TestBed.createComponent(CollectionPrices);http.expectOne('/api/exhibitions/3').flush({id:3,name:'Izložba'});http.expectOne('/api/exhibitions/3/prices/management').flush([]);await render(f);
    input(f,'category','ODRASLI');input(f,'validFrom','2030-01-01T10:00');input(f,'price','8');await render(f);submit(f);
    http.expectOne('/api/exhibitions/3/prices/management').flush({message:'SQL PRIVATE'},{status:409,statusText:'Conflict'});await render(f);
    expect(f.nativeElement.textContent).toContain('preklapanje perioda');expect(f.nativeElement.textContent).not.toContain('SQL PRIVATE');expect(f.componentInstance.category).toBe('ODRASLI');
  });
  for(const role of ['VISITOR','CASHIER','GUIDE','CURATOR','ADMINISTRATOR'])it(`uses existing privileges for ${role}`,async()=>{
    setup();const auth=TestBed.inject(Auth);auth.session.set({token:'test',expiresAt:Date.now()+60000});auth.currentAccount.set({id:1,email:'test',active:true,roles:[role],registeredAt:'',lastLogin:null});
    for(const adminOnly of [false,true]){
      const result=await TestBed.runInInjectionContext(()=>collectionGuard({data:{adminOnly}} as unknown as ActivatedRouteSnapshot,{url:'/collection'} as RouterStateSnapshot));
      expect(result===true).toBe(role==='ADMINISTRATOR'||role==='CURATOR'&&!adminOnly);
    }
  });
  it('redirects anonymous management access to login',async()=>{setup();const result=await TestBed.runInInjectionContext(()=>collectionGuard({data:{}} as ActivatedRouteSnapshot,{url:'/collection'} as RouterStateSnapshot));expect(String(result)).toContain('/login');});
});
