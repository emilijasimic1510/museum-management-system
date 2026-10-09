import { LocalizedContentPipe } from '../core/i18n';
import {UiLabelPipe} from '../core/i18n';
import { translate } from './../core/i18n';
import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Api } from '../core/api';
import { Auth } from '../core/auth';
import { EventState, MuseumEvent, EventRegistration, eventDate, registrationOpen, eventSoldOut } from '../core/events';
import { Exhibition } from '../core/models';
import { Feedback } from '../shared/feedback';
import { TranslatePipe } from '../core/i18n';

@Component({imports:[LocalizedContentPipe,UiLabelPipe,RouterLink,Feedback,TranslatePipe],template:`
<section class="page-heading"><p class="eyebrow">{{'uiText.museum.program'|t}}</p><h1>{{event()?.name||('uiText.event'|t)}}</h1><a routerLink="/events">{{'uiText.all.events'|t}}</a></section>
<app-feedback [busy]="busy()" [error]="error()" [notice]="notice()" />
<button class="button secondary" (click)="load()" [disabled]="busy()">{{'uiText.refresh.data'|t}}</button>
@if(event();as e){<div class="detail-layout"><section class="panel"><div class="event-tags"><span class="eyebrow">{{(e.type|uiLabel)}}</span><span class="badge">{{soldOut(e)?('browse.noPlaces'|t):(e.status|uiLabel)}}</span></div><h2>{{'event.about'|t}}</h2><dl class="event-facts"><dt>{{'event.start'|t}}</dt><dd>{{date(e.start)}}</dd><dt>{{'event.end'|t}}</dt><dd>{{date(e.end)}}</dd><dt>{{'ui.exhibition'|t}}</dt><dd>{{exhibitionName()||('uiText.exhibition.3'|t)+e.exhibitionId}}</dd><dt>{{'nav.language'|t}}</dt><dd>{{e.language||('uiText.not.specified'|t)}}</dd><dt>{{'event.capacity'|t}}</dt><dd>{{e.capacity}}</dd><dt>{{'event.freePlaces'|t}}</dt><dd>{{remaining(e)}}</dd></dl></section>
<section class="panel"><h2>{{'event.registration'|t}}</h2>
@if(!auth.session()){<p>{{open(e)?('event.loginToRegister'|t):('event.registerUnavailable'|t)}}</p><a class="button" routerLink="/login" [queryParams]="{returnUrl:'/events/'+e.id}">{{'uiText.sign.in'|t}}</a>}
@else if(auth.isVisitor()){
@if(!registrationLoaded()){<p>{{'event.registrationLoaded'|t}}</p>}
@else{
@if(registration();as r){<p>{{'uiText.status'|t}} <strong>{{(r.status|uiLabel)}}</strong></p>}
@if(!registration()||registration()?.status==='OTKAZAN'){<p>{{open(e)?('uiText.registration.is.available'|t):('uiText.registration.is.unavailable.the.event.has.started.is.cancelled.finished.or.ful'|t)}}</p><button class="button" [disabled]="busy()||!open(e)" (click)="register()">{{busy()?('login.wait'|t):registration()?('uiText.register.again'|t):('uiText.register.for.event'|t)}}</button>}
@else if(registration()?.status==='PRIJAVLJEN'){<button class="button secondary" [disabled]="busy()" (click)="confirmCancel.set(true)">{{'event.cancel'|t}}</button>}
@else{<p>{{'uiText.attendance.has.been.recorded.this.registration.cannot.be.cancelled'|t}}</p>}
@if(confirmCancel()){<div class="message" role="alert"><p>{{'uiText.cancel.your.registration.for'|t}}{{(e|localized:'name')}}“?</p><div class="event-actions"><button class="button" [disabled]="busy()" (click)="cancel()">{{'event.cancelConfirm'|t}}</button><button class="button secondary" [disabled]="busy()" (click)="confirmCancel.set(false)">{{'uiText.cancel'|t}}</button></div></div>}
}}
</section></div>}`})
export class EventDetail extends EventState {
  private api=inject(Api);private route=inject(ActivatedRoute);auth=inject(Auth);
  id=Number(this.route.snapshot.paramMap.get('id'));event=signal<MuseumEvent|null>(null);exhibitionName=signal('');registration=signal<EventRegistration|null>(null);registrationLoaded=signal(false);confirmCancel=signal(false);
  date=eventDate;open=registrationOpen;soldOut=eventSoldOut;
  constructor(){super();this.load();}
  remaining(e:MuseumEvent){return Math.max(0,e.capacity-e.registeredCount);}
  private async fetch(){
    this.registrationLoaded.set(false);
    const [event,exhibitions,registrations]=await Promise.all([this.api.publicGet<MuseumEvent>('/events/'+this.id),this.api.publicGet<Exhibition[]>('/exhibitions'),this.auth.isVisitor()?this.api.get<EventRegistration[]>('/visitors/me/registrations'):Promise.resolve([])]);
    this.event.set(event);this.exhibitionName.set(exhibitions.find(x=>x.id===event.exhibitionId)?.name||'');this.registration.set(registrations.find(r=>r.eventId===this.id)||null);this.registrationLoaded.set(true);
  }
  load(){void this.run(()=>this.fetch());}
  register(){const e=this.event();if(this.busy()||!this.auth.isVisitor()||!this.registrationLoaded()||!e||!registrationOpen(e)||(this.registration()&&this.registration()?.status!=='OTKAZAN'))return;void this.run(async()=>{const r=await this.api.post<EventRegistration>('/events/'+this.id+'/registrations',{});this.registration.set(r);this.notice.set(translate("event.registered"));await this.fetch();});}
  cancel(){if(this.busy()||!this.confirmCancel()||!this.auth.isVisitor()||!this.registrationLoaded()||this.registration()?.status!=='PRIJAVLJEN')return;void this.run(async()=>{await this.api.delete('/events/'+this.id+'/registrations/me');this.confirmCancel.set(false);this.notice.set(translate("event.cancelled"));await this.fetch();},translate("uiText.registration.cannot.be.cancelled.because.attendance.is.recorded.refresh.the.da"));}
}
