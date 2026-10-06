import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { provideRouter, Router, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { QrBoard } from './qr-board';
import { I18n } from '../core/i18n';
import { Auth, visitorGuard } from '../core/auth';
import { qrBoardGuard } from '../core/qr-board';
import { scanUrl } from '../core/scan';
import { routes } from '../routes';
import { toDataURL } from 'qrcode';
vi.mock('qrcode',()=>({toDataURL:vi.fn().mockResolvedValue('data:image/png;base64,TEST')}));
const runtime=globalThis as typeof globalThis & {__MUSEUM_CONFIG__?:{publicBaseUrl?:string}};
describe('Printable QR board',()=>{
  let http:HttpTestingController;
  beforeEach(()=>{delete runtime.__MUSEUM_CONFIG__;sessionStorage.clear();localStorage.clear();TestBed.configureTestingModule({providers:[provideRouter([]),provideHttpClient(),provideHttpClientTesting()]});http=TestBed.inject(HttpTestingController);vi.clearAllMocks();});
  afterEach(()=>{http.verify();delete runtime.__MUSEUM_CONFIG__;sessionStorage.clear();localStorage.clear();});
  async function settle(f:any){f.detectChanges();await f.whenStable();f.detectChanges();}
  async function setup(){
    const f=TestBed.createComponent(QrBoard);f.detectChanges();
    http.expectOne('/api/museums').flush([{id:1,name:'Museum A'},{id:2,name:'Museum B'}]);
    http.expectOne('/api/halls').flush([{id:10,museumId:1},{id:20,museumId:2}]);
    http.expectOne('/api/exhibitions').flush([{id:11,hallId:10,name:'Exhibition A'},{id:22,hallId:20,name:'Exhibition B'}]);
    http.expectOne('/api/exhibits').flush([{id:7,name:'Exhibit A'},{id:8,name:'Exhibit B'}]);await settle(f);return f;
  }
  function part(id:number,extra:any={}){return {id,exhibitionId:11,exhibitId:7,installedAt:null,removedAt:null,tourOrder:id,qrToken:'opaque-'+id,...extra};}
  async function choose(f:any,parts:any[]){f.componentInstance.changeMuseum(1);const pending=f.componentInstance.changeExhibition(11);http.expectOne('/api/exhibit-participations?exhibitionId=11').flush(parts);await pending;await settle(f);}
  it('filters museums, exhibition and active installation dates; changing selection clears old print data',async()=>{
    const f=await setup(),p=f.componentInstance;
    await choose(f,[part(1),part(2,{exhibitionId:22}),part(3,{installedAt:'2999-01-01T00:00:00Z'}),part(4,{removedAt:'2000-01-01T00:00:00Z'})]);
    expect(p.exhibitionsForMuseum().map(e=>e.id)).toEqual([11]);expect(p.cards().map(c=>c.id)).toEqual([1]);
    p.selectAll();p.preview();await settle(f);expect(p.pages().flat()).toHaveLength(1);
    p.changeMuseum(2);expect(p.selected().size).toBe(0);expect(p.pages()).toEqual([]);expect(p.exhibitionsForMuseum().map(e=>e.id)).toEqual([22]);
  });
  it('selects all, toggles and clears; paginates four cards and hides tokens and IDs',async()=>{
    const f=await setup(),p=f.componentInstance;await choose(f,[1,2,3,4,5].map(id=>part(id)));
    p.selectAll();expect(p.selected().size).toBe(5);p.toggle(2);expect(p.selected().has(2)).toBe(false);p.toggle(2);p.preview();await settle(f);
    expect(p.pages().map(page=>page.length)).toEqual([4,1]);
    expect(f.nativeElement.querySelectorAll('.qr-board-card')).toHaveLength(5);
    expect(f.nativeElement.textContent).not.toContain('opaque-');expect(f.nativeElement.textContent).not.toContain('/api/');
    p.clear();await settle(f);expect(p.pages()).toEqual([]);expect(p.printReady()).toBe(false);
  });
  it('warns when publicBaseUrl is missing and translates all card labels',async()=>{
    const f=await setup();await choose(f,[part(1)]);f.componentInstance.selectAll();f.componentInstance.preview();await settle(f);
    expect(f.nativeElement.textContent).toContain('Javna adresa aplikacije nije podešena');
    expect(f.nativeElement.textContent).toContain('Skenirajte QR kod za digitalni sadržaj');
    TestBed.inject(I18n).language.set('en');await settle(f);
    expect(f.nativeElement.textContent).toContain('Scan the QR code for digital content');expect(f.nativeElement.textContent).toContain('Exhibit A');
    expect(f.nativeElement.textContent).toContain('Select all');
  });
  it('reuses the QR component and configurable encoded URL; waits for images before printing',async()=>{
    runtime.__MUSEUM_CONFIG__={publicBaseUrl:'https://museum.example/'};
    expect(scanUrl('a+b/opaque')).toBe('https://museum.example/scan/a%2Bb%2Fopaque');
    const f=await setup(),p=f.componentInstance;await choose(f,[part(1,{qrToken:'a+b/opaque'})]);p.selectAll();p.preview();await settle(f);
    expect(f.nativeElement.textContent).not.toContain('Javna adresa aplikacije nije podešena');
    await vi.waitFor(()=>expect(toDataURL).toHaveBeenCalledWith('https://museum.example/scan/a%2Bb%2Fopaque',expect.any(Object)));
    const print=vi.spyOn(window,'print').mockImplementation(()=>{});p.print();expect(print).not.toHaveBeenCalled();
    f.nativeElement.querySelector('img').dispatchEvent(new Event('load'));await settle(f);p.print();expect(print).toHaveBeenCalledOnce();
    p.preview();await settle(f);expect(p.printReady()).toBe(false);print.mockRestore();
  });
  it('print styles hide the application shell and controls and preserve cards',async()=>{
    const f=await setup();await settle(f);
    const styles=Array.from(document.querySelectorAll('style')).map(s=>s.textContent).join('\n').replace(/\s+/g,'');
    expect(styles).toContain('@mediaprint');expect(styles).toContain('app-root>header');expect(styles).toContain('app-root>footer');
    expect(styles).toContain('.qr-board-controls');expect(styles).toContain('display:none!important');expect(styles).toContain('break-inside:avoid');
  });
  it('requires login, allows only curator/admin and leaves scan authorization intact',async()=>{
    const auth=TestBed.inject(Auth),router=TestBed.inject(Router);const state={url:'/staff/qr-board'} as RouterStateSnapshot;
    const check=()=>TestBed.runInInjectionContext(()=>qrBoardGuard({} as ActivatedRouteSnapshot,state));
    expect(router.serializeUrl(await check() as any)).toBe('/login?returnUrl=%2Fstaff%2Fqr-board');
    auth.session.set({token:'test',expiresAt:Date.now()+60000});
    for(const role of ['CURATOR','ADMINISTRATOR','VISITOR','MANAGER','CASHIER','GUIDE']){
      auth.currentAccount.set({id:1,email:'staff@example.test',active:true,registeredAt:'',lastLogin:null,roles:[role]});
      const result=await check();expect(result===true).toBe(['CURATOR','ADMINISTRATOR'].includes(role));
    }
    expect(routes.find(r=>r.path==='scan/:token')?.canActivate).toEqual([visitorGuard]);
    expect(routes.find(r=>r.path==='staff/qr-board')?.canActivate).toEqual([qrBoardGuard]);
  });
});
