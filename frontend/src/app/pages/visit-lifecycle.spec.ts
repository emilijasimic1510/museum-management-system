import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { VisitDetailPage } from './visit-detail';
import { VisitsPage } from './visits';
import { VisitRatingPanel } from '../shared/visit-rating';

const active={id:3,ticketId:7,exhibitionId:2,exhibitionName:'Grad kroz objektiv',status:'AKTIVNA',enteredAt:'2026-09-30T10:00:00Z',exitedAt:null};
const finished={...active,status:'ZAVRSENA',exitedAt:'2026-09-30T11:00:00Z'};
const saved={id:8,visitId:3,rating:5,comment:'Odličan obilazak.',ratedAt:'2026-09-30T11:01:00Z'};
const scanned={id:10,visitId:3,participationId:15,scannedAt:'2026-09-30T10:30:00Z',durationSeconds:20,next:[],participation:{participationId:15,name:'Jutro na trgu',contextDescription:null,content:[]}};

describe('Visitor visit lifecycle',()=>{
  let http:HttpTestingController;
  beforeEach(()=>{
    vi.useFakeTimers();
    TestBed.configureTestingModule({providers:[provideHttpClient(),provideHttpClientTesting(),provideRouter([]),
      {provide:ActivatedRoute,useValue:{snapshot:{paramMap:convertToParamMap({id:'3'}),queryParamMap:convertToParamMap({})}}}]});
    http=TestBed.inject(HttpTestingController);
  });
  afterEach(()=>{http.verify();vi.useRealTimers();});
  async function render<T>(f:ComponentFixture<T>){await vi.advanceTimersByTimeAsync(0);f.detectChanges();await vi.advanceTimersByTimeAsync(0);f.detectChanges();}
  function respondVisit(closed:boolean){
    http.expectOne('/api/visits/3').flush(closed?finished:active);
    http.expectOne('/api/visits/3/scans').flush(closed?[scanned]:[]);
    http.expectOne('/api/visits/3/routes').flush([]);
  }
  function ratingPanel(){const f=TestBed.createComponent(VisitRatingPanel);f.componentRef.setInput('visitId',3);f.detectChanges();return f;}
  it('submits a camera QR once and opens the returned exhibit content',async()=>{
    const f=TestBed.createComponent(VisitDetailPage);respondVisit(false);await render(f);
    expect(f.nativeElement.querySelector('input[name="qr"]')).toBeNull();
    f.componentInstance.performScan('/scan/EXHIBIT-15');f.componentInstance.performScan('/scan/EXHIBIT-15');
    const req=http.expectOne('/api/visits/3/scans');expect(req.request.body).toEqual({qrToken:'EXHIBIT-15'});req.flush(scanned);
    await vi.advanceTimersByTimeAsync(0);respondVisit(false);await render(f);
    expect(f.nativeElement.querySelector('.scanned-exhibit').textContent).toContain('Jutro na trgu');
    expect(f.componentInstance.lastScan()?.participation.participationId).toBe(15);
    http.expectNone(r=>r.url.includes('local-demo'));
  });
  function noRating(){http.expectOne('/api/visits/3/rating').flush(null,{status:404,statusText:'Not Found'});}
  function submit<T>(f:ComponentFixture<T>){f.nativeElement.querySelector('form').dispatchEvent(new Event('submit',{bubbles:true,cancelable:true}));}
  function enterRating(f:ComponentFixture<VisitRatingPanel>,score:number,comment=''){
    const select:HTMLSelectElement=f.nativeElement.querySelector('select[name="rating"]');
    select.selectedIndex=score;select.dispatchEvent(new Event('change',{bubbles:true}));
    const textarea:HTMLTextAreaElement=f.nativeElement.querySelector('textarea');
    textarea.value=comment;textarea.dispatchEvent(new Event('input',{bubbles:true}));
  }

  it('finishes an active visit, removes scan controls and shows preserved names and the rating form',async()=>{
    const f=TestBed.createComponent(VisitDetailPage);respondVisit(false);await render(f);
    http.expectNone('/api/local-demo/enabled');
    expect(f.nativeElement.querySelector('app-visit-rating')).toBeNull();
    expect(f.nativeElement.textContent).toContain('Završi posetu');
    f.componentInstance.confirmFinish.set(true);f.detectChanges();f.componentInstance.finish();f.componentInstance.finish();
    const request=http.expectOne('/api/visits/3/finish');expect(request.request.method).toBe('POST');request.flush(finished);
    await vi.advanceTimersByTimeAsync(0);respondVisit(true);await render(f);noRating();await render(f);
    expect(f.nativeElement.textContent).toContain('Poseta je završena');
    expect(f.nativeElement.textContent).toContain('Izlazak');
    expect(f.nativeElement.textContent).toContain('Jutro na trgu');
    expect(f.nativeElement.querySelector('input[name="qr"]')).toBeNull();
    expect(f.nativeElement.querySelector('select[name="rating"]')).not.toBeNull();
    expect(f.nativeElement.querySelector('a[href="/recommendations"]')).not.toBeNull();
    http.expectNone('/api/visits/3/content/15');
  });
  it('keeps the visit active when finish fails and permits retry',async()=>{
    const f=TestBed.createComponent(VisitDetailPage);respondVisit(false);await render(f);
    http.expectNone('/api/local-demo/enabled');
    f.componentInstance.confirmFinish.set(true);f.componentInstance.finish();
    http.expectOne('/api/visits/3/finish').flush({message:'PRIVATE EXCEPTION'},{status:500,statusText:'Failure'});await render(f);
    expect(f.componentInstance.visit()?.status).toBe('AKTIVNA');expect(f.componentInstance.busy()).toBe(false);
    expect(f.nativeElement.querySelector('[role="alert"]')).not.toBeNull();expect(f.nativeElement.textContent).not.toContain('PRIVATE EXCEPTION');
  });
  it('shows completed visits with exhibition, dates and saved rating in history',async()=>{
    const f=TestBed.createComponent(VisitsPage);http.expectOne('/api/visits/me').flush([finished]);await render(f);
    http.expectOne('/api/visits/3/rating').flush(saved);await render(f);
    expect(f.nativeElement.textContent).toContain('Grad kroz objektiv');expect(f.nativeElement.textContent).toContain('Izlazak');
    expect(f.nativeElement.textContent).toContain('5 / 5');expect(f.nativeElement.textContent).toContain(saved.comment);
    expect(f.nativeElement.querySelector('form')).toBeNull();expect(f.nativeElement.querySelector('a[href="/visits/3"]')).not.toBeNull();
  });
  it('shows an empty history',async()=>{const f=TestBed.createComponent(VisitsPage);http.expectOne('/api/visits/me').flush([]);await render(f);expect(f.nativeElement.textContent).toContain('Vaš dnevnik tek počinje');});
  it('submits one valid rating and comment, then replaces the form with the saved result',async()=>{
    const f=ratingPanel();noRating();await render(f);expect(f.nativeElement.querySelector('button[type="submit"]').disabled).toBe(true);
    enterRating(f,5,saved.comment);await render(f);submit(f);submit(f);
    const req=http.expectOne('/api/visits/3/rating');expect(req.request.method).toBe('POST');expect(req.request.body).toEqual({rating:5,comment:saved.comment});
    req.flush(saved);await render(f);expect(f.nativeElement.querySelector('form')).toBeNull();
    expect(f.nativeElement.textContent).toContain('5 / 5');expect(f.nativeElement.textContent).toContain('Ocena je sačuvana');
  });
  it('shows an existing rating without a second submission',async()=>{
    const f=ratingPanel();http.expectOne('/api/visits/3/rating').flush(saved);await render(f);
    expect(f.nativeElement.querySelector('form')).toBeNull();expect(f.nativeElement.textContent).toContain(saved.comment);
  });
  it('reconciles a duplicate rating conflict through GET before another submission',async()=>{
    const f=ratingPanel();noRating();await render(f);enterRating(f,4);await render(f);submit(f);
    http.expectOne('/api/visits/3/rating').flush(null,{status:409,statusText:'Conflict'});await render(f);
    expect(f.nativeElement.textContent).toContain('Poseta već ima ocenu');expect(f.nativeElement.querySelector('form')).toBeNull();
    f.nativeElement.querySelector('button').click();http.expectOne('/api/visits/3/rating').flush(saved);await render(f);
    expect(f.nativeElement.textContent).toContain('5 / 5');expect(f.nativeElement.querySelector('form')).toBeNull();
  });
  for(const status of [401,403,500])it(`does not treat rating ${status} as an absent rating`,async()=>{
    const f=ratingPanel();http.expectOne('/api/visits/3/rating').flush({message:'PRIVATE EXCEPTION'},{status,statusText:'Failure'});await render(f);
    expect(f.nativeElement.querySelector('form')).toBeNull();expect(f.nativeElement.querySelector('[role="alert"]')).not.toBeNull();
    expect(f.nativeElement.textContent).not.toContain('PRIVATE EXCEPTION');
  });
  it('bounds rating loading and allows checking again after a timeout',async()=>{
    const f=ratingPanel();const req=http.expectOne('/api/visits/3/rating');await vi.advanceTimersByTimeAsync(10001);await render(f);
    expect(req.cancelled).toBe(true);expect(f.componentInstance.busy()).toBe(false);expect(f.nativeElement.querySelector('form')).toBeNull();
    f.nativeElement.querySelector('button').click();noRating();await render(f);expect(f.nativeElement.querySelector('form')).not.toBeNull();
  });
});
