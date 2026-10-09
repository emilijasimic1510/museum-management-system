import { LocalizedContentPipe } from '../core/i18n';
import { localizedContent, translate } from './../core/i18n';
import { Component, inject, signal } from '@angular/core';
import { LocalDatePipe as DatePipe, LocalNumberPipe as DecimalPipe } from '../core/i18n';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Api, PageState } from '../core/api';
import { Exhibition, Ticket } from '../core/models';
import { Feedback } from '../shared/feedback';
import { StatusPipe } from '../shared/status';
import { TranslatePipe } from '../core/i18n';
@Component({imports:[LocalizedContentPipe,DatePipe,DecimalPipe,RouterLink,Feedback,StatusPipe,TranslatePipe],template:`
<section class="page-heading"><p class="eyebrow">{{'uiText.ready.for.a.new.experience'|t}}</p><h1>{{'ui.tickets'|t}}</h1><p>{{'uiText.show.your.tickets.and.qr.tokens.at.the.museum.entrance'|t}}</p></section><app-feedback [busy]="busy()" [error]="error()" [notice]="notice()" /><button class="text-button refresh" [disabled]="busy()" (click)="load()">{{'ui.refresh'|t}}</button>
<div class="ticket-list">@for(t of tickets();track t.id){<article class="ticket"><div class="ticket-main"><div class="card-meta"><span class="eyebrow">{{'uiText.ticket.2'|t}}{{t.id}}</span><span class="badge">{{t.status|status}}</span></div><h2>{{ticketName(t)}}</h2><p>{{(t|localized:'category')}} {{'uiText.time.slot'|t}}{{t.termId}}</p><dl><div><dt>{{'status.KUPLJENA'|t}}</dt><dd>{{t.purchasedAt|date:'dd.MM.yyyy. HH:mm'}}</dd></div><div><dt>{{'uiText.valid.until.2'|t}}</dt><dd>{{t.validUntil|date:'dd.MM.yyyy. HH:mm'}}</dd></div><div><dt>{{'uiText.paid.price'|t}}</dt><dd>{{t.paidPrice|number:'1.2-2'}}</dd></div></dl><a [routerLink]="['/exhibitions',t.exhibitionId]">{{'uiText.exhibition.details'|t}}</a></div><div class="ticket-token"><span class="eyebrow">{{'uiText.qr.token'|t}}</span><p class="muted small-text">{{'uiText.ticket.identifier.for.cash.desk.validation'|t}}</p><code>{{t.qrToken}}</code><button class="button secondary small" (click)="copy(t.qrToken)">{{'ui.copy'|t}}</button></div></article>}
@empty{@if(!busy()&&!error()){<div class="empty"><span class="empty-mark"></span><h2>{{'ui.emptyTickets'|t}}</h2><p>{{'uiText.choose.an.exhibition.and.time.slot.then.buy.your.first.ticket'|t}}</p><a class="button" routerLink="/">{{'ui.explore'|t}}</a></div>}}</div>
`})
export class TicketsPage extends PageState {
  private api=inject(Api);private route=inject(ActivatedRoute);tickets=signal<Ticket[]>([]);exhibitions=signal<Exhibition[]>([]);
  constructor(){super();this.load();}
  load(){void this.run(async()=>{const t=await this.api.get<Ticket[]>('/tickets/me');this.tickets.set(t);try{this.exhibitions.set(await this.api.get<Exhibition[]>('/exhibitions'));}catch{ /* Ticket identifiers remain usable if catalog access is unavailable. */ }const id=Number(this.route.snapshot.queryParamMap.get('purchased'));if(id){const bought=t.find(x=>x.id===id);this.notice.set(bought?.status==='KUPLJENA'?translate("uiText.purchase.successful.your.ticket.is.ready"):translate("uiText.check.your.latest.ticket.status.only.a.purchased.ticket.allows.entry"));}});}
  ticketName(ticket:Ticket){return localizedContent(ticket,'exhibitionName')||this.name(ticket.exhibitionId);}
  name(id:number){return localizedContent(this.exhibitions().find(e=>e.id===id),'name')||translate("uiText.exhibition.3")+id;}
  async copy(token:string){try{await navigator.clipboard.writeText(token);this.notice.set(translate("uiText.qr.token.copied"));}catch{this.error.set(translate("uiText.copying.is.unavailable.select.and.copy.the.displayed.token"));}}
}
