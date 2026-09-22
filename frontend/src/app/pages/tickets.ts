import { Component, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Api, PageState } from '../core/api';
import { Exhibition, Ticket } from '../core/models';
import { Feedback } from '../shared/feedback';
import { StatusPipe } from '../shared/status';
@Component({imports:[DatePipe,DecimalPipe,RouterLink,Feedback,StatusPipe],template:`
<section class="page-heading"><p class="eyebrow">SPREMNI ZA NOVO ISKUSTVO</p><h1>Moje ulaznice</h1><p>Svoje ulaznice i QR tokene pokažite na ulazu u muzej.</p></section><app-feedback [busy]="busy()" [error]="error()" [notice]="notice()" /><button class="text-button refresh" [disabled]="busy()" (click)="load()">Osveži podatke ↻</button>
<div class="ticket-list">@for(t of tickets();track t.id){<article class="ticket"><div class="ticket-main"><div class="card-meta"><span class="eyebrow">ULAZNICA #{{t.id}}</span><span class="badge">{{t.status|status}}</span></div><h2>{{name(t.exhibitionId)}}</h2><p>{{t.category}} · termin #{{t.termId}}</p><dl><div><dt>Kupljena</dt><dd>{{t.purchasedAt|date:'dd.MM.yyyy. HH:mm'}}</dd></div><div><dt>Važi do</dt><dd>{{t.validUntil|date:'dd.MM.yyyy. HH:mm'}}</dd></div><div><dt>Plaćena cena</dt><dd>{{t.paidPrice|number:'1.2-2'}}</dd></div></dl><a [routerLink]="['/exhibitions',t.exhibitionId]">Detalji izložbe ↗</a></div><div class="ticket-token"><span class="eyebrow">QR TOKEN</span><p class="muted small-text">Identifikator ulaznice za proveru na blagajni.</p><code>{{t.qrToken}}</code><button class="button secondary small" (click)="copy(t.qrToken)">Kopiraj token</button></div></article>}
@empty{@if(!busy()&&!error()){<div class="empty"><span class="empty-mark">↗</span><h2>Vaša prva poseta vas čeka</h2><p>Izaberite izložbu i termin, pa kupite svoju prvu ulaznicu.</p><a class="button" routerLink="/">Istražite izložbe</a></div>}}</div>
`})
export class TicketsPage extends PageState {
  private api=inject(Api);private route=inject(ActivatedRoute);tickets=signal<Ticket[]>([]);exhibitions=signal<Exhibition[]>([]);
  constructor(){super();this.load();}
  load(){void this.run(async()=>{const t=await this.api.get<Ticket[]>('/tickets/me');this.tickets.set(t);try{this.exhibitions.set(await this.api.get<Exhibition[]>('/exhibitions'));}catch{ /* Ticket identifiers remain usable if catalog access is unavailable. */ }const id=Number(this.route.snapshot.queryParamMap.get('purchased'));if(id){const bought=t.find(x=>x.id===id);this.notice.set(bought?.status==='KUPLJENA'?'Kupovina je uspešna. Ulaznica je spremna.':'Proverite status poslednje ulaznice; samo kupljena ulaznica omogućava ulaz.');}});}
  name(id:number){return this.exhibitions().find(e=>e.id===id)?.name||'Izložba #'+id;}
  async copy(token:string){try{await navigator.clipboard.writeText(token);this.notice.set('QR token je kopiran.');}catch{this.error.set('Kopiranje nije dostupno. Označite i kopirajte prikazani token.');}}
}
