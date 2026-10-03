import { Component, inject } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Api } from '../core/api';
import { CatalogExhibit, CatalogParticipation, CatalogResource, validCatalogId } from '../core/catalog';
import { Feedback } from '../shared/feedback';
import { VisitorContent } from '../shared/visitor-content';

@Component({imports:[RouterLink,Feedback,VisitorContent],template:`
<a class="back" routerLink="/exhibits">← Digitalni katalog</a>
<app-feedback [busy]="resource.busy()" [error]="resource.error()" />
@if(resource.notFound()){<section class="page-heading"><h1>Eksponat nije pronađen</h1><p>Proverite adresu ili izaberite drugi eksponat iz kataloga.</p></section>}
@else if(resource.error()){<button class="button secondary" [disabled]="resource.busy()" (click)="load()">Pokušaj ponovo</button>}
@if(resource.data();as e){
  <section class="page-heading"><p class="eyebrow">DIGITALNI KATALOG · EKSPONAT</p><h1>{{e.name}}</h1></section>
  <div class="detail-layout"><section>
    <div class="panel"><h2>O eksponatu</h2><p class="description">{{e.description || 'Opis eksponata još nije dodat.'}}</p>
      <dl class="catalog-facts">@if(e.type){<div><dt>Tip</dt><dd>{{e.type}}</dd></div>}@if(e.material){<div><dt>Materijal</dt><dd>{{e.material}}</dd></div>}@if(e.origin){<div><dt>Poreklo</dt><dd>{{e.origin}}</dd></div>}@if(e.creationPeriod){<div><dt>Period / godina nastanka</dt><dd>{{e.creationPeriod}}</dd></div>}</dl>
    </div>
    <div class="panel"><h2>Na izložbama</h2><app-feedback [busy]="participations.busy()" [error]="participations.error()" />
      @if(participations.error()){<button class="button secondary" [disabled]="participations.busy()" (click)="loadParticipations()">Ponovi učitavanje izložbi</button>}
      @if(participations.data();as entries){@for(p of entries;track $index){<article class="route-block"><h3><a [routerLink]="['/exhibitions',p.exhibition.id]">{{p.exhibition.name}}</a></h3><p class="description">{{p.contextDescription || 'Kontekstualni opis još nije dodat.'}}</p></article>}@empty{<p class="empty-inline">Trenutno nema učešća na javno objavljenim izložbama.</p>}}
    </div>
  </section><aside>
    <section class="panel"><h2>Autori i autorstvo</h2>@for(a of e.authors;track a.id){<div class="route-block"><h3><a [routerLink]="['/authors',a.id]">{{a.name}}</a></h3>@if(a.contributionType){<p>{{a.contributionType}}</p>}</div>}@empty{<p class="empty-inline">Podaci o autorstvu još nisu dostupni.</p>}</section>
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
  },'Eksponat nije pronađen.');if(validCatalogId(id))this.loadParticipations();}
  loadParticipations(){const id=this.id;void this.participations.load(()=>this.api.publicGet<CatalogParticipation[]>('/exhibits/'+id+'/participations'));}
}
