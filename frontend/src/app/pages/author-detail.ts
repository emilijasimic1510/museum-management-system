import { LocalizedContentPipe } from '../core/i18n';
import { translate } from './../core/i18n';
import { Component, inject } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Api } from '../core/api';
import { Author, CatalogResource, validCatalogId } from '../core/catalog';
import { Feedback } from '../shared/feedback';
import { TranslatePipe } from '../core/i18n';

@Component({imports:[LocalizedContentPipe,RouterLink,Feedback,TranslatePipe],template:`
<a class="back" routerLink="/authors">{{'uiText.all.authors'|t}}</a><app-feedback [busy]="resource.busy()" [error]="resource.error()" />
@if(resource.notFound()){<section class="page-heading"><h1>{{'public.authorNotFound'|t}}</h1><p>{{'uiText.check.the.address.or.choose.another.author'|t}}</p></section>}
@else if(resource.error()){<button class="button secondary" [disabled]="resource.busy()" (click)="load()">{{'ui.retry'|t}}</button>}
@if(resource.data();as a){
  <section class="page-heading"><p class="eyebrow">{{'uiText.digital.catalog.author'|t}}</p><h1>{{a.firstName}} {{a.lastName}}</h1></section>
  <section class="panel"><h2>{{'public.biography'|t}}</h2><dl class="catalog-facts">@if(a.birthYear!==null){<div><dt>{{'uiText.birth.year'|t}}</dt><dd>{{a.birthYear}}</dd></div>}@if(a.deathYear!==null){<div><dt>{{'uiText.death.year'|t}}</dt><dd>{{a.deathYear}}</dd></div>}</dl><p class="description">{{(a|localized:'biography')|| ('uiText.biography.is.not.available.yet'|t)}}</p></section>
  <section class="panel"><h2>{{'uiText.exhibits.and.authorship'|t}}</h2><p>{{'uiText.this.author.s.exhibit.list.is.not.available.on.this.page.authorship.informatio'|t}}</p><a class="button secondary" routerLink="/exhibits">{{'uiText.explore.exhibits'|t}}</a></section>
}
`})
export class AuthorDetailPage {
  private api=inject(Api);private id='';readonly resource=new CatalogResource<Author>();
  constructor(){inject(ActivatedRoute).paramMap.pipe(takeUntilDestroyed()).subscribe(params=>{this.id=params.get('id')||'';this.load();});}
  load(){const id=this.id;void this.resource.load(async()=>{
    if(!validCatalogId(id))throw new HttpErrorResponse({status:404});
    return this.api.publicGet<Author>('/authors/'+id);
  },translate("uiText.author.not.found"));}
}
