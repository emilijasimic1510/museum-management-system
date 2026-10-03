import { Component, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Api, PageState } from '../core/api';
import { Auth } from '../core/auth';
import { Exhibition, Price, Term, Ticket, Visitor } from '../core/models';
import { Feedback } from '../shared/feedback';
import { StatusPipe } from '../shared/status';

@Component({imports:[DatePipe,DecimalPipe,FormsModule,RouterLink,Feedback,StatusPipe],template:`
<a class="back" routerLink="/">← Sve izložbe</a><app-feedback [busy]="busy()" [error]="error()" [notice]="notice()" />
@if(error()&&!exhibition()){<button class="button" (click)="load()">Pokušaj ponovo</button>}
@if(exhibition();as e){<section class="page-heading"><p class="eyebrow">IZLOŽBA · {{e.status|status}}</p><h1>{{e.name}}</h1><p>{{e.start|date:'dd.MM.yyyy.'}} — {{e.end|date:'dd.MM.yyyy.'}}</p></section>
<div class="detail-layout"><section><div class="panel"><h2>O izložbi</h2><p class="description">{{e.description||'Opis izložbe još nije dodat.'}}</p></div><div class="panel"><h2>Termini posete</h2><p class="muted">Izaberite otvoren termin. Dostupnost mesta potvrđuje se pri kupovini.</p>
@for(t of terms();track t.id){<button class="term" [class.selected]="termId===t.id" [disabled]="!available(t)||busy()" (click)="termId=t.id"><span><strong>{{t.start|date:'dd.MM.yyyy.'}}</strong><small>{{t.start|date:'HH:mm'}} — {{t.end|date:'HH:mm'}} · kapacitet {{t.capacity}}</small></span><span class="badge">{{t.status|status}}</span></button>}@empty{<p class="empty-inline">Nema objavljenih termina.</p>}</div></section>
<aside class="panel purchase"><p class="eyebrow">VAŠA SLEDEĆA POSETA</p><h2>Ulaznica</h2><form #form="ngForm" (ngSubmit)="buy()"><label>Termin <span class="required-mark" aria-hidden="true">*</span><select name="term" [(ngModel)]="termId" required><option [ngValue]="null">Izaberite termin</option>@for(t of terms();track t.id){<option [ngValue]="t.id" [disabled]="!available(t)">{{t.start|date:'dd.MM.yyyy. HH:mm'}} · {{t.status|status}}</option>}</select></label><label>Kategorija <span class="required-mark" aria-hidden="true">*</span><select name="category" [(ngModel)]="category" required><option value="">Izaberite kategoriju</option>@for(p of prices();track p.category){<option [value]="p.category">{{p.category}} · {{p.price|number:'1.2-2'}}</option>}</select></label>
@if(!prices().length){<p class="message">Za ovu izložbu trenutno nema važećeg cenovnika.</p>}
<div class="total"><span>Cena</span><strong>{{selectedPrice()===null?'—':(selectedPrice()|number:'1.2-2')}}</strong></div><p class="muted small-text">Iznos iz važećeg cenovnika. Online kupovina koristi karticu. Lokalna demonstracija ne vrši stvarnu naplatu.</p>
@if(auth.session()){<button type="submit" class="button full" [disabled]="form.invalid||busy()||!canBuy()">{{busy()?'Obrada…':'Kupi ulaznicu'}} ↗</button>}@else{<a class="button full" routerLink="/login" [queryParams]="{returnUrl:'/exhibitions/'+e.id}">Prijavite se za kupovinu ↗</a>}
</form></aside></div>}
`})
export class ExhibitionPage extends PageState {
  private api=inject(Api); readonly auth=inject(Auth);private route=inject(ActivatedRoute);private router=inject(Router);
  exhibition=signal<Exhibition|null>(null);terms=signal<Term[]>([]);prices=signal<Price[]>([]);visitor=signal<Visitor|null>(null);termId:number|null=null;category='';
  constructor(){super();this.load();}
  load(){void this.run(async()=>{const id=Number(this.route.snapshot.paramMap.get('id'));const [e,p]=await Promise.all([this.api.get<Exhibition>('/exhibitions/'+id),this.api.get<Price[]>('/prices?exhibitionId='+id)]);if(!e.onlineVisible){this.error.set('Ova izložba nije javno objavljena.');return;}this.exhibition.set(e);this.terms.set(e.terms);const seen=new Set<string>();this.prices.set(p.filter(x=>{if(seen.has(x.category))return false;seen.add(x.category);return true;}));if(this.auth.session())this.visitor.set(await this.api.get<Visitor>('/visitors/me'));});}
  available(t:Term){return t.status==='OTVOREN'&&Date.parse(t.end)>Date.now()&&!['ZAVRSENA','OTKAZANA'].includes(this.exhibition()?.status||'');}
  canBuy(){return this.terms().some(t=>t.id===this.termId&&this.available(t))&&this.selectedPrice()!==null;}
  selectedPrice(){return this.prices().find(p=>p.category===this.category)?.price??null;}
  buy(){if(!this.canBuy()||!this.visitor())return;void this.run(async()=>{const t=await this.api.post<Ticket>('/tickets',{termId:this.termId,visitorId:this.visitor()!.id,category:this.category,paymentMethod:'KARTICA',demoDecline:false});await this.router.navigate(['/tickets'],{queryParams:{purchased:t.id}});});}
}
