import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AudioGuide } from './audio-guide';
import { Auth, authInterceptor } from '../core/auth';
describe('Private audio guide',()=>{
 let http:HttpTestingController;
 beforeEach(()=>{TestBed.configureTestingModule({providers:[provideHttpClient(withInterceptors([authInterceptor])),provideHttpClientTesting()]});http=TestBed.inject(HttpTestingController);TestBed.inject(Auth).session.set({token:'test-token',expiresAt:Date.now()+60000});vi.stubGlobal('URL',class extends URL {static override createObjectURL=vi.fn(()=> 'blob:test');static override revokeObjectURL=vi.fn();});});
 afterEach(()=>{http.verify();vi.unstubAllGlobals();sessionStorage.clear();});
 function page(content:any[]){const f=TestBed.createComponent(AudioGuide);f.componentRef.setInput('visitId',1);f.componentRef.setInput('participation',{participationId:7,name:'Eksponat',contextDescription:'Tekst',content});f.detectChanges();return f;}
 it('shows a neutral message without an empty player',()=>{const f=page([]);expect(f.nativeElement.querySelector('audio')).toBeNull();expect(f.nativeElement.textContent).toContain('Audio trenutno nije dostupan');});
 it('fetches latest version as authenticated Blob and releases URL',async()=>{const f=page([{id:1,type:'AUDIO',language:'sr',version:1},{id:2,type:'AUDIO',language:'sr',version:2}]);const req=http.expectOne('/api/visits/1/content/7/audio/2');expect(req.request.responseType).toBe('blob');expect(req.request.headers.get('Authorization')).toBe('Bearer test-token');req.flush(new Blob(['wav'],{type:'audio/wav'}));await f.whenStable();f.detectChanges();expect(f.nativeElement.querySelector('audio').src).toBe('blob:test');expect(f.nativeElement.textContent).toContain('aktuelna verzija 2');f.destroy();expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:test');});
 it('does not create stale URLs after destruction',async()=>{const f=page([{id:1,type:'AUDIO',language:'sr',version:1}]);const req=http.expectOne('/api/visits/1/content/7/audio/1');f.destroy();req.flush(new Blob(['wav']));await Promise.resolve();await Promise.resolve();expect(URL.createObjectURL).not.toHaveBeenCalled();});
 it('handles denied downloads without broken audio',async()=>{const f=page([{id:1,type:'AUDIO',language:'sr',version:1}]);http.expectOne('/api/visits/1/content/7/audio/1').flush(new Blob(),{status:403,statusText:'Forbidden'});await f.whenStable();f.detectChanges();expect(f.nativeElement.querySelector('audio')).toBeNull();expect(f.nativeElement.textContent).toContain('Tekst možete pročitati');});
});
