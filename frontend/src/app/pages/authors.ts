import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Api } from '../core/api';
import { Author, CatalogResource } from '../core/catalog';
import { Feedback } from '../shared/feedback';

@Component({imports:[RouterLink,Feedback],template:`
<a class="back" routerLink="/exhibits">← Digitalni katalog</a><section class="page-heading"><p class="eyebrow">LJUDI IZA ZBIRKE</p><h1>Autori</h1><p>Upoznajte autore i njihove životne priče.</p></section>
<app-feedback [busy]="resource.busy()" [error]="resource.error()" />
@if(resource.error()){<button class="button secondary" [disabled]="resource.busy()" (click)="load()">Pokušaj ponovo</button>}
@if(resource.data();as authors){<div class="cards catalog-cards">@for(a of authors;track a.id){
  <article class="panel catalog-card"><p class="eyebrow">AUTOR</p><h2><a [routerLink]="['/authors',a.id]">{{a.firstName}} {{a.lastName}}</a></h2>
    <dl class="catalog-facts">@if(a.birthYear!==null){<div><dt>Godina rođenja</dt><dd>{{a.birthYear}}</dd></div>}@if(a.deathYear!==null){<div><dt>Godina smrti</dt><dd>{{a.deathYear}}</dd></div>}</dl>
    <p class="clamp muted">{{a.biography || 'Biografija još nije dostupna.'}}</p><a class="button secondary" [routerLink]="['/authors',a.id]" [attr.aria-label]="'O autoru: '+a.firstName+' '+(a.lastName||'')">O autoru ↗</a>
  </article>
}@empty{<div class="empty"><h2>Autori još nisu dodati</h2><p>Biografije će se pojaviti kada budu dostupne.</p><a class="button secondary" routerLink="/exhibits">Pogledajte eksponate</a></div>}</div>}
`})
export class AuthorsPage {
  private api=inject(Api); readonly resource=new CatalogResource<Author[]>();
  constructor(){this.load();}
  load(){if(!this.resource.busy())void this.resource.load(()=>this.api.publicGet<Author[]>('/authors'));}
}
