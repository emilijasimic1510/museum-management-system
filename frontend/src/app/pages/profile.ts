import { Component, inject, signal } from '@angular/core';
import { LocalDatePipe as DatePipe } from '../core/i18n';
import { FormsModule, NgForm } from '@angular/forms';
import { Api, PageState } from '../core/api';
import { Account, Visitor } from '../core/models';
import { Feedback } from '../shared/feedback';
import { I18n, TranslatePipe } from '../core/i18n';
@Component({imports:[FormsModule,DatePipe,Feedback,TranslatePipe],template:`
<section class="page-heading"><p class="eyebrow">{{'uiText.your.account'|t}}</p><h1>{{'ui.profile'|t}}</h1><p>{{'uiText.your.details.and.recommendation.preferences.in.one.place'|t}}</p></section>
<app-feedback [busy]="busy()" [error]="error()" [notice]="notice()" />
@if(error()&&!visitor()){<button class="button" (click)="load()">{{'ui.retry'|t}}</button>}
@if(visitor();as v){<div class="profile-layout"><aside class="panel profile-summary"><span class="avatar">{{v.firstName.slice(0,1)}}{{v.lastName.slice(0,1)}}</span><h2>{{v.firstName}} {{v.lastName}}</h2><p>{{account()?.email}}</p><span class="badge">{{'role.VISITOR'|t}}</span><p class="muted">{{'uiText.member.since'|t}} {{account()?.registeredAt|date:'dd.MM.yyyy.'}}</p></aside><section class="panel"><h2>{{'uiText.personal.details'|t}}</h2><form #form="ngForm" (ngSubmit)="save(form)"><div class="form-row"><label>{{'login.firstName'|t}} <span class="required-mark" aria-hidden="true">*</span><input name="firstName" [(ngModel)]="firstName" required></label><label>{{'login.lastName'|t}} <span class="required-mark" aria-hidden="true">*</span><input name="lastName" [(ngModel)]="lastName" required></label></div><label>{{'uiText.phone.optional'|t}}<input name="phone" type="tel" [(ngModel)]="phone"></label><label class="checkbox"><input name="consent" type="checkbox" [(ngModel)]="consent"><span>{{'uiText.personalized.recommendations'|t}}<small>{{'uiText.i.allow.my.activity.to.be.used.for.exhibition.and.exhibit.recommendations'|t}}</small></span></label>@if(form.invalid&&form.submitted){<p class="field-error">{{'uiText.first.and.last.names.are.required'|t}}</p>}<button class="button" [disabled]="busy()" type="submit">{{'ui.save'|t}}</button></form></section></div>}
`})
export class ProfilePage extends PageState {
  private api=inject(Api); private i18n=inject(I18n); visitor=signal<Visitor|null>(null);account=signal<Account|null>(null);
  firstName='';lastName='';phone='';consent=false;
  constructor(){super();this.load();}
  load(){void this.run(async()=>{const [v,a]=await Promise.all([this.api.get<Visitor>('/visitors/me'),this.api.get<Account>('/accounts/me')]);this.visitor.set(v);this.account.set(a);this.firstName=v.firstName;this.lastName=v.lastName;this.phone=v.phone||'';this.consent=v.recommendationsConsent;});}
  save(form:NgForm){if(form.invalid)return;void this.run(async()=>{this.visitor.set(await this.api.put<Visitor>('/visitors/me',{firstName:this.firstName.trim(),lastName:this.lastName.trim(),phone:this.phone.trim(),recommendationsConsent:this.consent}));this.notice.set(this.i18n.t('ui.saveSuccess'));});}
}
