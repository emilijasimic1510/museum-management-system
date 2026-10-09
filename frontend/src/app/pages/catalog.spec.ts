import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { Auth, authInterceptor } from '../core/auth';
import { CatalogResource } from '../core/catalog';
import { routes } from '../routes';
import { App } from '../app';
import { ExhibitsPage } from './exhibits';
import { CatalogPage } from './catalog';

const exhibit={id:1,name:'Tišina',description:'Opis zbirke',type:'SLIKA',material:'Ulje',origin:'Srbija',creationPeriod:'2024',authors:[{id:2,name:'Mila Jovanović',contributionType:'Autor'}]};
const author={id:2,firstName:'Mila',lastName:'Jovanović',biography:'Biografija',birthYear:1988,deathYear:null};

describe('Public digital catalog',()=>{
  let requests:HttpTestingController;
  beforeEach(()=>{
    sessionStorage.clear();
    TestBed.configureTestingModule({providers:[provideRouter(routes),provideHttpClient(withInterceptors([authInterceptor])),provideHttpClientTesting()]});
    requests=TestBed.inject(HttpTestingController);
  });
  afterEach(()=>{requests.verify();sessionStorage.clear();});
  function role(value:string){const auth=TestBed.inject(Auth);auth.session.set({token:'test-only',expiresAt:Date.now()+60000});auth.currentAccount.set({id:1,email:'test@example.test',roles:[value],active:true,registeredAt:'',lastLogin:null});}
  async function render(harness:RouterTestingHarness){await Promise.resolve();await Promise.resolve();harness.detectChanges();}

  it('requests active browse data and keeps sold-out exhibitions linked to their details',async()=>{
    const fixture=TestBed.createComponent(CatalogPage);
    requests.expectOne('/api/museums').flush([]);
    requests.expectOne('/api/halls').flush([]);
    const exhibition={id:3,name:'Future exhibition',onlineVisible:true,status:'PLANIRANA',start:'2099-01-01T10:00:00Z',end:'2099-01-02T10:00:00Z',terms:[{id:4,status:'POPUNJEN',capacity:1,start:'2099-01-01T10:00:00Z',end:'2099-01-01T11:00:00Z'}]};
    requests.expectOne('/api/exhibitions?browse=true').flush([exhibition,{...exhibition,id:5,name:'Past exhibition',end:'2000-01-01T10:00:00Z'}]);
    await fixture.whenStable();fixture.detectChanges();
    expect(fixture.nativeElement.textContent).not.toContain('Past exhibition');
    expect(fixture.nativeElement.textContent).toContain('Nema mesta');
    expect(fixture.nativeElement.querySelector('h3 a').getAttribute('href')).toBe('/exhibitions/3');
  });

  for(const signedIn of [false,true])it(`allows ${signedIn?'visitor':'anonymous'} list and detail without exposing private fields`,async()=>{
    if(signedIn)role('VISITOR');
    const harness=await RouterTestingHarness.create();
    await harness.navigateByUrl('/exhibits');
    const list=requests.expectOne('/api/exhibits');expect(list.request.headers.has('Authorization')).toBe(false);
    list.flush([{...exhibit,qrToken:'PRIVATE-TEST',qrPath:'PRIVATE-PATH',inventoryNumber:'PRIVATE-INVENTORY'}]);await render(harness);
    expect(harness.routeNativeElement?.textContent).toContain('Tišina');
    expect(harness.routeNativeElement?.textContent).not.toContain('PRIVATE-');
    await harness.navigateByUrl('/exhibits/1');
    requests.expectOne('/api/exhibits/1').flush(exhibit);
    requests.expectOne('/api/exhibits/1/participations').flush([{exhibit:{id:1,name:'Tišina'},exhibition:{id:3,name:'Tragovi svetlosti'},contextDescription:'Kontekst'}]);await render(harness);
    if(signedIn){const access=requests.expectOne('/api/visits/me/exhibits/1/content');expect(access.request.headers.has('Authorization')).toBe(true);access.flush([]);await render(harness);}
    expect(harness.routeNativeElement?.textContent).toContain('Mila Jovanović');
    expect(harness.routeNativeElement?.textContent).toContain('Tragovi svetlosti');
    expect(harness.routeNativeElement?.textContent).toContain('Kontekst');
    expect(harness.routeNativeElement?.querySelector('[href="/scan"]')).toBeNull();
  });

  it('shows public exhibitions on cards, limits long lists and explains missing exhibitions',async()=>{
    const harness=await RouterTestingHarness.create();await harness.navigateByUrl('/exhibits');
    requests.expectOne('/api/exhibits').flush([
      {...exhibit,exhibitions:[{id:3,name:'Grad kroz objektiv'},{id:4,name:'Tragovi svetlosti'}]},
      {...exhibit,id:2,name:'Bez postavke',exhibitions:[]},
      {...exhibit,id:3,name:'Mnogo izložbi',exhibitions:Array.from({length:8},(_,i)=>({id:10+i,name:'Izložba '+i}))}
    ]);await render(harness);
    const cards=harness.routeNativeElement!.querySelectorAll('.catalog-card');
    expect(cards[0].querySelectorAll('.card-exhibitions li').length).toBe(2);
    expect(cards[0].textContent).toContain('Grad kroz objektiv');
    expect(cards[0].textContent).toContain('Tragovi svetlosti');
    expect(cards[0].querySelector('a[href="/exhibitions/3"]')).not.toBeNull();
    expect(cards[1].textContent).toContain('Trenutno nije na javno objavljenim izložbama.');
    expect(cards[2].querySelectorAll('.card-exhibitions li').length).toBe(3);
    expect(cards[2].textContent).toContain('Još 5 izložbi');
    expect(cards[2].querySelector('.card-exhibitions a[href="/exhibits/3"]')).not.toBeNull();
  });
  it('separates the contexts of one exhibit by exhibition on the public detail',async()=>{
    const harness=await RouterTestingHarness.create();await harness.navigateByUrl('/exhibits/1');
    requests.expectOne('/api/exhibits/1').flush(exhibit);
    requests.expectOne('/api/exhibits/1/participations').flush([
      {exhibit:{id:1,name:'Tišina'},exhibition:{id:3,name:'Grad kroz objektiv'},contextDescription:'Gradski kontekst'},
      {exhibit:{id:1,name:'Tišina'},exhibition:{id:4,name:'Tragovi svetlosti'},contextDescription:'Svetlosni kontekst'}
    ]);await render(harness);
    const blocks=Array.from(harness.routeNativeElement!.querySelectorAll('article.route-block'));
    expect(blocks.length).toBe(2);
    expect(blocks[0].textContent).toContain('Grad kroz objektiv');
    expect(blocks[0].textContent).toContain('Gradski kontekst');
    expect(blocks[0].textContent).not.toContain('Svetlosni kontekst');
    expect(blocks[1].textContent).toContain('Tragovi svetlosti');
    expect(blocks[1].textContent).toContain('Svetlosni kontekst');
  });
  it('searches and combines filters locally, resets them, and renders no results',async()=>{
    const fixture=TestBed.createComponent(ExhibitsPage);fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Učitavanje');
    requests.expectOne('/api/exhibits').flush([exhibit,{...exhibit,id:2,name:'Bronza',type:'SKULPTURA',material:'Metal',creationPeriod:'2023'}]);
    await fixture.whenStable();fixture.detectChanges();
    const page=fixture.componentInstance;page.query.set(' TIŠ ');page.type.set('SLIKA');page.material.set('Ulje');page.period.set('2024');
    expect(page.filtered().map(e=>e.id)).toEqual([1]);
    page.period.set('2023');fixture.detectChanges();expect(fixture.nativeElement.textContent).toContain('Nema rezultata');
    page.reset();expect(page.filtered().length).toBe(2);requests.expectNone('/api/exhibits');
  });
  it('renders an empty list',async()=>{
    const harness=await RouterTestingHarness.create();await harness.navigateByUrl('/exhibits');
    requests.expectOne('/api/exhibits').flush([]);await render(harness);
    expect(harness.routeNativeElement?.textContent).toContain('Katalog se priprema');
  });
  it('renders exhibit 404 and never redirects',async()=>{
    const harness=await RouterTestingHarness.create();await harness.navigateByUrl('/exhibits/999');
    requests.expectOne('/api/exhibits/999').flush(null,{status:404,statusText:'Not Found'});
    requests.expectOne('/api/exhibits/999/participations').flush(null,{status:404,statusText:'Not Found'});await render(harness);
    expect(harness.routeNativeElement?.querySelector('h1')?.textContent).toBe('Eksponat nije pronađen');
    expect(TestBed.inject(Router).url).toBe('/exhibits/999');
  });
  for(const status of [401,403,500])it(`handles ${status} as a retryable public error without a login loop`,async()=>{
    role('VISITOR');const harness=await RouterTestingHarness.create();await harness.navigateByUrl('/exhibits');
    requests.expectOne('/api/exhibits').flush(null,{status,statusText:'Test error'});await render(harness);
    expect(harness.routeNativeElement?.querySelector('[role="alert"]')).not.toBeNull();
    expect(TestBed.inject(Router).url).toBe('/exhibits');expect(TestBed.inject(Auth).session()).not.toBeNull();
    harness.routeNativeElement?.querySelector('button')?.click();
    requests.expectOne('/api/exhibits').flush([exhibit]);await render(harness);
    expect(harness.routeNativeElement?.textContent).toContain('Tišina');
  });
  it('loads author list/detail with no per-exhibit requests',async()=>{
    const harness=await RouterTestingHarness.create();await harness.navigateByUrl('/authors');
    requests.expectOne('/api/authors').flush([author]);await render(harness);
    expect(harness.routeNativeElement?.textContent).toContain('Mila');
    await harness.navigateByUrl('/authors/2');requests.expectOne('/api/authors/2').flush(author);await render(harness);
    expect(harness.routeNativeElement?.textContent).toContain('Biografija');
    requests.expectNone('/api/exhibits');
    await harness.navigateByUrl('/authors/3');requests.expectOne('/api/authors/3').flush({...author,id:3,firstName:'Luka'});await render(harness);
    expect(harness.routeNativeElement?.querySelector('h1')?.textContent).toContain('Luka');
    await harness.navigateByUrl('/authors/999');requests.expectOne('/api/authors/999').flush(null,{status:404,statusText:'Not Found'});await render(harness);
    expect(harness.routeNativeElement?.querySelector('h1')?.textContent).toBe('Autor nije pronađen');
  });
  it('keeps exhibit detail when participation loading fails',async()=>{
    const harness=await RouterTestingHarness.create();await harness.navigateByUrl('/exhibits/1');
    requests.expectOne('/api/exhibits/1').flush(exhibit);
    requests.expectOne('/api/exhibits/1/participations').flush(null,{status:500,statusText:'Test'});await render(harness);
    expect(harness.routeNativeElement?.querySelector('h1')?.textContent).toBe('Tišina');
    expect(harness.routeNativeElement?.textContent).toContain('Ponovi učitavanje izložbi');
  });
  it('redirects cashier away from all catalog routes',async()=>{
    role('CASHIER');const harness=await RouterTestingHarness.create();
    for(const path of ['/exhibits','/exhibits/1','/authors','/authors/2']) {
      await harness.navigateByUrl(path);expect(TestBed.inject(Router).url).toBe('/cashier');
      if(path==='/exhibits'){requests.expectOne('/api/exhibitions').flush([]);await render(harness);}
    }
    requests.expectNone('/api/exhibits');
  });
  for(const value of ['anonymous','VISITOR','CASHIER'])it(`shows the correct catalog navigation for ${value}`,async()=>{
    if(value!=='anonymous')role(value);
    const fixture=TestBed.createComponent(App);fixture.detectChanges();await fixture.whenStable();
    expect(!!fixture.nativeElement.querySelector('nav a[href="/exhibits"]')).toBe(value!=='CASHIER');
    expect(!!fixture.nativeElement.querySelector('nav a[href="/cashier"]')).toBe(value==='CASHIER');
  });
  it('ignores a stale response after switching resource ids',async()=>{
    const resource=new CatalogResource<string>();let finish!:(v:string)=>void;
    const first=resource.load(()=>new Promise(resolve=>finish=resolve));
    await resource.load(()=>Promise.resolve('new'));finish('old');await first;
    expect(resource.data()).toBe('new');
  });
});
