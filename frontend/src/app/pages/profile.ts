import { Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule, NgForm } from '@angular/forms';
import { Api, PageState } from '../core/api';
import { Account, Visitor } from '../core/models';
import { Feedback } from '../shared/feedback';
@Component({imports:[FormsModule,DatePipe,Feedback],template:`
<section class="page-heading"><p class="eyebrow">VAŠ NALOG</p><h1>Moj profil</h1><p>Vaši podaci i izbor preporuka, na jednom mestu.</p></section>
<app-feedback [busy]="busy()" [error]="error()" [notice]="notice()" />
@if(error()&&!visitor()){<button class="button" (click)="load()">Pokušaj ponovo</button>}
@if(visitor();as v){<div class="profile-layout"><aside class="panel profile-summary"><span class="avatar">{{v.firstName.slice(0,1)}}{{v.lastName.slice(0,1)}}</span><h2>{{v.firstName}} {{v.lastName}}</h2><p>{{account()?.email}}</p><span class="badge">Posetilac</span><p class="muted">Član od {{account()?.registeredAt|date:'dd.MM.yyyy.'}}</p></aside><section class="panel"><h2>Lični podaci</h2><form #form="ngForm" (ngSubmit)="save(form)"><div class="form-row"><label>Ime <span class="required-mark" aria-hidden="true">*</span><input name="firstName" [(ngModel)]="firstName" required></label><label>Prezime <span class="required-mark" aria-hidden="true">*</span><input name="lastName" [(ngModel)]="lastName" required></label></div><label>Telefon (opciono)<input name="phone" type="tel" [(ngModel)]="phone"></label><label class="checkbox"><input name="consent" type="checkbox" [(ngModel)]="consent"><span>Personalizovane preporuke<small>Dozvoljavam da se moje aktivnosti koriste za preporuke izložbi i eksponata.</small></span></label>@if(form.invalid&&form.submitted){<p class="field-error">Ime i prezime su obavezni.</p>}<button class="button" [disabled]="busy()" type="submit">Sačuvaj izmene</button></form></section></div>}
`})
export class ProfilePage extends PageState {
  private api=inject(Api); visitor=signal<Visitor|null>(null);account=signal<Account|null>(null);
  firstName='';lastName='';phone='';consent=false;
  constructor(){super();this.load();}
  load(){void this.run(async()=>{const [v,a]=await Promise.all([this.api.get<Visitor>('/visitors/me'),this.api.get<Account>('/accounts/me')]);this.visitor.set(v);this.account.set(a);this.firstName=v.firstName;this.lastName=v.lastName;this.phone=v.phone||'';this.consent=v.recommendationsConsent;});}
  save(form:NgForm){if(form.invalid)return;void this.run(async()=>{this.visitor.set(await this.api.put<Visitor>('/visitors/me',{firstName:this.firstName.trim(),lastName:this.lastName.trim(),phone:this.phone.trim(),recommendationsConsent:this.consent}));this.notice.set('Podaci su sačuvani.');});}
}
