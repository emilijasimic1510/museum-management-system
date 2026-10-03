import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { CashierSale } from './cashier-sale';

describe('Cashier sale',()=>{
 let http:HttpTestingController;
 beforeEach(()=>{TestBed.configureTestingModule({imports:[CashierSale],providers:[provideHttpClient(),provideHttpClientTesting()]});http=TestBed.inject(HttpTestingController);});
 afterEach(()=>http.verify());
 async function settle(fixture:any){for(let i=0;i<10;i++)await Promise.resolve();fixture.detectChanges();await fixture.whenStable();fixture.detectChanges();}
 it('loads data, submits the existing cashier route and displays the transaction',async()=>{
  const fixture=TestBed.createComponent(CashierSale);fixture.detectChanges();
  http.expectOne('/api/exhibitions').flush([{id:7,name:'Grad kroz objektiv',onlineVisible:true,status:'AKTIVNA',end:'2099-01-01T00:00:00Z',terms:[{id:11,exhibitionId:7,start:'2090-01-01T10:00:00Z',end:'2090-01-01T12:00:00Z',capacity:20,status:'OTVOREN'}]}]);
  await settle(fixture);const exhibition:HTMLSelectElement=fixture.nativeElement.querySelector('[name=exhibition]');exhibition.selectedIndex=1;exhibition.dispatchEvent(new Event('change',{bubbles:true}));http.expectOne('/api/prices?exhibitionId=7').flush([{exhibitionId:7,category:'ODRASLI',price:8,validFrom:'2020-01-01T00:00:00Z',validTo:null}]);await settle(fixture);for(const name of ['term','category']){const select:HTMLSelectElement=fixture.nativeElement.querySelector('[name='+name+']');select.selectedIndex=1;select.dispatchEvent(new Event('change',{bubbles:true}));}await settle(fixture);fixture.detectChanges();expect(fixture.nativeElement.textContent).toContain('8.00');
  fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));fixture.detectChanges();const sale=http.expectOne('/api/cashier/tickets');expect(sale.request.body).toEqual({termId:11,category:'ODRASLI',paymentMethod:'GOTOVINA'});sale.flush({id:91,visitorId:null,termId:11,exhibitionId:7,category:'ODRASLI',purchasedAt:'2090-01-01T10:00:00Z',paidPrice:8,status:'KUPLJENA',qrToken:'safe-token',validUntil:'2090-01-01T12:00:00Z'});await settle(fixture);http.expectOne('/api/tickets/91/payments').flush([{id:44,ticketId:91,amount:8,method:'GOTOVINA',status:'USPESNA'}]);await settle(fixture);fixture.detectChanges();expect(fixture.nativeElement.textContent).toContain('Ulaznica je prodata');expect(fixture.nativeElement.textContent).toContain('Transakcija #44');
 });
 it('shows empty state when no exhibitions are available',async()=>{const fixture=TestBed.createComponent(CashierSale);fixture.detectChanges();http.expectOne('/api/exhibitions').flush([]);await settle(fixture);fixture.detectChanges();expect(fixture.nativeElement.textContent).toContain('Nema javno dostupnih izložbi');});
});
