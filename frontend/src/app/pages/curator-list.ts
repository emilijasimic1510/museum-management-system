import { translate } from './../core/i18n';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Api } from '../core/api';
import { Author, CatalogExhibit } from '../core/catalog';
import { CuratorState, ManagedParticipation } from '../core/curator';
import { Exhibition } from '../core/models';
import { Feedback } from '../shared/feedback';
import { TranslatePipe } from '../core/i18n';

@Component({imports:[FormsModule,RouterLink,Feedback,TranslatePipe],template:`
<section class="page-heading"><p class="eyebrow">{{'uiText.catalog.management'|t}}</p><h1>{{title|t}}</h1><a class="button" [routerLink]="base+'/new'">{{'uiText.add'|t}} {{kind==='authors'?'autora':kind==='exhibits'?'eksponat':('uiText.installation.3'|t)}}</a></section>
<app-feedback [busy]="busy()" [error]="error()" [notice]="notice()" />
<label>{{'catalog.search'|t}}<input type="search" [ngModel]="query()" (ngModelChange)="query.set($event)" [attr.placeholder]="'uiText.enter.a.name'|t"></label>
@if(error()){<button class="button secondary" (click)="load()" [disabled]="busy()">{{'qrBoard.retry'|t}}</button>}
@if(!busy()&&!rows().length&&!error()){<p class="empty">{{'uiText.no.records.yet.add.the.first.record'|t}}</p>}
@else if(!busy()&&rows().length&&!filtered().length){<p class="empty">{{'uiText.no.results.for.this.search'|t}}</p>}
<div class="cards">@for(row of filtered();track row.id){<article class="panel catalog-card"><h2>{{row.name}}</h2><p>{{row.description}}</p><div class="curator-actions"><a class="button secondary" [routerLink]="base+'/'+row.id+'/edit'">{{'uiText.edit'|t}}</a><button class="button danger" [disabled]="busy()" (click)="pending.set(row.id)">{{'uiText.delete'|t}}</button></div>
@if(pending()===row.id){<div class="message" role="alert"><p>{{'uiText.delete.2'|t}}{{row.name}}{{'uiText.this.cannot.be.undone.related.data.may.prevent.deletion'|t}}</p><div class="curator-actions"><button class="button danger" [disabled]="busy()" (click)="remove(row.id)">{{'uiText.confirm.deletion'|t}}</button><button class="button secondary" [disabled]="busy()" (click)="pending.set(null)">{{'uiText.cancel'|t}}</button></div></div>}
</article>}</div>
`})
export class CuratorList extends CuratorState {
  private api=inject(Api);kind=inject(ActivatedRoute).snapshot.data['kind'] as string;
  base='/curator/'+this.kind;title=this.kind==='exhibits'?(translate("nav.exhibits")):this.kind==='authors'?translate("nav.authors"):translate("uiText.exhibition.installations");
  rows=signal<{id:number;name:string;description:string}[]>([]);query=signal('');pending=signal<number|null>(null);
  filtered=computed(()=>this.rows().filter(r=>r.name.toLocaleLowerCase('sr').includes(this.query().trim().toLocaleLowerCase('sr'))));
  constructor(){super();this.load();}
  private async fetch(){
    if(this.kind==='exhibits')this.rows.set((await this.api.get<CatalogExhibit[]>('/exhibits')).map(e=>({id:e.id,name:e.name,description:e.description||''})));
    else if(this.kind==='authors')this.rows.set((await this.api.get<Author[]>('/authors')).map(a=>({id:a.id,name:a.firstName+' '+(a.lastName||''),description:a.biography||''})));
    else{
      const [parts,exhibits,exhibitions]=await Promise.all([this.api.get<ManagedParticipation[]>('/exhibit-participations'),this.api.get<CatalogExhibit[]>('/exhibits'),this.api.get<Exhibition[]>('/exhibitions')]);
      this.rows.set(parts.map(p=>({id:p.id,name:(exhibits.find(e=>e.id===p.exhibitId)?.name||translate("uiText.exhibit.2"))+' — '+(exhibitions.find(e=>e.id===p.exhibitionId)?.name||translate("ui.exhibition")),description:p.contextDescription||''})));
    }
  }
  load(){void this.run(()=>this.fetch());}
  remove(id:number){if(this.pending()!==id)return;void this.run(async()=>{await this.api.delete('/'+(this.kind==='participations'?'exhibit-participations':this.kind)+'/'+id);this.pending.set(null);this.rows.update(rows=>rows.filter(r=>r.id!==id));this.notice.set(translate("uiText.record.deleted"));});}
}
