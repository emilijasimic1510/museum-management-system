import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { RecommendationsPage } from './recommendations';
import { VisitDetailPage } from './visit-detail';
describe('Recommendation availability',()=>{
 let http:HttpTestingController;
 beforeEach(()=>{vi.useFakeTimers();TestBed.configureTestingModule({providers:[provideHttpClient(),provideHttpClientTesting(),provideRouter([]),{provide:ActivatedRoute,useValue:{snapshot:{paramMap:convertToParamMap({id:'3'}),queryParamMap:convertToParamMap({})}}}]});http=TestBed.inject(HttpTestingController);});
 afterEach(()=>{http.verify();vi.useRealTimers();});
 it('times out and retries to empty',async()=>{const f=TestBed.createComponent(RecommendationsPage),p=f.componentInstance;const req=http.expectOne('/api/recommendations/exhibitions');expect(p.busy()).toBe(true);await vi.advanceTimersByTimeAsync(10001);f.detectChanges();expect(req.cancelled).toBe(true);expect(p.busy()).toBe(false);expect(f.nativeElement.textContent).toContain('Pokušaj ponovo');expect(f.nativeElement.textContent).toContain('Istražite javne izložbe');p.load();http.expectOne('/api/recommendations/exhibitions').flush([]);await vi.advanceTimersByTimeAsync(0);f.detectChanges();expect(f.nativeElement.textContent).toContain('Nova otkrića');});
 it('shows loaded fallback',async()=>{const f=TestBed.createComponent(RecommendationsPage);http.expectOne('/api/recommendations/exhibitions').flush([{exhibitionId:1,name:'Izložba',start:'2026-01-01',end:'2027-01-01',explanation:'Javno dostupna izložba; opšti pregled'}]);await vi.advanceTimersByTimeAsync(0);f.detectChanges();expect(f.nativeElement.textContent).toContain('prikazan je opšti pregled');});
 it('does not block active visit on demo',async()=>{const f=TestBed.createComponent(VisitDetailPage),p=f.componentInstance;http.expectOne('/api/visits/3').flush({id:3,status:'AKTIVNA',enteredAt:'2026-09-30',ticketId:1});http.expectOne('/api/visits/3/scans').flush([]);http.expectOne('/api/visits/3/routes').flush([]);await vi.advanceTimersByTimeAsync(0);expect(p.busy()).toBe(false);http.expectOne('/api/local-demo/enabled').flush({enabled:true});await vi.advanceTimersByTimeAsync(0);const req=http.expectOne('/api/local-demo/visits/3');await vi.advanceTimersByTimeAsync(10001);expect(req.cancelled).toBe(true);expect(p.visit()?.status).toBe('AKTIVNA');expect(p.notice()).toContain('Možete uneti QR');});
});
