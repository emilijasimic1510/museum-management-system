import {UiLabelPipe} from '../core/i18n';
import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Api } from '../core/api';
import { Auth } from '../core/auth';
import { EventState, MuseumEvent, OwnGuide, eventDate, } from '../core/events';
import { Feedback } from '../shared/feedback';
import { TranslatePipe } from '../core/i18n';

@Component({imports:[UiLabelPipe,RouterLink,Feedback,TranslatePipe],template:`
<section class="page-heading"><p class="eyebrow">{{'uiText.guide.workspace'|t}}</p><h1>{{'uiText.program.and.encounters'|t}}</h1><p>{{'uiText.overview.of.museum.events.and.registrations'|t}}</p></section>
<app-feedback [busy]="busy()" [error]="error()" /><button class="button secondary" (click)="load()" [disabled]="busy()">{{'uiText.refresh.overview'|t}}</button>
@if(counts();as c){<div class="cards event-summary"><article class="panel"><h2>{{c.future}}</h2><p>{{'uiText.upcoming.events'|t}}</p><small>{{'uiText.planned.and.full.events.that.have.not.started.yet'|t}}</small></article><article class="panel"><h2>{{c.mine}}</h2><p>{{'uiText.events.assigned.to.me'|t}}</p></article><article class="panel"><h2>{{c.registered}}</h2><p>{{'uiText.occupied.places.at.upcoming.events'|t}}</p><small>{{'uiText.total.registrations.a.visitor.may.register.for.multiple.events'|t}}</small></article></div>}
<div class="event-actions"><a class="button" routerLink="/guide/events/new">{{'uiText.new.event'|t}}</a><a class="button secondary" routerLink="/guide/registrations">{{'uiText.registration.overview'|t}}</a></div>
<section class="panel"><h2>{{'uiText.starting.soon'|t}}</h2>@if(counts()&&!soon().length){<p>{{'uiText.no.upcoming.events'|t}}</p>}@for(e of soon();track e.id){<article class="event-row"><h3>{{e.name}}</h3><p>{{date(e.start)}} · {{(e.type|uiLabel)}} · {{e.registeredCount}} / {{e.capacity}} {{'uiText.places'|t}}</p><a [routerLink]="['/guide/events',e.id,'registrations']">{{'uiText.view.registrations'|t}}</a></article>}</section>`})
export class GuideOverview extends EventState {
  private api=inject(Api);counts=signal<{future:number;mine:number;registered:number}|null>(null);soon=signal<MuseumEvent[]>([]);date=eventDate;
  constructor(){super();this.load();}
  load(){void this.run(async()=>{const [events,guide]=await Promise.all([this.api.get<MuseumEvent[]>('/events'),this.api.get<OwnGuide>('/guides/me')]);const future=events.filter(e=>['PLANIRAN','POPUNJEN'].includes(e.status)&&Date.parse(e.start)>Date.now()).sort((a,b)=>Date.parse(a.start)-Date.parse(b.start));this.counts.set({future:future.length,mine:events.filter(e=>e.guideId===guide.id).length,registered:future.reduce((sum,e)=>sum+e.registeredCount,0)});this.soon.set(future.slice(0,5));});}
}
@Component({imports:[UiLabelPipe,Feedback,TranslatePipe],template:`<section class="page-heading"><p class="eyebrow">{{'uiText.guide.workspace'|t}}</p><h1>{{'ui.profile'|t}}</h1></section><app-feedback [busy]="busy()" [error]="error()" />@if(profile();as p){<section class="panel"><h2>{{p.firstName}} {{p.lastName}}</h2><p>{{auth.currentAccount()?.email}}</p><p>{{'uiText.primary.language.2'|t}} {{p.primaryLanguage||('uiText.not.specified'|t)}}</p><p>{{'uiText.role.guide'|t}}</p></section>}@if(error()){<button class="button" (click)="load()" [disabled]="busy()">{{'ui.retry'|t}}</button>}`})
export class GuideProfile extends EventState {
  private api=inject(Api);auth=inject(Auth);profile=signal<OwnGuide|null>(null);constructor(){super();this.load();}
  load(){void this.run(async()=>{this.profile.set(await this.api.get<OwnGuide>('/guides/me'));});}
}
