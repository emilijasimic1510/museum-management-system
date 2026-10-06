import { translate } from './../core/i18n';
import { Component, inject, signal } from '@angular/core';
import { LocalDatePipe as DatePipe } from '../core/i18n';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Api } from '../core/api';
import { Auth } from '../core/auth';
import { CollectionKind, CollectionRecord, CollectionState, collectionLabels, hasCollectionRole } from '../core/collection-management';
import { Feedback } from '../shared/feedback';
import { TranslatePipe } from '../core/i18n';
import { StatusPipe } from '../shared/status';

@Component({imports:[RouterLink,DatePipe,Feedback,StatusPipe,TranslatePipe],template:`
<section class="page-heading"><p class="eyebrow">{{'uiText.spaces.and.program'|t}}</p><h1>{{label|t}}</h1><p>{{'uiText.manage.museum.spaces.schedules.and.prices'|t}}</p></section>
<nav class="collection-actions" [attr.aria-label]="'uiText.program.management'|t">@if(admin()){<a routerLink="/collection/museums">{{'uiText.museums'|t}}</a><a routerLink="/collection/halls">{{'uiText.halls'|t}}</a>}<a routerLink="/collection/exhibitions">{{'uiText.exhibitions'|t}}</a><a routerLink="/collection/exhibition-terms">{{'uiText.time.slots'|t}}</a></nav>
<app-feedback [busy]="busy()" [error]="error()" [notice]="notice()" />
<div class="collection-actions"><a class="button" [routerLink]="['/collection',kind,'new']" [queryParams]="parentQuery">{{'uiText.add.record'|t}}</a><button class="button secondary" [disabled]="busy()" (click)="load()">{{'uiText.refresh'|t}}</button>@if(parentId){<a [routerLink]="['/collection',kind]">{{'uiText.show.all'|t}}</a>}</div>
<div class="cards">@for(row of rows();track row.id){<article class="panel visit-history-card">
  <h2><a [routerLink]="['/collection',kind,row.id]">{{row.name || ('uiText.time.slot.2'|t)+row.id}}</a></h2>
  @if(row.status){<p><span class="badge">{{row.status|status}}</span></p>}@if(kind==='museums'){<p><span class="badge" [class.success]="row.active" [class.warning]="!row.active">{{row.active?('uiText.active.2'|t):('uiText.inactive.2'|t)}}</span></p>}
  @if(kind==='halls'){<p>{{'uiText.museum.2'|t}}{{row.museumId}}</p>}@if(kind==='exhibitions'){<p>{{'uiText.hall'|t}}{{row.hallId}} · {{row.onlineVisible?('uiText.publicly.visible'|t):('uiText.not.publicly.visible'|t)}}</p>}
  @if(row.start){<p>{{row.start|date:'dd.MM.yyyy. HH:mm'}} — {{row.end|date:'dd.MM.yyyy. HH:mm'}}</p>}
  <div class="collection-actions"><a [routerLink]="['/collection',kind,row.id,'edit']">{{'uiText.edit'|t}}</a>
  @if(kind==='museums'){<a routerLink="/collection/halls" [queryParams]="{museumId:row.id}">{{'uiText.museum.halls'|t}}</a>}
  @if(kind==='halls'){<a routerLink="/collection/exhibitions" [queryParams]="{hallId:row.id}">{{'uiText.exhibitions.in.this.hall'|t}}</a>}
  @if(kind==='exhibitions'){<a routerLink="/collection/exhibition-terms" [queryParams]="{exhibitionId:row.id}">{{'uiText.time.slots'|t}}</a><a [routerLink]="['/collection/exhibitions',row.id,'prices']">{{'uiText.price.list'|t}}</a>}
  </div></article>}@empty{@if(!busy()&&!error()){<p class="empty">{{'uiText.no.records.to.display'|t}}</p>}}</div>
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
    if(this.route.snapshot.queryParamMap.has('removed'))this.notice.set(translate("uiText.record.removed"));});}
}
