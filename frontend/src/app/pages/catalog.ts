import { Component, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Api, PageState } from '../core/api';
import { Exhibition, Hall, Museum } from '../core/models';
import { Feedback } from '../shared/feedback';
import { StatusPipe } from '../shared/status';

@Component({imports:[DatePipe,FormsModule,RouterLink,Feedback,StatusPipe],template:`
<section class="hero"><div><p class="eyebrow">UMETNOST · ISTORIJA · OTKRIĆA</p><h1>Prostor za<br><em>novu perspektivu.</em></h1><p>Istražite muzeje i izložbe. Izaberite svoj termin.<br>Doživite priče koje ostaju sa vama.</p><a class="button light" href="#exhibitions">Istražite izložbe <span aria-hidden="true">↓</span></a></div><div class="hero-art" aria-hidden="true"><div class="art-frame"><div class="art-sun"></div><div class="art-column"></div><div class="art-orbit"></div></div><span>GLEDATI. OTKRIVATI. PAMTITI.</span></div></section>
<app-feedback [busy]="busy()" [error]="error()" />
@if(error()){<button class="button secondary" (click)="load()">Pokušaj ponovo</button>}
<section id="exhibitions" class="section"><div class="section-heading"><div><p class="eyebrow">ISPLANIRAJTE POSETU</p><h2>Izložbe za istraživanje</h2></div><span class="count">{{filtered().length}} izložbi</span></div>
<div class="filters"><label class="search">Pretraga<input aria-label="Pretraga izložbi" placeholder="Naziv ili opis izložbe" [ngModel]="query()" (ngModelChange)="query.set($event)"></label><label>Muzej<select [ngModel]="museumId()" (ngModelChange)="museumId.set($event)"><option [ngValue]="0">Svi muzeji</option>@for(m of museums();track m.id){<option [ngValue]="m.id">{{m.name}}</option>}</select></label></div>
<div class="cards">@for(e of filtered();track e.id){<article class="exhibition-card"><div class="card-art" aria-hidden="true"><span>{{e.name.slice(0,1)}}</span><i></i></div><div class="card-body"><div class="card-meta"><span>{{museumName(e.hallId)}}</span><span class="badge">{{e.status|status}}</span></div><h3><a [routerLink]="['/exhibitions',e.id]">{{e.name}}</a></h3><p class="clamp">{{e.description || 'Upoznajte izložbu i pogledajte dostupne termine.'}}</p><div class="card-bottom"><span>{{e.start|date:'dd.MM.yyyy.'}} — {{e.end|date:'dd.MM.yyyy.'}}</span><a class="arrow-link" [routerLink]="['/exhibitions',e.id]" [attr.aria-label]="'Detalji: '+e.name">↗</a></div></div></article>}
@empty{ @if(!busy()&&!error()){<div class="empty"><span class="empty-mark">↗</span><h3>{{query()||museumId()?'Nema rezultata pretrage':'Izložbe još nisu objavljene'}}</h3><p>{{query()||museumId()?'Promenite pretragu ili izbor muzeja.':'Ovde će se pojaviti izložbe kada ih muzej objavi.'}}</p></div>}}</div></section>
<section class="section museums"><div class="section-heading"><div><p class="eyebrow">MESTA KOJA PRIČAJU PRIČE</p><h2>Naši muzeji</h2></div></div><div class="museum-grid">@for(m of museums();track m.id){<article class="museum-card"><span class="eyebrow">{{m.foundationYear?'OD '+m.foundationYear:'MUZEJ'}}</span><h3>{{m.name}}</h3><p>{{m.description||'Istražite izložbe ovog muzeja.'}}</p><address>{{m.address}}</address><button class="text-button" (click)="selectMuseum(m.id)">Pogledajte izložbe ↗</button></article>}@empty{@if(!busy()&&!error()){<p class="muted">Trenutno nema aktivnih muzeja.</p>}}</div></section>
`})
export class CatalogPage extends PageState {
  private api=inject(Api); museums=signal<Museum[]>([]); exhibitions=signal<Exhibition[]>([]); halls=signal<Hall[]>([]);
  query=signal('');museumId=signal(0);
  filtered=computed(()=>this.exhibitions().filter(e=>(!this.museumId()||this.halls().some(h=>h.id===e.hallId&&h.museumId===this.museumId())) && `${e.name} ${e.description||''}`.toLocaleLowerCase().includes(this.query().toLocaleLowerCase().trim())));
  constructor(){super();this.load();}
  load(){void this.run(async()=>{const [m,e,h]=await Promise.all([this.api.get<Museum[]>('/museums'),this.api.get<Exhibition[]>('/exhibitions'),this.api.get<Hall[]>('/halls')]);this.museums.set(m.filter(x=>x.active));this.halls.set(h);this.exhibitions.set(e.filter(x=>x.onlineVisible));});}
  museumName(hallId:number){const museum=this.halls().find(h=>h.id===hallId)?.museumId;return this.museums().find(m=>m.id===museum)?.name||'Izložba';}
  selectMuseum(id:number){this.museumId.set(id);document.getElementById('exhibitions')?.scrollIntoView({behavior:'smooth'});}
}
