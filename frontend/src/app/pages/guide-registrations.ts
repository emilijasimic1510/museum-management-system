import {UiLabelPipe} from '../core/i18n';
import { translate } from './../core/i18n';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Api } from '../core/api';
import { EventState, MuseumEvent, EventRegistration, ManagedRegistration, RegistrationStatus, registrationStatuses, eventDate } from '../core/events';
import { Feedback } from '../shared/feedback';
import { TranslatePipe } from '../core/i18n';

@Component({imports:[UiLabelPipe,FormsModule,RouterLink,Feedback,TranslatePipe],template:`
<section class="page-heading"><p class="eyebrow">{{'uiText.guide.workspace'|t}}</p><h1>{{'uiText.event.registrations'|t}}</h1><a routerLink="/guide/events">{{'uiText.events'|t}}</a></section>
<app-feedback [busy]="busy()" [error]="error()" [notice]="notice()" />
<section class="panel"><label>{{'uiText.event'|t}}<select [ngModel]="selected()" (ngModelChange)="select($event)" [disabled]="busy()"><option [ngValue]="null">{{'uiText.choose.an.event'|t}}</option>@for(e of events();track e.id){<option [ngValue]="e.id">{{e.name}} — {{date(e.start)}}</option>}</select></label><button class="button secondary" (click)="load()" [disabled]="busy()">{{'uiText.refresh.data'|t}}</button></section>
@if(event();as e){<section class="panel"><h2>{{e.name}}</h2><p>{{(e.status|uiLabel)}} {{'uiText.registered.4'|t}} <span class="capacity">{{e.registeredCount}} / {{e.capacity}}</span> {{'uiText.available.places'|t}} {{remaining(e)}}</p><a [routerLink]="['/guide/events',e.id,'edit']">{{'uiText.edit.event.2'|t}}</a></section>}
@if(!busy()&&!error()&&!events().length){<p class="empty">{{'uiText.no.events.yet'|t}}</p>}
@if(!busy()&&!error()&&event()&&!rows().length){<p class="empty">{{'uiText.no.registrations.for.this.event'|t}}</p>}
<div class="cards">@for(r of rows();track r.visitorId){<article class="panel"><h2>{{r.firstName}} {{r.lastName}}</h2><p>{{'uiText.visitor'|t}}{{r.visitorId}}</p><p>{{'uiText.registered.5'|t}} {{date(r.registeredAt)}}</p><p>{{'uiText.status'|t}} <span class="badge">{{(r.status|uiLabel)}}</span></p><form (ngSubmit)="save(r)"><label>{{'uiText.new.status'|t}}<select [name]="'status-'+r.visitorId" [(ngModel)]="draft[r.visitorId]" [disabled]="busy()">@for(s of statuses;track s){<option [value]="s">{{(s|uiLabel)}}</option>}</select></label><button class="button" type="submit" [disabled]="busy()||draft[r.visitorId]===r.status">{{'uiText.save.status'|t}}</button></form></article>}</div>`})
export class GuideRegistrations extends EventState {
  private api=inject(Api);private route=inject(ActivatedRoute);
  selected=signal<number|null>(Number(this.route.snapshot.paramMap.get('id'))||null);events=signal<MuseumEvent[]>([]);event=signal<MuseumEvent|null>(null);rows=signal<ManagedRegistration[]>([]);draft:Record<number,RegistrationStatus>={};
  statuses=registrationStatuses;date=eventDate;
  constructor(){super();this.load();}
  remaining(e:MuseumEvent){return Math.max(0,e.capacity-e.registeredCount);}
  private async fetchSelected(){const id=this.selected();if(id===null){this.rows.set([]);this.event.set(null);return;}const [event,rows]=await Promise.all([this.api.get<MuseumEvent>('/events/'+id),this.api.get<ManagedRegistration[]>('/events/'+id+'/registrations')]);this.event.set(event);this.rows.set(rows);this.draft=Object.fromEntries(rows.map(r=>[r.visitorId,r.status]));}
  load(){void this.run(async()=>{this.events.set(await this.api.get<MuseumEvent[]>('/events'));await this.fetchSelected();});}
  select(id:number|null){if(this.busy())return;this.selected.set(id);this.rows.set([]);this.event.set(null);void this.run(()=>this.fetchSelected());}
  save(r:EventRegistration){if(this.busy()||!this.selected()||!registrationStatuses.includes(this.draft[r.visitorId])||this.draft[r.visitorId]===r.status)return;void this.run(async()=>{const saved=await this.api.put<EventRegistration>('/events/'+this.selected()+'/registrations/'+r.visitorId,{status:this.draft[r.visitorId]});this.rows.update(rows=>rows.map(row=>row.visitorId===saved.visitorId?{...row,...saved}:row));this.notice.set(translate("uiText.registration.status.saved"));await this.fetchSelected();});}
}
