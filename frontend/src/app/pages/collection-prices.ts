import { Component, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { FormsModule, NgForm } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Api } from '../core/api';
import { CollectionRecord, CollectionState, ManagedPrice, localTimestamp, toInstant } from '../core/collection-management';
import { Feedback } from '../shared/feedback';

@Component({imports:[DatePipe,DecimalPipe,FormsModule,RouterLink,Feedback],template:`
<a class="back" [routerLink]="['/collection/exhibitions',id]">← Izložba</a><section class="page-heading"><h1>Cenovnik</h1><h2>{{name()}}</h2><p>Istorija cena i važenje po kategoriji. Iznos na već prodatim kartama ostaje sačuvan.</p></section>
<app-feedback [busy]="busy()" [error]="error()" [notice]="notice()" />
<button class="text-button" [disabled]="busy()" (click)="load()">Osveži istoriju cena</button>
@if(loaded()){
<div class="cards">@for(p of prices();track p.category+p.validFrom){<article class="panel visit-history-card"><h2>{{p.category}}</h2><p class="price-value"><strong>{{p.price|number:'1.2-2'}}</strong></p><p>Od {{p.validFrom|date:'dd.MM.yyyy. HH:mm:ss'}}<br>Do {{p.validTo?(p.validTo|date:'dd.MM.yyyy. HH:mm:ss'):'daljeg'}}</p><p class="description">{{p.note}}</p><button type="button" class="button secondary" [disabled]="busy()" (click)="edit(p)">Izmeni cenu / kraj perioda</button></article>}@empty{<p class="empty">Cenovnik još nema periode.</p>}</div>
<form #form="ngForm" class="panel" (ngSubmit)="save(form)"><h2>{{editing()?'Izmena perioda':'Novi cenovni period'}}</h2>
<fieldset class="collection-fields" [disabled]="busy()"><div class="collection-grid">
<label>Kategorija <span class="required-mark" aria-hidden="true">*</span><input name="category" [(ngModel)]="category" required [readonly]="!!editing()"></label>
<label>Važi od <span class="required-mark" aria-hidden="true">*</span><input type="datetime-local" step="0.001" name="validFrom" [(ngModel)]="validFrom" required [readonly]="!!editing()"></label>
<label>Važi do (opciono)<input type="datetime-local" step="0.001" name="validTo" [(ngModel)]="validTo"></label>
<label>Cena <span class="required-mark" aria-hidden="true">*</span><input type="number" name="price" [(ngModel)]="price" required min="0" step="0.01"></label>
<label class="collection-wide">Napomena<textarea name="note" [(ngModel)]="note" rows="3"></textarea></label>
</div><p class="muted">Periodi iste kategorije ne smeju se preklapati. Za novu cenu prvo završite prethodni otvoreni period, pa dodajte sledeći sa istim graničnim datumom. Kategorija i početak postojećeg perioda se ne menjaju.</p>
<div class="collection-actions"><button class="button" [disabled]="form.invalid||busy()">Sačuvaj period</button>@if(editing()){<button class="button secondary" type="button" (click)="reset()">Odustani / novi period</button>}</div></fieldset></form>
}
`})
export class CollectionPrices extends CollectionState {
  private api=inject(Api);id=Number(inject(ActivatedRoute).snapshot.paramMap.get('id'));name=signal('');prices=signal<ManagedPrice[]>([]);loaded=signal(false);
  editing=signal<ManagedPrice|null>(null);category='';validFrom='';validTo='';price:number|null=null;note='';
  get endpoint(){return `/exhibitions/${this.id}/prices/management`;}
  constructor(){super();this.load();}
  load(){void this.run(async()=>{this.loaded.set(false);if(!Number.isSafeInteger(this.id)||this.id<=0){this.error.set('Neispravan ID izložbe.');return;}
    const [e,p]=await Promise.all([this.api.get<CollectionRecord>('/exhibitions/'+this.id,10000),this.api.get<ManagedPrice[]>(this.endpoint,10000)]);this.name.set(e.name||'');this.prices.set(p);this.loaded.set(true);});}
  edit(p:ManagedPrice){this.editing.set(p);this.category=p.category;this.validFrom=localTimestamp(p.validFrom);this.validTo=p.validTo?localTimestamp(p.validTo):'';this.price=p.price;this.note=p.note||'';this.error.set('');this.notice.set('');}
  reset(){this.editing.set(null);this.category='';this.validFrom='';this.validTo='';this.price=null;this.note='';}
  save(form:NgForm){if(form.invalid||this.busy()||!this.loaded())return;void this.run(async()=>{
    const old=this.editing(),from=old?.validFrom||toInstant(this.validFrom),to=this.validTo?toInstant(this.validTo,old?.validTo):null;
    if(!this.category.trim()||(to&&Date.parse(to)<=Date.parse(from))){this.error.set('Kategorija je obavezna, a kraj važenja mora biti nakon početka.');return;}
    const body={validTo:to,price:this.price,note:this.note.trim()||null};
    const saved=old?await this.api.put<ManagedPrice>(this.endpoint+'?category='+encodeURIComponent(old.category)+'&validFrom='+encodeURIComponent(old.validFrom),body)
      :await this.api.post<ManagedPrice>(this.endpoint,{...body,category:this.category.trim(),validFrom:from});
    this.prices.update(rows=>[saved,...rows.filter(p=>p.category!==saved.category||p.validFrom!==saved.validFrom)]);this.reset();this.notice.set('Cenovni period je sačuvan. Plaćene cene postojećih karata nisu promenjene.');
  });}
}
