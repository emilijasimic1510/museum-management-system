import { Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Api } from '../core/api';
import { Auth } from '../core/auth';
import { CollectionKind, CollectionRecord, CollectionState, collectionLabels, hasCollectionRole } from '../core/collection-management';
import { Feedback } from '../shared/feedback';
import { StatusPipe } from '../shared/status';

@Component({imports:[RouterLink,DatePipe,Feedback,StatusPipe],template:`
<section class="page-heading"><p class="eyebrow">PROSTORI I PROGRAM</p><h1>{{label}}</h1><p>Upravljanje muzejskim prostorima, rasporedom i cenama.</p></section>
<nav class="collection-actions" aria-label="Upravljanje programom">@if(admin()){<a routerLink="/collection/museums">Muzeji</a><a routerLink="/collection/halls">Sale</a>}<a routerLink="/collection/exhibitions">Izložbe</a><a routerLink="/collection/exhibition-terms">Termini</a></nav>
<app-feedback [busy]="busy()" [error]="error()" [notice]="notice()" />
<div class="collection-actions"><a class="button" [routerLink]="['/collection',kind,'new']" [queryParams]="parentQuery">Dodaj zapis</a><button class="button secondary" [disabled]="busy()" (click)="load()">Osveži</button>@if(parentId){<a [routerLink]="['/collection',kind]">Prikaži sve</a>}</div>
<div class="cards">@for(row of rows();track row.id){<article class="panel visit-history-card">
  <h2><a [routerLink]="['/collection',kind,row.id]">{{row.name || 'Termin #'+row.id}}</a></h2>
  @if(row.status){<p><span class="badge">{{row.status|status}}</span></p>}@if(kind==='museums'){<p><span class="badge" [class.success]="row.active" [class.warning]="!row.active">{{row.active?'Aktivan':'Neaktivan'}}</span></p>}
  @if(kind==='halls'){<p>Muzej #{{row.museumId}}</p>}@if(kind==='exhibitions'){<p>Sala #{{row.hallId}} · {{row.onlineVisible?'Javno vidljiva':'Nije javno vidljiva'}}</p>}
  @if(row.start){<p>{{row.start|date:'dd.MM.yyyy. HH:mm'}} — {{row.end|date:'dd.MM.yyyy. HH:mm'}}</p>}
  <div class="collection-actions"><a [routerLink]="['/collection',kind,row.id,'edit']">Izmeni</a>
  @if(kind==='museums'){<a routerLink="/collection/halls" [queryParams]="{museumId:row.id}">Sale muzeja</a>}
  @if(kind==='halls'){<a routerLink="/collection/exhibitions" [queryParams]="{hallId:row.id}">Izložbe u sali</a>}
  @if(kind==='exhibitions'){<a routerLink="/collection/exhibition-terms" [queryParams]="{exhibitionId:row.id}">Termini</a><a [routerLink]="['/collection/exhibitions',row.id,'prices']">Cenovnik</a>}
  </div></article>}@empty{@if(!busy()&&!error()){<p class="empty">Nema zapisa za prikaz.</p>}}</div>
`})
export class CollectionList extends CollectionState {
  private api=inject(Api);private route=inject(ActivatedRoute);private auth=inject(Auth);
  kind=this.route.snapshot.data['kind'] as CollectionKind;label=collectionLabels[this.kind];rows=signal<CollectionRecord[]>([]);
  parentKey=({halls:'museumId',exhibitions:'hallId','exhibition-terms':'exhibitionId'} as Partial<Record<CollectionKind,string>>)[this.kind];
  parentId=this.parentKey?this.route.snapshot.queryParamMap.get(this.parentKey):null;
  parentQuery=this.parentKey&&this.parentId?{[this.parentKey]:this.parentId}:{};
  admin(){return hasCollectionRole(this.auth,true);}
  constructor(){super();this.route.queryParamMap.pipe(takeUntilDestroyed()).subscribe(params=>{
    this.parentId=this.parentKey?params.get(this.parentKey):null;this.parentQuery=this.parentKey&&this.parentId?{[this.parentKey]:this.parentId}:{};this.load();
  });}
  load(){void this.run(async()=>{this.rows.set([]);const rows=await this.api.get<CollectionRecord[]>('/'+this.kind,10000);
    this.rows.set(rows.filter(r=>!this.parentId||r[this.parentKey!]===Number(this.parentId)));
    if(this.route.snapshot.queryParamMap.has('removed'))this.notice.set('Zapis je uklonjen.');});}
}
