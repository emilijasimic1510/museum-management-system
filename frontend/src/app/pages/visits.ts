import { Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Api, PageState } from '../core/api';
import { Visit } from '../core/models';
import { Feedback } from '../shared/feedback';
import { StatusPipe } from '../shared/status';
import { VisitRatingPanel } from '../shared/visit-rating';
@Component({imports:[DatePipe,RouterLink,Feedback,StatusPipe,VisitRatingPanel],template:`
<section class="page-heading"><p class="eyebrow">VAŠ MUZEJSKI DNEVNIK</p><h1>Moje posete</h1><p>Aktivne posete, otkriveni eksponati i utisci sa prethodnih izložbi.</p></section>
<app-feedback [busy]="busy()" [error]="error()" />
<button class="text-button refresh" [disabled]="busy()" (click)="load()">Osveži podatke ↻</button>
<div class="cards">@for(v of visits();track v.id){
  <article class="panel visit-history-card"><span class="badge">{{v.status|status}}</span>
    <h2>{{v.exhibitionName || 'Poseta #'+v.id}}</h2><p>Poseta #{{v.id}} · Ulaznica #{{v.ticketId}}</p>
    <p>Ulazak {{v.enteredAt|date:'dd.MM.yyyy. HH:mm'}}</p>
    @if(v.exitedAt){<p class="muted">Izlazak {{v.exitedAt|date:'dd.MM.yyyy. HH:mm'}}</p>}
    @if(v.status==='ZAVRSENA'){<app-visit-rating [visitId]="v.id" [editable]="false" />}
    <a class="button secondary" [routerLink]="['/visits',v.id]">{{v.status==='ZAVRSENA'?'Skenirani eksponati i ocena ↗':'Pogledajte posetu ↗'}}</a>
  </article>
}@empty{@if(!busy()&&!error()){<div class="empty"><h2>Vaš dnevnik tek počinje</h2><p>Poseta se pojavljuje kada blagajnik validira vašu ulaznicu. Ovde ostaje i nakon završetka obilaska.</p><a class="button" routerLink="/tickets">Moje ulaznice</a></div>}}</div>
`})
export class VisitsPage extends PageState {private api=inject(Api);visits=signal<Visit[]>([]);constructor(){super();this.load();}load(){void this.run(async()=>{this.visits.set([]);this.visits.set(await this.api.get<Visit[]>('/visits/me',10000));});}}
