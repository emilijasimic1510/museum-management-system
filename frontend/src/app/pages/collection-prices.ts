import { translate } from './../core/i18n';
import { Component, inject, signal } from '@angular/core';
import { LocalDatePipe as DatePipe, LocalNumberPipe as DecimalPipe } from '../core/i18n';
import { FormsModule, NgForm } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Api } from '../core/api';
import { CollectionRecord, CollectionState, ManagedPrice, localTimestamp, toInstant } from '../core/collection-management';
import { Feedback } from '../shared/feedback';
import { TranslatePipe } from '../core/i18n';

@Component({imports:[DatePipe,DecimalPipe,FormsModule,RouterLink,Feedback,TranslatePipe],template:`
<a class="back" [routerLink]="['/collection/exhibitions',id]">{{'uiText.exhibition'|t}}</a><section class="page-heading"><h1>{{'uiText.price.list'|t}}</h1><h2>{{name()}}</h2><p>{{'uiText.price.history.and.validity.by.category.prices.on.tickets.already.sold.are.reta'|t}}</p></section>
<app-feedback [busy]="busy()" [error]="error()" [notice]="notice()" />
<button class="text-button" [disabled]="busy()" (click)="load()">{{'uiText.refresh.price.history'|t}}</button>
@if(loaded()){
<div class="cards">@for(p of prices();track p.category+p.validFrom){<article class="panel visit-history-card"><h2>{{p.category}}</h2><p class="price-value"><strong>{{p.price|number:'1.2-2'}}</strong></p><p>{{'uiText.from'|t}} {{p.validFrom|date:'dd.MM.yyyy. HH:mm:ss'}}<br>{{'uiText.to'|t}} {{p.validTo?(p.validTo|date:'dd.MM.yyyy. HH:mm:ss'):('ui.openEnded'|t)}}</p><p class="description">{{p.note}}</p><button type="button" class="button secondary" [disabled]="busy()" (click)="edit(p)">{{'uiText.edit.price.period.end'|t}}</button></article>}@empty{<p class="empty">{{'uiText.no.pricing.periods.yet'|t}}</p>}</div>
<form #form="ngForm" class="panel" (ngSubmit)="save(form)"><h2>{{editing()?('uiText.edit.period'|t):('uiText.new.pricing.period'|t)}}</h2>
<fieldset class="collection-fields" [disabled]="busy()"><div class="collection-grid">
<label>{{'ui.category'|t}} <span class="required-mark" aria-hidden="true">*</span><input name="category" [(ngModel)]="category" required [readonly]="!!editing()"></label>
<label>{{'uiText.valid.from'|t}} <span class="required-mark" aria-hidden="true">*</span><input type="datetime-local" step="0.001" name="validFrom" [(ngModel)]="validFrom" required [readonly]="!!editing()"></label>
<label>{{'uiText.valid.until.optional'|t}}<input type="datetime-local" step="0.001" name="validTo" [(ngModel)]="validTo"></label>
<label>{{'ui.price'|t}} <span class="required-mark" aria-hidden="true">*</span><input type="number" name="price" [(ngModel)]="price" required min="0" step="0.01"></label>
<label class="collection-wide">{{'uiText.note'|t}}<textarea name="note" [(ngModel)]="note" rows="3"></textarea></label>
</div><p class="muted">{{'uiText.periods.in.the.same.category.must.not.overlap.close.the.previous.open.period.b'|t}}</p>
<div class="collection-actions"><button class="button" [disabled]="form.invalid||busy()">{{'uiText.save.period'|t}}</button>@if(editing()){<button class="button secondary" type="button" (click)="reset()">{{'uiText.cancel.new.period'|t}}</button>}</div></fieldset></form>
}
`})
export class CollectionPrices extends CollectionState {
  private api=inject(Api);id=Number(inject(ActivatedRoute).snapshot.paramMap.get('id'));name=signal('');prices=signal<ManagedPrice[]>([]);loaded=signal(false);
  editing=signal<ManagedPrice|null>(null);category='';validFrom='';validTo='';price:number|null=null;note='';
  get endpoint(){return `/exhibitions/${this.id}/prices/management`;}
  constructor(){super();this.load();}
  load(){void this.run(async()=>{this.loaded.set(false);if(!Number.isSafeInteger(this.id)||this.id<=0){this.error.set(translate("uiText.invalid.exhibition.id"));return;}
    const [e,p]=await Promise.all([this.api.get<CollectionRecord>('/exhibitions/'+this.id,10000),this.api.get<ManagedPrice[]>(this.endpoint,10000)]);this.name.set(e.name||'');this.prices.set(p);this.loaded.set(true);});}
  edit(p:ManagedPrice){this.editing.set(p);this.category=p.category;this.validFrom=localTimestamp(p.validFrom);this.validTo=p.validTo?localTimestamp(p.validTo):'';this.price=p.price;this.note=p.note||'';this.error.set('');this.notice.set('');}
  reset(){this.editing.set(null);this.category='';this.validFrom='';this.validTo='';this.price=null;this.note='';}
  save(form:NgForm){if(form.invalid||this.busy()||!this.loaded())return;void this.run(async()=>{
    const old=this.editing(),from=old?.validFrom||toInstant(this.validFrom),to=this.validTo?toInstant(this.validTo,old?.validTo):null;
    if(!this.category.trim()||(to&&Date.parse(to)<=Date.parse(from))){this.error.set(translate("uiText.category.is.required.and.validity.must.end.after.it.starts"));return;}
    const body={validTo:to,price:this.price,note:this.note.trim()||null};
    const saved=old?await this.api.put<ManagedPrice>(this.endpoint+'?category='+encodeURIComponent(old.category)+'&validFrom='+encodeURIComponent(old.validFrom),body)
      :await this.api.post<ManagedPrice>(this.endpoint,{...body,category:this.category.trim(),validFrom:from});
    this.prices.update(rows=>[saved,...rows.filter(p=>p.category!==saved.category||p.validFrom!==saved.validFrom)]);this.reset();this.notice.set(translate("uiText.pricing.period.saved.paid.prices.on.existing.tickets.are.unchanged"));
  });}
}
