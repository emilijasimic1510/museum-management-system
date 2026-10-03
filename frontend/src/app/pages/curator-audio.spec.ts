import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { CuratorContent } from './curator-content';
describe('Curator audio generation',()=>{
 let http:HttpTestingController;
 beforeEach(()=>{TestBed.configureTestingModule({providers:[provideHttpClient(),provideHttpClientTesting(),provideRouter([]),{provide:ActivatedRoute,useValue:{snapshot:{data:{},paramMap:convertToParamMap({}),queryParamMap:convertToParamMap({participationId:'7'})}}}]});http=TestBed.inject(HttpTestingController);});
 afterEach(()=>http.verify());
 async function settle(f:any){for(let i=0;i<10;i++)await Promise.resolve();f.detectChanges();await f.whenStable();f.detectChanges();}
 async function page(context:string|null){const f=TestBed.createComponent(CuratorContent);f.detectChanges();http.expectOne('/api/exhibit-participations').flush([{id:7,contextDescription:context}]);http.expectOne('/api/exhibits').flush([]);http.expectOne('/api/exhibitions').flush([]);await Promise.resolve();await Promise.resolve();http.expectOne('/api/exhibit-participations/7/digital-content').flush([]);await settle(f);return f;}
 it('disables generation without contextual text',async()=>{const f=await page('  ');const button=Array.from(f.nativeElement.querySelectorAll('button')).find((b:any)=>b.textContent.includes('Generiši')) as HTMLButtonElement;expect(button.disabled).toBe(true);f.componentInstance.generateAudio();http.expectNone('/api/curator/participations/7/audio');});
 it('shows loading, success metadata and TTS failure',async()=>{const f=await page('Kontekst');f.componentInstance.generateAudio();f.detectChanges();expect(f.componentInstance.busy()).toBe(true);const req=http.expectOne('/api/curator/participations/7/audio');expect(req.request.body).toEqual({language:'sr'});req.flush({id:9,participationId:7,type:'AUDIO',path:'',language:'sr',version:2,automaticallyGenerated:true,createdAt:'2026-10-02T12:00:00Z'});await settle(f);expect(f.nativeElement.textContent).toContain('Audio vodič je generisan');expect(f.nativeElement.textContent).toContain('Automatski generisan');f.componentInstance.generateAudio();http.expectOne('/api/curator/participations/7/audio').flush({code:'LOCAL_AUDIO_ERROR',detail:'eSpeak NG nije dostupan.'},{status:503,statusText:'Unavailable'});await settle(f);expect(f.nativeElement.textContent).toContain('eSpeak NG nije dostupan.');expect(f.componentInstance.busy()).toBe(false);});
});
