import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { Auth, authInterceptor } from '../core/auth';
import { MediaImage } from './media-image';
import { ImageEditor } from './image-editor';

describe('Image media',()=>{
  let http:HttpTestingController;
  beforeEach(()=>{sessionStorage.clear();localStorage.clear();TestBed.configureTestingModule({providers:[provideRouter([]),provideHttpClient(withInterceptors([authInterceptor])),provideHttpClientTesting()]});http=TestBed.inject(HttpTestingController);
    Object.defineProperty(URL,'createObjectURL',{configurable:true,value:vi.fn(()=> 'blob:test-image')});Object.defineProperty(URL,'revokeObjectURL',{configurable:true,value:vi.fn()});});
  afterEach(()=>{http.verify();sessionStorage.clear();});
  it('shows a placeholder when the public image is missing and preserves alt text',()=>{
    const f=TestBed.createComponent(MediaImage);f.componentRef.setInput('path','/media/exhibits/1');f.componentRef.setInput('alt','Museum work');f.detectChanges();
    const img=f.nativeElement.querySelector('img');expect(img.alt).toBe('Museum work');expect(img.getAttribute('src')).toBe('/api/media/exhibits/1?v=0');img.dispatchEvent(new Event('error'));f.detectChanges();expect(f.nativeElement.querySelector('img')).toBeNull();expect(f.nativeElement.textContent).toContain('Slika nije dostupna');
  });
  it('uses authenticated blobs for protected images and revokes them on logout',async()=>{
    const auth=TestBed.inject(Auth);auth.session.set({token:'media-test',expiresAt:Date.now()+60000});
    const f=TestBed.createComponent(MediaImage);f.componentRef.setInput('path','/visits/1/content/2/images/3');f.componentRef.setInput('protected',true);f.componentRef.setInput('alt','Installation');f.detectChanges();
    const req=http.expectOne('/api/visits/1/content/2/images/3');expect(req.request.headers.get('Authorization')).toBe('Bearer media-test');req.flush(new Blob(['png'],{type:'image/png'}));await f.whenStable();f.detectChanges();expect(f.nativeElement.querySelector('img').src).toContain('blob:test-image');auth.clear();f.detectChanges();expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:test-image');expect(f.nativeElement.querySelector('img')).toBeNull();
  });
  it('uploads multipart data and refreshes the curator preview after success',async()=>{
    TestBed.inject(Auth).session.set({token:'media-test',expiresAt:Date.now()+60000});
    const f=TestBed.createComponent(ImageEditor);f.componentRef.setInput('endpoint','/curator/media/exhibits/1');f.componentRef.setInput('preview','/curator/media/exhibits/1');f.detectChanges();http.expectOne('/api/curator/media/exhibits/1').flush(new Blob(['old']));await f.whenStable();
    const input=f.nativeElement.querySelector('input');Object.defineProperty(input,'files',{value:[new File(['new'],'test.png',{type:'image/png'})]});input.dispatchEvent(new Event('change'));f.detectChanges();f.nativeElement.querySelector('button').click();
    const req=http.expectOne('/api/curator/media/exhibits/1');expect(req.request.method).toBe('POST');expect(req.request.body instanceof FormData).toBe(true);expect(req.request.body.get('file').name).toBe('test.png');req.flush({contentId:null});await f.whenStable();f.detectChanges();http.expectOne('/api/curator/media/exhibits/1').flush(new Blob(['new']));await f.whenStable();expect(f.nativeElement.textContent).toContain('Slika je sačuvana');
  });
  it('rejects invalid files before sending requests and displays server failures',async()=>{
    const f=TestBed.createComponent(ImageEditor);f.componentRef.setInput('endpoint','/curator/participations/2/images');f.detectChanges();
    f.componentInstance.choose({target:{files:[new File(['bad'],'x.svg',{type:'image/svg+xml'})]}} as unknown as Event);expect(f.componentInstance.file()).toBeNull();expect(f.componentInstance.message()).toBe('media.invalid');
    f.componentInstance.file.set(new File(['png'],'x.png',{type:'image/png'}));void f.componentInstance.upload();http.expectOne('/api/curator/participations/2/images').flush(null,{status:500,statusText:'Error'});await f.whenStable();expect(f.componentInstance.message()).toBe('media.error');expect(f.componentInstance.busy()).toBe(false);
  });
});
