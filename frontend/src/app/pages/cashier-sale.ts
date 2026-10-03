import { Component, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { FormsModule, NgForm } from '@angular/forms';
import { Api, errorMessage } from '../core/api';
import { Exhibition, Price, Term, Ticket } from '../core/models';
import { Feedback } from '../shared/feedback';
import { StatusPipe } from '../shared/status';

interface Payment { id:number; ticketId:number; amount:number; method:string; status:string }
interface Sale { ticket:Ticket; exhibition:string; term:Term }
@Component({selector:'app-cashier-sale',imports:[FormsModule,DatePipe,DecimalPipe,Feedback,StatusPipe],template:`
<section class="panel cashier-sale"><h2>Prodaja ulaznice</h2><p>Prodaja anonimnom kupcu na blagajni.</p>
<app-feedback [busy]="loading()||selling()" [error]="error()" />
@if(!sale()) { <form #form="ngForm" (ngSubmit)="sell(form)">
<label>Izložba <span class="required-mark" aria-hidden="true">*</span><select name="exhibition" [ngModel]="exhibitionId" (ngModelChange)="choose($event)" required><option [ngValue]="null">Izaberite izložbu</option>@for(e of exhibitions();track e.id){<option [ngValue]="e.id">{{e.name}}</option>}</select></label>
@if(!loading()&&!exhibitions().length&&!error()){<p class="empty-inline">Nema javno dostupnih izložbi.</p>}
<label>Termin <span class="required-mark" aria-hidden="true">*</span><select name="term" [(ngModel)]="termId" required><option [ngValue]="null">Izaberite termin</option>@for(t of terms();track t.id){<option [ngValue]="t.id">#{{t.id}} · {{t.start|date:'dd.MM.yyyy. HH:mm'}} — {{t.end|date:'HH:mm'}}</option>}</select></label>
@if(exhibitionId&&!loading()&&!terms().length){<p class="empty-inline">Nema otvorenih termina za prodaju.</p>}
<label>Cenovna kategorija <span class="required-mark" aria-hidden="true">*</span><select name="category" [(ngModel)]="category" required><option value="">Izaberite kategoriju</option>@for(p of prices();track p.category){<option [value]="p.category">{{p.category}} · {{p.price|number:'1.2-2'}}</option>}</select></label>
@if(exhibitionId&&!loading()&&!prices().length){<p class="message">Za ovu izložbu nema važeće cene.</p>}
<label>Način plaćanja <span class="required-mark" aria-hidden="true">*</span><select name="method" [(ngModel)]="method" required><option value="GOTOVINA">Gotovina</option><option value="KARTICA">Kartica</option><option value="DRUGO">Drugo</option></select></label>
<div class="total"><span>Trenutna cena</span><strong>{{selectedPrice()===null?'—':(selectedPrice()|number:'1.2-2')}}</strong></div>
<button class="button" type="submit" [disabled]="form.invalid||!canSell()||loading()||selling()">{{selling()?'Prodaja u toku…':'Prodaj ulaznicu'}}</button>
</form> }
@if(sale();as s){<section class="cashier-receipt" aria-live="polite"><h3>Ulaznica je prodata · #{{s.ticket.id}}</h3><dl><div><dt>Izložba</dt><dd>{{s.exhibition}}</dd></div><div><dt>Termin</dt><dd>#{{s.ticket.termId}} · {{s.term.start|date:'dd.MM.yyyy. HH:mm'}}</dd></div><div><dt>Kategorija</dt><dd>{{s.ticket.category}}</dd></div><div><dt>Cena</dt><dd>{{s.ticket.paidPrice|number:'1.2-2'}}</dd></div><div><dt>Status</dt><dd>{{s.ticket.status|status}}</dd></div></dl><label>QR/token ulaznice<textarea readonly rows="2" [value]="s.ticket.qrToken"></textarea></label><h3>Rezultat transakcije</h3><app-feedback [busy]="paymentsBusy()" [error]="paymentsError()" />@for(p of payments();track p.id){<p>Transakcija #{{p.id}} · {{p.status}} · {{p.method}} · {{p.amount|number:'1.2-2'}}</p>}<button class="button" type="button" (click)="newSale()">Nova prodaja</button></section>}
</section>`})
export class CashierSale {
 private api=inject(Api); exhibitions=signal<Exhibition[]>([]); prices=signal<Price[]>([]); loading=signal(false); selling=signal(false); error=signal(''); sale=signal<Sale|null>(null); payments=signal<Payment[]>([]); paymentsBusy=signal(false); paymentsError=signal('');
 exhibitionId:number|null=null;termId:number|null=null;category='';method='GOTOVINA';
 constructor(){void this.load();}
 terms(){return (this.exhibitions().find(e=>e.id===this.exhibitionId)?.terms||[]).filter(t=>t.status==='OTVOREN'&&Date.parse(t.end)>Date.now());}
 validPrice(p:Price){return Date.parse(p.validFrom)<=Date.now()&&(!p.validTo||Date.parse(p.validTo)>Date.now());}
 selectedPrice(){return this.prices().find(p=>p.category===this.category)?.price??null;}
 canSell(){return this.terms().some(t=>t.id===this.termId)&&this.selectedPrice()!==null;}
 async load(){this.loading.set(true);this.error.set('');try{const xs=await this.api.get<Exhibition[]>('/exhibitions');this.exhibitions.set(xs.filter(e=>e.onlineVisible&&['AKTIVNA','PLANIRANA'].includes(e.status)&&Date.parse(e.end)>Date.now()));}catch(e){this.error.set(errorMessage(e));}finally{this.loading.set(false);}}
 async choose(id:number|null){this.exhibitionId=id;this.termId=null;this.category='';this.prices.set([]);if(!id)return;this.loading.set(true);try{const xs=await this.api.get<Price[]>('/prices?exhibitionId='+id);const seen=new Set<string>();this.prices.set(xs.filter(p=>this.validPrice(p)).filter(p=>{if(seen.has(p.category))return false;seen.add(p.category);return true;}));}catch(e){this.error.set(errorMessage(e));}finally{this.loading.set(false);}}
 async sell(form:NgForm){if(form.invalid||!this.canSell()||this.selling())return;const e=this.exhibitions().find(x=>x.id===this.exhibitionId)!,t=this.terms().find(x=>x.id===this.termId)!;this.selling.set(true);this.error.set('');try{const ticket=await this.api.post<Ticket>('/cashier/tickets',{termId:t.id,category:this.category,paymentMethod:this.method});this.sale.set({ticket,exhibition:e.name,term:t});await this.loadPayments();}catch(err){this.error.set(errorMessage(err));}finally{this.selling.set(false);}}
 async loadPayments(){const s=this.sale();if(!s)return;this.paymentsBusy.set(true);this.paymentsError.set('');try{this.payments.set(await this.api.get<Payment[]>('/tickets/'+s.ticket.id+'/payments'));}catch{this.paymentsError.set('Ulaznica je evidentirana, ali transakcija nije učitana. Ponovo učitajte podatke.');}finally{this.paymentsBusy.set(false);}}
 newSale(){this.sale.set(null);this.payments.set([]);void this.load();}
}
