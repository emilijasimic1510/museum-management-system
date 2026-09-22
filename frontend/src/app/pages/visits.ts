import { Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Api, PageState } from '../core/api';
import { Visit } from '../core/models';
import { Feedback } from '../shared/feedback';
import { StatusPipe } from '../shared/status';
@Component({imports:[DatePipe,RouterLink,Feedback,StatusPipe],template:`
<section class="page-heading"><p class="eyebrow">VAŠ MUZEJSKI DNEVNIK</p><h1>Moje posete</h1><p>Eksponati koje ste otkrili i rute koje tek treba istražiti.</p></section><app-feedback [busy]="busy()" [error]="error()" /><button class="text-button refresh" [disabled]="busy()" (click)="load()">Osveži podatke ↻</button><div class="cards">@for(v of visits();track v.id){<article class="panel"><span class="badge">{{v.status|status}}</span><h2>Poseta #{{v.id}}</h2><p>Ulaznica #{{v.ticketId}}</p><p>{{v.enteredAt|date:'dd.MM.yyyy. HH:mm'}}</p>@if(v.exitedAt){<p class="muted">Završena {{v.exitedAt|date:'dd.MM.yyyy. HH:mm'}}</p>}<a class="button secondary" [routerLink]="['/visits',v.id]">Pogledajte posetu ↗</a></article>}@empty{@if(!busy()&&!error()){<div class="empty"><span class="empty-mark">◎</span><h2>Vaš dnevnik tek počinje</h2><p>Poseta se pojavljuje kada blagajnik na ulazu validira vašu ulaznicu. Ovde ćete pratiti skeniranja i preporučene rute.</p><a class="button" routerLink="/tickets">Moje ulaznice</a></div>}}</div>
`})
export class VisitsPage extends PageState {private api=inject(Api);visits=signal<Visit[]>([]);constructor(){super();this.load();}load(){void this.run(async()=>{this.visits.set(await this.api.get<Visit[]>('/visits/me'));});}}
