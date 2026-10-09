import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { TicketsPage } from './tickets';
import { I18n } from '../core/i18n';
describe('Ticket exhibition name',()=>{
  it('keeps the exhibition visible even when the catalog is unavailable, and translates it',async()=>{
    TestBed.configureTestingModule({providers:[provideRouter([]),provideHttpClient(),provideHttpClientTesting()]});
    const language=TestBed.inject(I18n);language.setLanguage('sr');const http=TestBed.inject(HttpTestingController);const f=TestBed.createComponent(TicketsPage);
    http.expectOne('/api/tickets/me').flush([{id:1,exhibitionId:3,exhibitionName:'Tragovi svetlosti',category:'ODRASLI',purchasedAt:'2026-10-08',validUntil:'2026-10-09',status:'KUPLJENA',paidPrice:8,qrToken:'ticket-1'}]);
    await vi.waitFor(()=>http.expectOne('/api/exhibitions').flush(null,{status:500,statusText:'Unavailable'}));await f.whenStable();f.detectChanges();
    expect(f.nativeElement.querySelector('h2').textContent).toBe('Tragovi svetlosti');language.setLanguage('en');f.detectChanges();expect(f.nativeElement.querySelector('h2').textContent).toBe('Traces of Light');http.verify();language.setLanguage('sr');
  });
});
