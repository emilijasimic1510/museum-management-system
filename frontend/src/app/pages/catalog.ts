import { translate } from './../core/i18n';
import { MediaImage } from '../shared/media-image';
import { Component, computed, inject, signal } from '@angular/core';
import { LocalDatePipe as DatePipe } from '../core/i18n';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Api, PageState } from '../core/api';
import { Exhibition, Hall, Museum } from '../core/models';
import { Feedback } from '../shared/feedback';
import { StatusPipe } from '../shared/status';
import { TranslatePipe } from '../core/i18n';

@Component({imports:[MediaImage,DatePipe,FormsModule,RouterLink,Feedback,StatusPipe,TranslatePipe],template:`
<section class="hero"><div><p class="eyebrow">{{'catalog.eyebrow'|t}}</p><h1>{{'catalog.hero'|t}}</h1><p>{{'catalog.intro'|t}}</p><a class="button light" href="#exhibitions">{{'catalog.explore'|t}} <span aria-hidden="true">↓</span></a></div><div class="hero-art" aria-hidden="true"><div class="art-frame"><div class="art-sun"></div><div class="art-column"></div><div class="art-orbit"></div></div><span>{{'uiText.observe.discover.remember'|t}}</span></div></section>
<app-feedback [busy]="busy()" [error]="error()" />
@if(error()){<button class="button secondary" (click)="load()">{{'ui.retry'|t}}</button>}
<section id="exhibitions" class="section"><div class="section-heading"><div><p class="eyebrow">{{'catalog.plan'|t}}</p><h2>{{'catalog.heading'|t}}</h2></div><span class="count">{{filtered().length}} {{'catalog.exhibitions'|t}}</span></div>
<div class="filters"><label class="search">{{'catalog.search'|t}}<input [attr.aria-label]="'uiText.search.exhibitions'|t" [attr.placeholder]="'catalog.searchPlaceholder'|t" [ngModel]="query()" (ngModelChange)="query.set($event)"></label><label>{{'catalog.museum'|t}}<select [ngModel]="museumId()" (ngModelChange)="museumId.set($event)"><option [ngValue]="0">{{'catalog.allMuseums'|t}}</option>@for(m of museums();track m.id){<option [ngValue]="m.id">{{m.name}}</option>}</select></label></div>
<div class="cards">@for(e of filtered();track e.id){<article class="exhibition-card"><app-media-image [path]="'/media/exhibitions/'+e.id" [alt]="e.name" /><div class="card-body"><div class="card-meta"><span>{{museumName(e.hallId)}}</span><span class="badge">{{e.status|status}}</span></div><h3><a [routerLink]="['/exhibitions',e.id]">{{e.name}}</a></h3><p class="clamp">{{e.description || ('catalog.exhibitionFallback'|t)}}</p><div class="card-bottom"><span>{{e.start|date:'dd.MM.yyyy.'}} — {{e.end|date:'dd.MM.yyyy.'}}</span><a class="arrow-link" [routerLink]="['/exhibitions',e.id]" [attr.aria-label]="('uiText.details'|t)+' '+e.name">↗</a></div></div></article>}
@empty{ @if(!busy()&&!error()){<div class="empty"><span class="empty-mark">↗</span><h3>{{query()||museumId()?('catalog.emptySearch'|t):('catalog.empty'|t)}}</h3><p>{{query()||museumId()?('catalog.emptySearchText'|t):('catalog.emptyText'|t)}}</p></div>}}</div></section>
<section class="section museums"><div class="section-heading"><div><p class="eyebrow">{{'catalog.places'|t}}</p><h2>{{'catalog.museums'|t}}</h2></div></div><div class="museum-grid">@for(m of museums();track m.id){<article class="museum-card"><span class="eyebrow">{{m.foundationYear?('uiText.from'|t)+' '+m.foundationYear:('uiText.museum'|t)}}</span><h3>{{m.name}}</h3><p>{{m.description||('catalog.museumFallback'|t)}}</p><address>{{m.address}}</address><button class="text-button" (click)="selectMuseum(m.id)">{{'uiText.view.exhibitions'|t}}</button></article>}@empty{@if(!busy()&&!error()){<p class="muted">{{'catalog.noMuseums'|t}}</p>}}</div></section>
`})
export class CatalogPage extends PageState {
  private api=inject(Api); museums=signal<Museum[]>([]); exhibitions=signal<Exhibition[]>([]); halls=signal<Hall[]>([]);
  query=signal('');museumId=signal(0);
  filtered=computed(()=>this.exhibitions().filter(e=>(!this.museumId()||this.halls().some(h=>h.id===e.hallId&&h.museumId===this.museumId())) && `${e.name} ${e.description||''}`.toLocaleLowerCase().includes(this.query().toLocaleLowerCase().trim())));
  constructor(){super();this.load();}
  load(){void this.run(async()=>{const [m,e,h]=await Promise.all([this.api.get<Museum[]>('/museums'),this.api.get<Exhibition[]>('/exhibitions'),this.api.get<Hall[]>('/halls')]);this.museums.set(m.filter(x=>x.active));this.halls.set(h);this.exhibitions.set(e.filter(x=>x.onlineVisible));});}
  museumName(hallId:number){const museum=this.halls().find(h=>h.id===hallId)?.museumId;return this.museums().find(m=>m.id===museum)?.name||translate("ui.exhibition");}
  selectMuseum(id:number){this.museumId.set(id);document.getElementById('exhibitions')?.scrollIntoView({behavior:'smooth'});}
}
