import { Component, inject } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Api } from '../core/api';
import { Author, CatalogResource, validCatalogId } from '../core/catalog';
import { Feedback } from '../shared/feedback';

@Component({imports:[RouterLink,Feedback],template:`
<a class="back" routerLink="/authors">← Svi autori</a><app-feedback [busy]="resource.busy()" [error]="resource.error()" />
@if(resource.notFound()){<section class="page-heading"><h1>Autor nije pronađen</h1><p>Proverite adresu ili izaberite drugog autora.</p></section>}
@else if(resource.error()){<button class="button secondary" [disabled]="resource.busy()" (click)="load()">Pokušaj ponovo</button>}
@if(resource.data();as a){
  <section class="page-heading"><p class="eyebrow">DIGITALNI KATALOG · AUTOR</p><h1>{{a.firstName}} {{a.lastName}}</h1></section>
  <section class="panel"><h2>Biografija</h2><dl class="catalog-facts">@if(a.birthYear!==null){<div><dt>Godina rođenja</dt><dd>{{a.birthYear}}</dd></div>}@if(a.deathYear!==null){<div><dt>Godina smrti</dt><dd>{{a.deathYear}}</dd></div>}</dl><p class="description">{{a.biography || 'Biografija još nije dostupna.'}}</p></section>
  <section class="panel"><h2>Eksponati i autorstvo</h2><p>Spisak eksponata ovog autora trenutno nije dostupan na ovoj stranici. Informacije o autorstvu možete pronaći na detaljima pojedinačnih eksponata.</p><a class="button secondary" routerLink="/exhibits">Istražite eksponate</a></section>
}
`})
export class AuthorDetailPage {
  private api=inject(Api);private id='';readonly resource=new CatalogResource<Author>();
  constructor(){inject(ActivatedRoute).paramMap.pipe(takeUntilDestroyed()).subscribe(params=>{this.id=params.get('id')||'';this.load();});}
  load(){const id=this.id;void this.resource.load(async()=>{
    if(!validCatalogId(id))throw new HttpErrorResponse({status:404});
    return this.api.publicGet<Author>('/authors/'+id);
  },'Autor nije pronađen.');}
}
