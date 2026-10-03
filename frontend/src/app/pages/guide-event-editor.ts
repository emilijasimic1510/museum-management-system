import { Component, inject, signal } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Api } from '../core/api';
import { EventState, MuseumEvent, OwnGuide, EventType, EventStatus, EventRequest, eventLabels, eventTypes, eventStatuses } from '../core/events';
import { Exhibition } from '../core/models';
import { localDate } from '../core/curator';
import { validCatalogId } from '../core/catalog';
import { Feedback } from '../shared/feedback';

@Component({imports:[FormsModule,RouterLink,Feedback],template:`
<section class="page-heading"><p class="eyebrow">PROGRAM MUZEJA</p><h1>{{id?'Izmena događaja':'Novi događaj'}}</h1><a routerLink="/guide/events">← Događaji</a></section>
<app-feedback [busy]="busy()" [error]="error()" [notice]="notice()" />
@if(!ready()&&error()){<button class="button" (click)="load()" [disabled]="busy()">Pokušaj ponovo</button>}
@if(ready()){<section class="panel"><form #form="ngForm" (ngSubmit)="save(form)"><fieldset [disabled]="busy()"><div class="event-form-grid">
<label class="full-width">Naziv <span class="required-mark" aria-hidden="true">*</span><input name="name" [(ngModel)]="name" required maxlength="250"></label>
<label>Tip <span class="required-mark" aria-hidden="true">*</span><select name="type" [(ngModel)]="type" required>@for(t of types;track t){<option [value]="t">{{labels[t]}}</option>}</select></label>
<label>Status <span class="required-mark" aria-hidden="true">*</span><select name="status" [(ngModel)]="status" required>@for(s of statuses;track s){<option [value]="s">{{labels[s]}}</option>}</select></label>
<label>Početak <span class="required-mark" aria-hidden="true">*</span><input type="datetime-local" name="start" [(ngModel)]="start" required></label><label>Završetak <span class="required-mark" aria-hidden="true">*</span><input type="datetime-local" name="end" [(ngModel)]="end" required></label>
<label>Kapacitet <span class="required-mark" aria-hidden="true">*</span><input type="number" name="capacity" [(ngModel)]="capacity" required min="0" step="1" max="2147483647"></label><label>Jezik (opciono)<input name="language" [(ngModel)]="language"></label>
<label>Izložba <span class="required-mark" aria-hidden="true">*</span><select name="exhibition" [(ngModel)]="exhibitionId" required><option [ngValue]="null">Izaberite izložbu</option>@for(e of exhibitions();track e.id){<option [ngValue]="e.id">{{e.name}}</option>}@if(missingExhibition()){<option [ngValue]="exhibitionId">Trenutna izložba #{{exhibitionId}} (nije javno dostupna)</option>}</select></label>
<label>Vodič<select name="guide" [(ngModel)]="guideId"><option [ngValue]="null">Bez dodeljenog vodiča</option>@if(ownGuide();as g){<option [ngValue]="g.id">Ja — {{g.firstName}} {{g.lastName}}</option>}@if(otherGuideId!==null){<option [ngValue]="otherGuideId">Trenutno dodeljeni vodič #{{otherGuideId}}</option>}</select></label></div>
<p class="muted">Vreme se unosi u lokalnoj vremenskoj zoni. Promena kapaciteta ne može ukloniti postojeće prijave.</p>
@if(form.submitted&&form.invalid){<p class="field-error">Popunite obavezna polja i unesite nenegativan ceo broj mesta.</p>}
<div class="event-actions"><button class="button" type="submit" [disabled]="busy()">{{busy()?'Čuvanje…':'Sačuvaj događaj'}}</button><a class="button secondary" routerLink="/guide/events">Nazad na događaje</a>@if(id){<a class="button secondary" [routerLink]="['/guide/events',id,'registrations']">Pogledaj prijave</a>}</div></fieldset></form></section>}
`})
export class GuideEventEditor extends EventState {
  private api=inject(Api);private route=inject(ActivatedRoute);private router=inject(Router);
  id=Number(this.route.snapshot.paramMap.get('id'))||null;ready=signal(false);exhibitions=signal<Exhibition[]>([]);ownGuide=signal<OwnGuide|null>(null);
  name='';type:EventType='TURA';status:EventStatus='PLANIRAN';start='';end='';capacity:number|null=20;language='sr';exhibitionId:number|null=null;guideId:number|null=null;otherGuideId:number|null=null;
  types=eventTypes;statuses=eventStatuses;labels=eventLabels;
  constructor(){super();this.load();}
  missingExhibition(){return this.exhibitionId!==null&&!this.exhibitions().some(e=>e.id===this.exhibitionId);}
  load(){void this.run(async()=>{
    const rawId=this.route.snapshot.paramMap.get('id');
    if(rawId&&!validCatalogId(rawId)){this.error.set('Događaj nije pronađen. Proverite adresu.');return;}
    const [exhibitions,guide,event]=await Promise.all([this.api.get<Exhibition[]>('/exhibitions'),this.api.get<OwnGuide>('/guides/me'),this.id?this.api.get<MuseumEvent>('/events/'+this.id):Promise.resolve(null)]);
    this.exhibitions.set(exhibitions);this.ownGuide.set(guide);this.guideId=guide.id;
    if(event){this.name=event.name;this.type=event.type;this.status=event.status;this.start=localDate(event.start);this.end=localDate(event.end);this.capacity=event.capacity;this.language=event.language||'';this.exhibitionId=event.exhibitionId;this.guideId=event.guideId;this.otherGuideId=event.guideId!==guide.id?event.guideId:null;}
    this.ready.set(true);if(this.route.snapshot.queryParamMap.has('saved'))this.notice.set('Događaj je sačuvan.');
  });}
  save(form:NgForm){
    if(this.busy()||!this.ready()||form.invalid)return;
    if(!this.name.trim()||!this.exhibitionId||this.capacity===null||!Number.isInteger(this.capacity)||this.capacity<0||this.capacity>2147483647){this.error.set('Unesite naziv, izložbu i nenegativan ceo broj mesta.');return;}
    const start=new Date(this.start),end=new Date(this.end);
    if(!Number.isFinite(start.getTime())||!Number.isFinite(end.getTime())||end<=start){this.error.set('Završetak mora biti posle početka događaja.');return;}
    const body:EventRequest={exhibitionId:this.exhibitionId,guideId:this.guideId,name:this.name.trim(),type:this.type,status:this.status,start:start.toISOString(),end:end.toISOString(),capacity:this.capacity,language:this.language.trim()||null};
    void this.run(async()=>{const saved=await (this.id?this.api.put<MuseumEvent>('/events/'+this.id,body):this.api.post<MuseumEvent>('/events',body));this.status=saved.status;this.notice.set('Događaj je sačuvan.');if(!this.id){this.id=saved.id;await this.router.navigate(['/guide/events',saved.id,'edit'],{replaceUrl:true,queryParams:{saved:1}});}},'Kapacitet ne može biti manji od broja zauzetih mesta. Proverite postojeće prijave.');
  }
}
