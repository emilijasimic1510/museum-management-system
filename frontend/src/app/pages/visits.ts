import { Component, inject, signal } from '@angular/core';
import { LocalDatePipe as DatePipe } from '../core/i18n';
import { RouterLink } from '@angular/router';
import { Api, PageState } from '../core/api';
import { Visit } from '../core/models';
import { Feedback } from '../shared/feedback';
import { StatusPipe } from '../shared/status';
import { VisitRatingPanel } from '../shared/visit-rating';
import { TranslatePipe } from '../core/i18n';
@Component({imports:[DatePipe,RouterLink,Feedback,StatusPipe,VisitRatingPanel,TranslatePipe],template:`
<section class="page-heading"><p class="eyebrow">{{'uiText.your.museum.diary'|t}}</p><h1>{{'ui.visits'|t}}</h1><p>{{'uiText.active.visits.discovered.exhibits.and.impressions.from.past.exhibitions'|t}}</p></section>
<app-feedback [busy]="busy()" [error]="error()" />
<button class="text-button refresh" [disabled]="busy()" (click)="load()">{{'ui.refresh'|t}}</button>
<div class="cards">@for(v of visits();track v.id){
  <article class="panel visit-history-card"><span class="badge">{{v.status|status}}</span>
    <h2>{{v.exhibitionName || ('uiText.visit'|t)+v.id}}</h2><p>{{'uiText.visit'|t}}{{v.id}} {{'uiText.ticket.3'|t}}{{v.ticketId}}</p>
    <p>{{'uiText.entry'|t}} {{v.enteredAt|date:'dd.MM.yyyy. HH:mm'}}</p>
    @if(v.exitedAt){<p class="muted">{{'uiText.exit'|t}} {{v.exitedAt|date:'dd.MM.yyyy. HH:mm'}}</p>}
    @if(v.status==='ZAVRSENA'){<app-visit-rating [visitId]="v.id" [editable]="false" />}
    <a class="button secondary" [routerLink]="['/visits',v.id]">{{v.status==='ZAVRSENA'?('uiText.scanned.exhibits.and.rating'|t):('uiText.view.visit'|t)}}</a>
  </article>
}@empty{@if(!busy()&&!error()){<div class="empty"><h2>{{'ui.emptyVisits'|t}}</h2><p>{{'uiText.a.visit.appears.when.a.cashier.validates.your.ticket.it.remains.here.after.the'|t}}</p><a class="button" routerLink="/tickets">{{'ui.tickets'|t}}</a></div>}}</div>
`})
export class VisitsPage extends PageState {private api=inject(Api);visits=signal<Visit[]>([]);constructor(){super();this.load();}load(){void this.run(async()=>{this.visits.set([]);this.visits.set(await this.api.get<Visit[]>('/visits/me',10000));});}}
