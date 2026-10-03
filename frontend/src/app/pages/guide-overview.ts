import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Api } from '../core/api';
import { Auth } from '../core/auth';
import { EventState, MuseumEvent, OwnGuide, eventDate, eventLabels } from '../core/events';
import { Feedback } from '../shared/feedback';

@Component({imports:[RouterLink,Feedback],template:`
<section class="page-heading"><p class="eyebrow">PROSTOR ZA VODIČE</p><h1>Program i susreti</h1><p>Pregled događaja i prijava u muzeju.</p></section>
<app-feedback [busy]="busy()" [error]="error()" /><button class="button secondary" (click)="load()" [disabled]="busy()">Osveži pregled</button>
@if(counts();as c){<div class="cards event-summary"><article class="panel"><h2>{{c.future}}</h2><p>Budućih događaja</p><small>Planirani i popunjeni događaji koji još nisu počeli.</small></article><article class="panel"><h2>{{c.mine}}</h2><p>Događaja dodeljenih meni</p></article><article class="panel"><h2>{{c.registered}}</h2><p>Zauzetih mesta na budućim događajima</p><small>Zbir prijava; isti posetilac može biti prijavljen na više događaja.</small></article></div>}
<div class="event-actions"><a class="button" routerLink="/guide/events/new">Novi događaj</a><a class="button secondary" routerLink="/guide/registrations">Pregled prijava</a></div>
<section class="panel"><h2>Uskoro počinju</h2>@if(counts()&&!soon().length){<p>Nema budućih događaja.</p>}@for(e of soon();track e.id){<article class="event-row"><h3>{{e.name}}</h3><p>{{date(e.start)}} · {{labels[e.type]}} · {{e.registeredCount}} / {{e.capacity}} mesta</p><a [routerLink]="['/guide/events',e.id,'registrations']">Pogledaj prijave</a></article>}</section>`})
export class GuideOverview extends EventState {
  private api=inject(Api);counts=signal<{future:number;mine:number;registered:number}|null>(null);soon=signal<MuseumEvent[]>([]);date=eventDate;labels=eventLabels;
  constructor(){super();this.load();}
  load(){void this.run(async()=>{const [events,guide]=await Promise.all([this.api.get<MuseumEvent[]>('/events'),this.api.get<OwnGuide>('/guides/me')]);const future=events.filter(e=>['PLANIRAN','POPUNJEN'].includes(e.status)&&Date.parse(e.start)>Date.now()).sort((a,b)=>Date.parse(a.start)-Date.parse(b.start));this.counts.set({future:future.length,mine:events.filter(e=>e.guideId===guide.id).length,registered:future.reduce((sum,e)=>sum+e.registeredCount,0)});this.soon.set(future.slice(0,5));});}
}
@Component({imports:[Feedback],template:`<section class="page-heading"><p class="eyebrow">PROSTOR ZA VODIČE</p><h1>Moj profil</h1></section><app-feedback [busy]="busy()" [error]="error()" />@if(profile();as p){<section class="panel"><h2>{{p.firstName}} {{p.lastName}}</h2><p>{{auth.currentAccount()?.email}}</p><p>Primarni jezik: {{p.primaryLanguage||'Nije naveden'}}</p><p>Uloga: Vodič</p></section>}@if(error()){<button class="button" (click)="load()" [disabled]="busy()">Pokušaj ponovo</button>}`})
export class GuideProfile extends EventState {
  private api=inject(Api);auth=inject(Auth);profile=signal<OwnGuide|null>(null);constructor(){super();this.load();}
  load(){void this.run(async()=>{this.profile.set(await this.api.get<OwnGuide>('/guides/me'));});}
}
