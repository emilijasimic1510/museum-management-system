import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Api } from '../core/api';
import { Auth } from '../core/auth';
import { Author, CatalogExhibit } from '../core/catalog';
import { CuratorState, ManagedParticipation } from '../core/curator';
import { Exhibition } from '../core/models';
import { Feedback } from '../shared/feedback';
import { TranslatePipe } from '../core/i18n';

@Component({imports:[RouterLink,Feedback,TranslatePipe],template:`
<section class="page-heading"><p class="eyebrow">{{'uiText.curator.workspace'|t}}</p><h1>{{'uiText.collection.overview'|t}}</h1><p>{{'uiText.manage.objects.authors.and.exhibition.installations'|t}}</p></section>
<app-feedback [busy]="busy()" [error]="error()" [notice]="notice()" />
@if(error()){<button class="button" (click)="load()" [disabled]="busy()">{{'ui.retry'|t}}</button>}
@if(counts();as c){<div class="cards">
<article class="panel"><h2>{{c.exhibits}}</h2><p>{{'uiText.exhibits'|t}}</p><a routerLink="/curator/exhibits">{{'uiText.manage.exhibits'|t}}</a></article>
<article class="panel"><h2>{{c.authors}}</h2><p>{{'uiText.authors'|t}}</p><a routerLink="/curator/authors">{{'uiText.manage.authors'|t}}</a></article>
<article class="panel"><h2>{{c.participations}}</h2><p>{{'uiText.exhibition.installations.2'|t}}</p><small>{{'uiText.active.installations'|t}} {{c.active}} {{'uiText.installed.and.not.yet.removed.in.active.exhibitions.currently.in.progress'|t}}</small><p><a routerLink="/curator/participations">{{'nav.participations'|t}}</a></p></article>
</div>}
<section class="panel"><h2>{{'uiText.quick.actions'|t}}</h2><div class="curator-actions"><a class="button" routerLink="/curator/exhibits/new">{{'uiText.new.exhibit'|t}}</a><a class="button secondary" routerLink="/curator/authors/new">{{'uiText.new.author'|t}}</a><a class="button secondary" routerLink="/curator/participations/new">{{'uiText.add.exhibit.to.exhibition'|t}}</a><a class="button secondary" routerLink="/curator/digital-content/new">{{'uiText.add.digital.content'|t}}</a></div><p class="muted">{{'uiText.content.is.viewed.per.installation.the.total.is.not.calculated.through.extra.r'|t}}</p></section>
`})
export class CuratorOverview extends CuratorState {
  private api=inject(Api);counts=signal<{exhibits:number;authors:number;participations:number;active:number}|null>(null);
  constructor(){super();this.load();}
  load(){void this.run(async()=>{
    const [exhibits,authors,parts,exhibitions]=await Promise.all([this.api.get<CatalogExhibit[]>('/exhibits'),this.api.get<Author[]>('/authors'),this.api.get<ManagedParticipation[]>('/exhibit-participations'),this.api.get<Exhibition[]>('/exhibitions')]);
    const now=Date.now(),active=new Set(exhibitions.filter(e=>e.status==='AKTIVNA'&&Date.parse(e.start)<=now&&Date.parse(e.end)>now).map(e=>e.id));
    this.counts.set({exhibits:exhibits.length,authors:authors.length,participations:parts.length,active:parts.filter(p=>active.has(p.exhibitionId)&&!!p.installedAt&&Date.parse(p.installedAt)<=now&&(!p.removedAt||Date.parse(p.removedAt)>now)).length});
  });}
}
@Component({imports:[Feedback,TranslatePipe],template:`
<section class="page-heading"><h1>{{'uiText.employee.profile'|t}}</h1></section><app-feedback [busy]="busy()" [error]="error()" />
@if(auth.currentAccount();as a){<section class="panel"><h2>{{a.email}}</h2><p>{{auth.isCurator()?('nav.catalogManagement'|t):''}}</p><p>{{'uiText.roles'|t}} {{a.roles.join(', ')}}</p><p>{{'uiText.detailed.employee.information.is.not.available.through.the.current.account.vie'|t}}</p></section>}
@if(error()){<button class="button" (click)="load()">{{'ui.retry'|t}}</button>}
`})
export class CuratorProfile extends CuratorState {
  auth=inject(Auth);constructor(){super();this.load();}load(){void this.run(async()=>{await this.auth.account();});}
}
@Component({imports:[RouterLink,TranslatePipe],template:`<section class="page-heading"><p class="eyebrow">403</p><h1>{{'uiText.access.denied'|t}}</h1><p>{{'uiText.your.role.does.not.allow.access.to.this.page'|t}}</p><a class="button" routerLink="/">{{'uiText.go.to.home'|t}}</a></section>`})
export class ForbiddenPage {}
