import {UiLabelPipe} from '../core/i18n';
import { translate } from './../core/i18n';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Api } from '../core/api';
import { EventState, MuseumEvent, eventStatuses, eventDate, localDay, registrationOpen } from '../core/events';
import { Feedback } from '../shared/feedback';
import { TranslatePipe } from '../core/i18n';

@Component({imports:[UiLabelPipe,FormsModule,RouterLink,Feedback,TranslatePipe],template:`
<section class="page-heading"><p class="eyebrow">{{guide?('uiText.guide.workspace'|t):('uiText.experience.the.museum'|t)}}</p><h1>{{'nav.guideEvents'|t}}</h1><p>{{guide?('uiText.manage.the.museum.program.and.visitor.registrations'|t):('uiText.find.a.tour.lecture.or.workshop.and.reserve.your.place'|t)}}</p>
@if(guide){<a class="button" routerLink="/guide/events/new">{{'uiText.new.event'|t}}</a>}</section>
<app-feedback [busy]="busy()" [error]="error()" [notice]="notice()" />
<section class="panel event-filters" [attr.aria-label]="'uiText.event.filters'|t">
<label>{{'catalog.search'|t}}<input type="search" [ngModel]="query()" (ngModelChange)="query.set($event)" [attr.placeholder]="'uiText.event.name'|t"></label>
<label>{{'ui.status'|t}}<select [ngModel]="status()" (ngModelChange)="status.set($event)"><option value="">{{'uiText.all.statuses'|t}}</option>@for(s of statuses;track s){<option [value]="s">{{(s|uiLabel)}}</option>}</select></label>
<label>{{'uiText.from.date'|t}}<input type="date" [ngModel]="from()" (ngModelChange)="from.set($event)"></label>
<label>{{'uiText.to.date'|t}}<input type="date" [ngModel]="to()" (ngModelChange)="to.set($event)"></label>
<button class="button secondary" (click)="load()" [disabled]="busy()">{{'uiText.refresh'|t}}</button></section>
@if(!busy()&&!error()&&!filtered().length){<p class="empty">{{events().length?('uiText.no.events.match.the.selected.filters'|t):('uiText.no.events.at.the.moment'|t)}}</p>}
<div class="cards">@for(e of filtered();track e.id){<article class="panel event-card"><div class="event-tags"><span class="eyebrow">{{(e.type|uiLabel)}}</span><span class="badge">{{(e.status|uiLabel)}}</span></div><h2>{{e.name}}</h2>
<p>{{date(e.start)}} – {{date(e.end)}}</p><p>{{'uiText.registered.3'|t}} {{e.registeredCount}} / {{e.capacity}} {{'uiText.available.places'|t}} {{remaining(e)}}</p>
@if(e.language){<p>{{'uiText.language'|t}} {{e.language}}</p>}
@if(guide){<div class="event-actions"><a class="button secondary" [routerLink]="['/guide/events',e.id,'edit']">{{'uiText.edit'|t}}</a><a class="button secondary" [routerLink]="['/guide/events',e.id,'registrations']">{{'nav.registrations'|t}}</a><button class="button danger" [disabled]="busy()" (click)="pending.set(e.id)">{{'uiText.delete'|t}}</button></div>
@if(pending()===e.id){<div class="message" role="alert"><p>{{'uiText.delete.2'|t}}{{e.name}}{{'uiText.an.event.with.registration.history.cannot.be.deleted'|t}}</p><div class="event-actions"><button class="button danger" [disabled]="busy()" (click)="remove(e.id)">{{'uiText.confirm.deletion'|t}}</button><button class="button secondary" [disabled]="busy()" (click)="pending.set(null)">{{'uiText.cancel'|t}}</button></div></div>}}
@else{<p>{{open(e)?('uiText.registration.is.available'|t):('event.registerUnavailable'|t)}}</p><a class="button secondary" [routerLink]="['/events',e.id]">{{'uiText.details.and.registration'|t}}</a>}
</article>}</div>`})
export class EventsPage extends EventState {
  private api=inject(Api);guide=inject(ActivatedRoute).snapshot.data['guide']===true;
  events=signal<MuseumEvent[]>([]);query=signal('');status=signal('');from=signal('');to=signal('');pending=signal<number|null>(null);
  statuses=eventStatuses;date=eventDate;open=registrationOpen;
  filtered=computed(()=>this.events().filter(e=>e.name.toLocaleLowerCase('sr').includes(this.query().trim().toLocaleLowerCase('sr'))&&(!this.status()||e.status===this.status())&&(!this.from()||localDay(e.start)>=this.from())&&(!this.to()||localDay(e.start)<=this.to())).sort((a,b)=>Date.parse(a.start)-Date.parse(b.start)));
  constructor(){super();this.load();}
  remaining(e:MuseumEvent){return Math.max(0,e.capacity-e.registeredCount);}
  load(){void this.run(async()=>{this.events.set(await (this.guide?this.api.get<MuseumEvent[]>('/events'):this.api.publicGet<MuseumEvent[]>('/events')));});}
  remove(id:number){if(!this.guide||this.pending()!==id)return;void this.run(async()=>{await this.api.delete('/events/'+id);this.events.update(items=>items.filter(e=>e.id!==id));this.pending.set(null);this.notice.set(translate("uiText.event.deleted"));},translate("uiText.the.event.has.registrations.and.cannot.be.deleted.if.it.will.not.take.place.ch"));}
}
