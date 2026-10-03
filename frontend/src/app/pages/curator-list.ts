import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Api } from '../core/api';
import { Author, CatalogExhibit } from '../core/catalog';
import { CuratorState, ManagedParticipation } from '../core/curator';
import { Exhibition } from '../core/models';
import { Feedback } from '../shared/feedback';

@Component({imports:[FormsModule,RouterLink,Feedback],template:`
<section class="page-heading"><p class="eyebrow">UPRAVLJANJE KATALOGOM</p><h1>{{title}}</h1><a class="button" [routerLink]="base+'/new'">Dodaj {{kind==='authors'?'autora':kind==='exhibits'?'eksponat':'učešće'}}</a></section>
<app-feedback [busy]="busy()" [error]="error()" [notice]="notice()" />
<label>Pretraga<input type="search" [ngModel]="query()" (ngModelChange)="query.set($event)" placeholder="Unesite naziv ili ime"></label>
@if(error()){<button class="button secondary" (click)="load()" [disabled]="busy()">Ponovo učitaj</button>}
@if(!busy()&&!rows().length&&!error()){<p class="empty">Još nema zapisa. Dodajte prvi zapis.</p>}
@else if(!busy()&&rows().length&&!filtered().length){<p class="empty">Nema rezultata za ovu pretragu.</p>}
<div class="cards">@for(row of filtered();track row.id){<article class="panel catalog-card"><h2>{{row.name}}</h2><p>{{row.description}}</p><div class="curator-actions"><a class="button secondary" [routerLink]="base+'/'+row.id+'/edit'">Izmeni</a><button class="button danger" [disabled]="busy()" (click)="pending.set(row.id)">Obriši</button></div>
@if(pending()===row.id){<div class="message" role="alert"><p>Obrisati „{{row.name}}“? Ova radnja se ne može opozvati. Povezani podaci mogu sprečiti brisanje.</p><div class="curator-actions"><button class="button danger" [disabled]="busy()" (click)="remove(row.id)">Potvrdi brisanje</button><button class="button secondary" [disabled]="busy()" (click)="pending.set(null)">Odustani</button></div></div>}
</article>}</div>
`})
export class CuratorList extends CuratorState {
  private api=inject(Api);kind=inject(ActivatedRoute).snapshot.data['kind'] as string;
  base='/curator/'+this.kind;title=this.kind==='exhibits'?'Eksponati':this.kind==='authors'?'Autori':'Postavke izložbi';
  rows=signal<{id:number;name:string;description:string}[]>([]);query=signal('');pending=signal<number|null>(null);
  filtered=computed(()=>this.rows().filter(r=>r.name.toLocaleLowerCase('sr').includes(this.query().trim().toLocaleLowerCase('sr'))));
  constructor(){super();this.load();}
  private async fetch(){
    if(this.kind==='exhibits')this.rows.set((await this.api.get<CatalogExhibit[]>('/exhibits')).map(e=>({id:e.id,name:e.name,description:e.description||''})));
    else if(this.kind==='authors')this.rows.set((await this.api.get<Author[]>('/authors')).map(a=>({id:a.id,name:a.firstName+' '+(a.lastName||''),description:a.biography||''})));
    else{
      const [parts,exhibits,exhibitions]=await Promise.all([this.api.get<ManagedParticipation[]>('/exhibit-participations'),this.api.get<CatalogExhibit[]>('/exhibits'),this.api.get<Exhibition[]>('/exhibitions')]);
      this.rows.set(parts.map(p=>({id:p.id,name:(exhibits.find(e=>e.id===p.exhibitId)?.name||'Eksponat')+' — '+(exhibitions.find(e=>e.id===p.exhibitionId)?.name||'Izložba'),description:p.contextDescription||''})));
    }
  }
  load(){void this.run(()=>this.fetch());}
  remove(id:number){if(this.pending()!==id)return;void this.run(async()=>{await this.api.delete('/'+(this.kind==='participations'?'exhibit-participations':this.kind)+'/'+id);this.pending.set(null);this.rows.update(rows=>rows.filter(r=>r.id!==id));this.notice.set('Zapis je obrisan.');});}
}
