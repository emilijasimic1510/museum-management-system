import { Component, inject, signal } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { Api, PageState } from '../core/api';
import { Ticket, Visit } from '../core/models';
import { Feedback } from '../shared/feedback';

@Component({imports:[FormsModule,DatePipe,Feedback],template:`
<section class="page-heading"><p class="eyebrow">ULAZ U MUZEJ</p><h1>Blagajna</h1><p>Unesite QR token sa ulaznice i termin kojem karta pripada. Validacija odmah aktivira posetu.</p></section>
<app-feedback [busy]="busy()" [error]="error()" [notice]="notice()" />
<div class="detail-layout"><section class="panel"><h2>Validacija QR karte</h2><form #form="ngForm" (ngSubmit)="validate(form)">
<label>QR token<input name="qrToken" [(ngModel)]="qrToken" required autocomplete="off" placeholder="Nalepite token sa ulaznice"></label>
<label>ID termina<input name="termId" type="number" [(ngModel)]="termId" required min="1" placeholder="npr. 12"></label>
<button class="button" [disabled]="busy()" type="submit">{{busy()?'Provera…':'Validiraj ulaznicu'}}</button></form>
<p class="muted small-text">Skeniranje čita isti token; ako skener kopira sadržaj u polje, samo unesite i ID termina.</p></section>
<section class="panel"><h2>Rezultat</h2>@if(result();as v){<span class="badge success">VALIDNA · AKTIVNA</span><dl><div><dt>Poseta</dt><dd>#{{v.id}}</dd></div><div><dt>Ulaznica</dt><dd>#{{v.ticketId}}</dd></div><div><dt>Ulazak</dt><dd>{{v.enteredAt|date:'dd.MM.yyyy. HH:mm'}}</dd></div></dl>@if(ticket();as t){<p><strong>{{ticketLabel(t)}}</strong><br>{{t.category}} · termin #{{t.termId}} · važi do {{t.validUntil|date:'dd.MM.yyyy. HH:mm'}}</p>}}@else{<p class="muted">Rezultat validacije će se pojaviti ovde.</p>}</section></div>`})
export class CashierPage extends PageState {
  private api=inject(Api); qrToken=''; termId:number|null=null; result=signal<Visit|null>(null); ticket=signal<Ticket|null>(null);
  validate(form:NgForm){if(form.invalid||!this.termId)return;void this.run(async()=>{try{const visit=await this.api.post<Visit>('/cashier/validate',{qrToken:this.qrToken.trim(),termId:Number(this.termId)});this.result.set(visit);this.ticket.set(await this.api.get<Ticket>('/tickets/'+visit.ticketId));this.notice.set('Ulaznica je validna. Poseta je kreirana i aktivna.');}catch(error){this.result.set(null);this.ticket.set(null);throw error;}});}
  ticketLabel(ticket:Ticket){return ticket.status==='AKTIVNA'?'Ulaznica iskorišćena za aktivan ulazak':ticket.status;}
}
