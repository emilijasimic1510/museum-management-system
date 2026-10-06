import { TranslatePipe } from './../core/i18n';
import { MediaImage } from '../shared/media-image';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Api } from '../core/api';
import { CatalogExhibit, CatalogResource } from '../core/catalog';
import { Feedback } from '../shared/feedback';

@Component({imports:[TranslatePipe,MediaImage,FormsModule,RouterLink,Feedback],template:`
<section class="page-heading"><p class="eyebrow">{{'uiText.museum.collection'|t}}</p><h1>{{'nav.catalog'|t}}</h1><p>{{'uiText.discover.objects.their.authors.and.the.stories.connecting.them.to.exhibitions'|t}}</p><a class="button secondary" routerLink="/authors">{{'uiText.explore.authors'|t}}</a></section>
<app-feedback [busy]="resource.busy()" [error]="resource.error()" />
@if(resource.error()){<button class="button secondary" [disabled]="resource.busy()" (click)="load()">{{'ui.retry'|t}}</button>}
@if(resource.data();as items){
  @if(items.length){
    <div class="catalog-filters">
      <label>{{'uiText.search.by.name'|t}}<input type="search" name="query" [ngModel]="query()" (ngModelChange)="query.set($event)" [attr.placeholder]="'uiText.exhibit.name'|t"></label>
      <label>{{'uiText.type.2'|t}}<select name="type" [ngModel]="type()" (ngModelChange)="type.set($event)"><option value="">{{'uiText.all.types'|t}}</option>@for(value of types();track value){<option [value]="value">{{value}}</option>}</select></label>
      <label>{{'uiText.material'|t}}<select name="material" [ngModel]="material()" (ngModelChange)="material.set($event)"><option value="">{{'uiText.all.materials'|t}}</option>@for(value of materials();track value){<option [value]="value">{{value}}</option>}</select></label>
      <label>{{'uiText.period.year'|t}}<select name="period" [ngModel]="period()" (ngModelChange)="period.set($event)"><option value="">{{'uiText.all.periods'|t}}</option>@for(value of periods();track value){<option [value]="value">{{value}}</option>}</select></label>
    </div>
    <div class="section-heading"><p class="muted" role="status">{{'uiText.results'|t}} {{filtered().length}}</p><button class="text-button" (click)="reset()" [disabled]="!hasFilters()">{{'uiText.clear.filters'|t}}</button></div>
    <div class="cards catalog-cards">@for(e of filtered();track e.id){
      <article class="panel catalog-card"><app-media-image [path]="'/media/exhibits/'+e.id" [alt]="e.name" /><p class="eyebrow">{{e.type || ('uiText.exhibit'|t)}}</p><h2><a [routerLink]="['/exhibits',e.id]">{{e.name}}</a></h2>
        <dl class="catalog-facts">@if(e.material){<div><dt>{{'uiText.material'|t}}</dt><dd>{{e.material}}</dd></div>}@if(e.origin){<div><dt>{{'uiText.origin'|t}}</dt><dd>{{e.origin}}</dd></div>}@if(e.creationPeriod){<div><dt>{{'uiText.period.year'|t}}</dt><dd>{{e.creationPeriod}}</dd></div>}</dl>
        <div class="card-exhibitions"><h3>{{'uiText.exhibitions'|t}}</h3>
          <ul>@for(x of (e.exhibitions || []).slice(0,3);track x.id){<li><a [routerLink]="['/exhibitions',x.id]">{{x.name}}</a></li>}</ul>
          @if(!(e.exhibitions || []).length){<p class="muted">{{'uiText.not.currently.in.publicly.listed.exhibitions'|t}}</p>}
          @if((e.exhibitions || []).length>3){<a [routerLink]="['/exhibits',e.id]">{{'uiText.more'|t}} {{e.exhibitions.length-3}} {{'uiText.exhibitions.view.all'|t}}</a>}
        </div>
        <p class="muted clamp">{{e.description || ('uiText.no.exhibit.description.yet'|t)}}</p><a class="button secondary" [routerLink]="['/exhibits',e.id]" [attr.aria-label]="('uiText.exhibit.details'|t)+' '+e.name">{{'uiText.view.exhibit'|t}}</a>
      </article>
    }@empty{<div class="empty"><h2>{{'uiText.no.results'|t}}</h2><p>{{'uiText.change.the.name.or.selected.filters'|t}}</p><button class="button secondary" (click)="reset()">{{'uiText.show.all.exhibits'|t}}</button></div>}</div>
  }@else{<div class="empty"><h2>{{'uiText.the.catalog.is.being.prepared'|t}}</h2><p>{{'uiText.no.exhibits.added.yet.explore.the.available.exhibitions'|t}}</p><a class="button secondary" routerLink="/">{{'catalog.explore'|t}}</a></div>}
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
