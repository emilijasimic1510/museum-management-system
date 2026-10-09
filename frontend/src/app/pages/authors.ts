import { LocalizedContentPipe } from '../core/i18n';
import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Api } from '../core/api';
import { Author, CatalogResource } from '../core/catalog';
import { Feedback } from '../shared/feedback';
import { TranslatePipe } from '../core/i18n';

@Component({imports:[LocalizedContentPipe,RouterLink,Feedback,TranslatePipe],template:`
<a class="back" routerLink="/exhibits">{{'uiText.digital.catalog'|t}}</a><section class="page-heading"><p class="eyebrow">{{'uiText.people.behind.the.collection'|t}}</p><h1>{{'nav.authors'|t}}</h1><p>{{'uiText.meet.the.authors.and.discover.their.life.stories'|t}}</p></section>
<app-feedback [busy]="resource.busy()" [error]="resource.error()" />
@if(resource.error()){<button class="button secondary" [disabled]="resource.busy()" (click)="load()">{{'ui.retry'|t}}</button>}
@if(resource.data();as authors){<div class="cards catalog-cards">@for(a of authors;track a.id){
  <article class="panel catalog-card"><p class="eyebrow">{{'uiText.author'|t}}</p><h2><a [routerLink]="['/authors',a.id]">{{a.firstName}} {{a.lastName}}</a></h2>
    <dl class="catalog-facts">@if(a.birthYear!==null){<div><dt>{{'uiText.birth.year'|t}}</dt><dd>{{a.birthYear}}</dd></div>}@if(a.deathYear!==null){<div><dt>{{'uiText.death.year'|t}}</dt><dd>{{a.deathYear}}</dd></div>}</dl>
    <p class="clamp muted">{{(a|localized:'biography')|| ('uiText.biography.is.not.available.yet'|t)}}</p><a class="button secondary" [routerLink]="['/authors',a.id]" [attr.aria-label]="('uiText.about.the.author'|t)+' '+a.firstName+' '+(a.lastName||'')">{{'uiText.about.the.author.2'|t}}</a>
  </article>
}@empty{<div class="empty"><h2>{{'public.noAuthors'|t}}</h2><p>{{'uiText.biographies.will.appear.when.available'|t}}</p><a class="button secondary" routerLink="/exhibits">{{'uiText.view.exhibits'|t}}</a></div>}</div>}
`})
export class AuthorsPage {
  private api=inject(Api); readonly resource=new CatalogResource<Author[]>();
  constructor(){this.load();}
  load(){if(!this.resource.busy())void this.resource.load(()=>this.api.publicGet<Author[]>('/authors'));}
}
