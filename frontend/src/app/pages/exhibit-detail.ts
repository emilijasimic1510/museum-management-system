import { LocalizedContentPipe } from '../core/i18n';
import { translate } from './../core/i18n';
import { MediaImage } from '../shared/media-image';
import { Component, inject } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Api } from '../core/api';
import { CatalogExhibit, CatalogParticipation, CatalogResource, validCatalogId } from '../core/catalog';
import { Feedback } from '../shared/feedback';
import { TranslatePipe } from '../core/i18n';
import { VisitorContent } from '../shared/visitor-content';

@Component({imports:[LocalizedContentPipe,MediaImage,RouterLink,Feedback,VisitorContent,TranslatePipe],template:`
<a class="back" routerLink="/exhibits">{{'uiText.digital.catalog'|t}}</a>
<app-feedback [busy]="resource.busy()" [error]="resource.error()" />
@if(resource.notFound()){<section class="page-heading"><h1>{{'public.exhibitNotFound'|t}}</h1><p>{{'uiText.check.the.address.or.choose.another.exhibit.from.the.catalog'|t}}</p></section>}
@else if(resource.error()){<button class="button secondary" [disabled]="resource.busy()" (click)="load()">{{'ui.retry'|t}}</button>}
@if(resource.data();as e){
  <section class="page-heading"><p class="eyebrow">{{'uiText.digital.catalog.exhibit'|t}}</p><h1>{{(e|localized:'name')}}</h1></section>
  <app-media-image [path]="'/media/exhibits/'+e.id" [alt]="e|localized:'name'" [contain]="true" /><div class="detail-layout"><section>
    <div class="panel"><h2>{{'public.aboutExhibit'|t}}</h2><p class="description">{{(e|localized:'description')|| ('uiText.no.exhibit.description.yet'|t)}}</p>
      <dl class="catalog-facts">@if(e.type){<div><dt>{{'uiText.type.2'|t}}</dt><dd>{{e.type|t}}</dd></div>}@if(e.material){<div><dt>{{'uiText.material'|t}}</dt><dd>{{(e|localized:'material')}}</dd></div>}@if(e.origin){<div><dt>{{'uiText.origin'|t}}</dt><dd>{{(e|localized:'origin')}}</dd></div>}@if(e.creationPeriod){<div><dt>{{'uiText.creation.period.year'|t}}</dt><dd>{{e.creationPeriod}}</dd></div>}</dl>
    </div>
    <div class="panel"><h2>{{'uiText.in.exhibitions'|t}}</h2><app-feedback [busy]="participations.busy()" [error]="participations.error()" />
      @if(participations.error()){<button class="button secondary" [disabled]="participations.busy()" (click)="loadParticipations()">{{'uiText.reload.exhibitions'|t}}</button>}
      @if(participations.data();as entries){@for(p of entries;track $index){<article class="route-block"><h3><a [routerLink]="['/exhibitions',p.exhibition.id]">{{(p.exhibition|localized:'name')}}</a></h3><p class="description">{{(p|localized:'contextDescription')|| ('uiText.no.contextual.description.yet'|t)}}</p></article>}@empty{<p class="empty-inline">{{'uiText.no.installations.in.publicly.listed.exhibitions.at.the.moment'|t}}</p>}}
    </div>
  </section><aside>
    <section class="panel"><h2>{{'uiText.authors.and.authorship'|t}}</h2>@for(a of e.authors;track a.id){<div class="route-block"><h3><a [routerLink]="['/authors',a.id]">{{(a|localized:'name')}}</a></h3>@if(a.contributionType){<p>{{(a|localized:'contributionType')}}</p>}</div>}@empty{<p class="empty-inline">{{'uiText.authorship.information.is.not.available.yet'|t}}</p>}</section>
    <app-visitor-content [exhibitId]="e.id" />
  </aside></div>
}
`})
export class ExhibitDetailPage {
  private api=inject(Api);private id='';readonly resource=new CatalogResource<CatalogExhibit>();
  readonly participations=new CatalogResource<CatalogParticipation[]>();
  constructor(){inject(ActivatedRoute).paramMap.pipe(takeUntilDestroyed()).subscribe(params=>{this.id=params.get('id')||'';this.load();});}
  load(){const id=this.id;void this.resource.load(async()=>{
    if(!validCatalogId(id))throw new HttpErrorResponse({status:404});
    return this.api.publicGet<CatalogExhibit>('/exhibits/'+id);
  },translate("uiText.exhibit.not.found"));if(validCatalogId(id))this.loadParticipations();}
  loadParticipations(){const id=this.id;void this.participations.load(()=>this.api.publicGet<CatalogParticipation[]>('/exhibits/'+id+'/participations'));}
}
