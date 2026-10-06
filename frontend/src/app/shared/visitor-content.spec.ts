import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController,provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { Auth } from '../core/auth';
import { VisitorContent } from './visitor-content';
describe('Scan to unlock visitor content',()=>{
 let http:HttpTestingController;const locked:any={participationId:10,exhibitionId:1,exhibitionName:'Izložba A',visitId:3,state:'SCAN_REQUIRED',participation:null};
 beforeEach(()=>{sessionStorage.clear();TestBed.configureTestingModule({providers:[provideHttpClient(),provideHttpClientTesting(),provideRouter([])]});http=TestBed.inject(HttpTestingController);});afterEach(()=>{http.verify();sessionStorage.clear();});
 function page(login=true){if(login)TestBed.inject(Auth).session.set({token:'test',expiresAt:Date.now()+60000});const f=TestBed.createComponent(VisitorContent);f.componentRef.setInput('exhibitId',1);f.detectChanges();return f;}async function settle(f:any){await f.whenStable();f.detectChanges();}
 it('shows the anonymous visit requirement',async()=>{const f=page(false);await settle(f);http.expectNone('/api/visits/me/exhibits/1/content');expect(f.nativeElement.textContent).toContain('Digitalni sadržaj dostupan tokom obilaska');});
 it('flushes the protected text request after an installation is unlocked',async()=>{const f=page();http.expectOne('/api/visits/me/exhibits/1/content').flush([locked]);await settle(f);f.componentInstance.load();http.expectOne('/api/visits/me/exhibits/1/content').flush([{...locked,state:'UNLOCKED',participation:{participationId:10,name:'Eksponat',contextDescription:'Kontekst A',content:[{id:1,type:'TEKST',path:'/content/a',language:'sr',version:1}]}}]);await settle(f);http.expectOne('/api/visits/3/content/10/text/1').flush({text:'Kontekst A'});await new Promise<void>(resolve=>queueMicrotask(resolve));f.detectChanges();await f.whenStable();f.detectChanges();expect(f.nativeElement.textContent).toContain('Kontekst A');});
 it('does not display stale content after exhibit changes',async()=>{const f=page();const first=http.expectOne('/api/visits/me/exhibits/1/content');f.componentRef.setInput('exhibitId',2);f.detectChanges();http.expectOne('/api/visits/me/exhibits/2/content').flush([locked]);first.flush([{...locked,state:'UNLOCKED',participation:{contextDescription:'STALE SECRET',content:[]}}]);await settle(f);expect(f.nativeElement.textContent).not.toContain('STALE SECRET');});
 it('rejects unsafe content links',()=>{const f=page(false);expect(f.componentInstance.contentLink('javascript:alert(1)')).toBeNull();});
});
