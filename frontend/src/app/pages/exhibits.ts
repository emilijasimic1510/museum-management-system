import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Api } from '../core/api';
import { CatalogExhibit, CatalogResource } from '../core/catalog';
import { Feedback } from '../shared/feedback';

@Component({imports:[FormsModule,RouterLink,Feedback],template:`
<section class="page-heading"><p class="eyebrow">ZBIRKA MUZEJA</p><h1>Digitalni katalog</h1><p>Upoznajte predmete, njihove autore i priče koje ih povezuju sa izložbama.</p><a class="button secondary" routerLink="/authors">Istražite autore ↗</a></section>
<app-feedback [busy]="resource.busy()" [error]="resource.error()" />
@if(resource.error()){<button class="button secondary" [disabled]="resource.busy()" (click)="load()">Pokušaj ponovo</button>}
@if(resource.data();as items){
  @if(items.length){
    <div class="catalog-filters">
      <label>Pretraga po nazivu<input type="search" name="query" [ngModel]="query()" (ngModelChange)="query.set($event)" placeholder="Naziv eksponata"></label>
      <label>Tip<select name="type" [ngModel]="type()" (ngModelChange)="type.set($event)"><option value="">Svi tipovi</option>@for(value of types();track value){<option [value]="value">{{value}}</option>}</select></label>
      <label>Materijal<select name="material" [ngModel]="material()" (ngModelChange)="material.set($event)"><option value="">Svi materijali</option>@for(value of materials();track value){<option [value]="value">{{value}}</option>}</select></label>
      <label>Period / godina<select name="period" [ngModel]="period()" (ngModelChange)="period.set($event)"><option value="">Svi periodi</option>@for(value of periods();track value){<option [value]="value">{{value}}</option>}</select></label>
    </div>
    <div class="section-heading"><p class="muted" role="status">Broj rezultata: {{filtered().length}}</p><button class="text-button" (click)="reset()" [disabled]="!hasFilters()">Poništi filtere</button></div>
    <div class="cards catalog-cards">@for(e of filtered();track e.id){
      <article class="panel catalog-card"><p class="eyebrow">{{e.type || 'EKSPONAT'}}</p><h2><a [routerLink]="['/exhibits',e.id]">{{e.name}}</a></h2>
        <dl class="catalog-facts">@if(e.material){<div><dt>Materijal</dt><dd>{{e.material}}</dd></div>}@if(e.origin){<div><dt>Poreklo</dt><dd>{{e.origin}}</dd></div>}@if(e.creationPeriod){<div><dt>Period / godina</dt><dd>{{e.creationPeriod}}</dd></div>}</dl>
        <div class="card-exhibitions"><h3>Izložbe</h3>
          <ul>@for(x of (e.exhibitions || []).slice(0,3);track x.id){<li><a [routerLink]="['/exhibitions',x.id]">{{x.name}}</a></li>}</ul>
          @if(!(e.exhibitions || []).length){<p class="muted">Trenutno nije na javno objavljenim izložbama.</p>}
          @if((e.exhibitions || []).length>3){<a [routerLink]="['/exhibits',e.id]">Još {{e.exhibitions.length-3}} izložbi — pogledajte sve</a>}
        </div>
        <p class="muted clamp">{{e.description || 'Opis eksponata još nije dodat.'}}</p><a class="button secondary" [routerLink]="['/exhibits',e.id]" [attr.aria-label]="'Detalji eksponata: '+e.name">Pogledajte eksponat ↗</a>
      </article>
    }@empty{<div class="empty"><h2>Nema rezultata</h2><p>Promenite naziv ili izabrane filtere.</p><button class="button secondary" (click)="reset()">Prikaži sve eksponate</button></div>}</div>
  }@else{<div class="empty"><h2>Katalog se priprema</h2><p>Eksponati još nisu dodati. Istražite trenutno dostupne izložbe.</p><a class="button secondary" routerLink="/">Istražite izložbe</a></div>}
}
`})
export class ExhibitsPage {
  private api=inject(Api); readonly resource=new CatalogResource<CatalogExhibit[]>();
  query=signal('');type=signal('');material=signal('');period=signal('');
  types=computed(()=>this.options('type'));materials=computed(()=>this.options('material'));periods=computed(()=>this.options('creationPeriod'));
  hasFilters=computed(()=>!!(this.query()||this.type()||this.material()||this.period()));
  filtered=computed(()=>(this.resource.data()||[]).filter(e=>
    e.name.toLocaleLowerCase('sr').includes(this.query().trim().toLocaleLowerCase('sr')) &&
    (!this.type()||e.type===this.type()) && (!this.material()||e.material===this.material()) && (!this.period()||e.creationPeriod===this.period())));
  constructor(){this.load();}
  load(){if(!this.resource.busy())void this.resource.load(()=>this.api.publicGet<CatalogExhibit[]>('/exhibits'));}
  reset(){this.query.set('');this.type.set('');this.material.set('');this.period.set('');}
  private options(field:'type'|'material'|'creationPeriod'){return [...new Set((this.resource.data()||[]).map(e=>e[field]).filter((v):v is string=>!!v?.trim()))].sort((a,b)=>a.localeCompare(b,'sr'));}
}
