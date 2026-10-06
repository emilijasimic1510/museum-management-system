import { TranslatePipe, translate } from './../core/i18n';
import { MediaImage } from '../shared/media-image';
import { Component, inject, signal } from '@angular/core';
import { LocalDatePipe as DatePipe, LocalNumberPipe as DecimalPipe } from '../core/i18n';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Api, PageState } from '../core/api';
import { Auth } from '../core/auth';
import { Exhibition, Price, Term, Ticket, Visitor } from '../core/models';
import { Feedback } from '../shared/feedback';
import { StatusPipe } from '../shared/status';

@Component({imports:[TranslatePipe,MediaImage,DatePipe,DecimalPipe,FormsModule,RouterLink,Feedback,StatusPipe],template:`
<a class="back" routerLink="/">{{'uiText.all.exhibitions'|t}}</a><app-feedback [busy]="busy()" [error]="error()" [notice]="notice()" />
@if(error()&&!exhibition()){<button class="button" (click)="load()">{{'ui.retry'|t}}</button>}
@if(exhibition();as e){<section class="page-heading"><p class="eyebrow">{{'uiText.exhibition.4'|t}} {{e.status|status}}</p><h1>{{e.name}}</h1><p>{{e.start|date:'dd.MM.yyyy.'}} — {{e.end|date:'dd.MM.yyyy.'}}</p></section>
<app-media-image [path]="'/media/exhibitions/'+e.id" [alt]="e.name" [contain]="true" /><div class="detail-layout"><section><div class="panel"><h2>{{'uiText.about.the.exhibition'|t}}</h2><p class="description">{{e.description||('uiText.no.exhibition.description.yet'|t)}}</p></div><div class="panel"><h2>{{'uiText.visit.time.slots'|t}}</h2><p class="muted">{{'uiText.choose.an.open.time.slot.availability.is.confirmed.at.purchase'|t}}</p>
@for(t of terms();track t.id){<button class="term" [class.selected]="termId===t.id" [disabled]="!available(t)||busy()" (click)="termId=t.id"><span><strong>{{t.start|date:'dd.MM.yyyy.'}}</strong><small>{{t.start|date:'HH:mm'}} — {{t.end|date:'HH:mm'}} {{'uiText.capacity'|t}} {{t.capacity}}</small></span><span class="badge">{{t.status|status}}</span></button>}@empty{<p class="empty-inline">{{'uiText.no.published.time.slots'|t}}</p>}</div></section>
<aside class="panel purchase"><p class="eyebrow">{{'uiText.your.next.visit'|t}}</p><h2>{{'uiText.ticket'|t}}</h2><form #form="ngForm" (ngSubmit)="buy()"><label>{{'ui.term'|t}} <span class="required-mark" aria-hidden="true">*</span><select name="term" [(ngModel)]="termId" required><option [ngValue]="null">{{'ui.chooseTerm'|t}}</option>@for(t of terms();track t.id){<option [ngValue]="t.id" [disabled]="!available(t)">{{t.start|date:'dd.MM.yyyy. HH:mm'}} · {{t.status|status}}</option>}</select></label><label>{{'ui.category'|t}} <span class="required-mark" aria-hidden="true">*</span><select name="category" [(ngModel)]="category" required><option value="">{{'ui.chooseCategory'|t}}</option>@for(p of prices();track p.category){<option [value]="p.category">{{p.category}} · {{p.price|number:'1.2-2'}}</option>}</select></label>
@if(!prices().length){<p class="message">{{'uiText.there.is.no.current.price.list.for.this.exhibition'|t}}</p>}
<div class="total"><span>{{'ui.price'|t}}</span><strong>{{selectedPrice()===null?'—':(selectedPrice()|number:'1.2-2')}}</strong></div><p class="muted small-text">{{'uiText.amount.from.the.current.price.list.online.purchases.use.a.card.the.local.demo.'|t}}</p>
@if(auth.session()){<button type="submit" class="button full" [disabled]="form.invalid||busy()||!canBuy()">{{busy()?('uiText.processing'|t):('uiText.buy.ticket'|t)}} ↗</button>}@else{<a class="button full" routerLink="/login" [queryParams]="{returnUrl:'/exhibitions/'+e.id}">{{'uiText.sign.in.to.purchase'|t}}</a>}
</form></aside></div>}
`})
export class ExhibitionPage extends PageState {
  private api=inject(Api); readonly auth=inject(Auth);private route=inject(ActivatedRoute);private router=inject(Router);
  exhibition=signal<Exhibition|null>(null);terms=signal<Term[]>([]);prices=signal<Price[]>([]);visitor=signal<Visitor|null>(null);termId:number|null=null;category='';
  constructor(){super();this.load();}
  load(){void this.run(async()=>{const id=Number(this.route.snapshot.paramMap.get('id'));const [e,p]=await Promise.all([this.api.get<Exhibition>('/exhibitions/'+id),this.api.get<Price[]>('/prices?exhibitionId='+id)]);if(!e.onlineVisible){this.error.set(translate("uiText.this.exhibition.is.not.publicly.listed"));return;}this.exhibition.set(e);this.terms.set(e.terms);const seen=new Set<string>();this.prices.set(p.filter(x=>{if(seen.has(x.category))return false;seen.add(x.category);return true;}));if(this.auth.session())this.visitor.set(await this.api.get<Visitor>('/visitors/me'));});}
  available(t:Term){return t.status==='OTVOREN'&&Date.parse(t.end)>Date.now()&&!['ZAVRSENA','OTKAZANA'].includes(this.exhibition()?.status||'');}
  canBuy(){return this.terms().some(t=>t.id===this.termId&&this.available(t))&&this.selectedPrice()!==null;}
  selectedPrice(){return this.prices().find(p=>p.category===this.category)?.price??null;}
  buy(){if(!this.canBuy()||!this.visitor())return;void this.run(async()=>{const t=await this.api.post<Ticket>('/tickets',{termId:this.termId,visitorId:this.visitor()!.id,category:this.category,paymentMethod:'KARTICA',demoDecline:false});await this.router.navigate(['/tickets'],{queryParams:{purchased:t.id}});});}
}
