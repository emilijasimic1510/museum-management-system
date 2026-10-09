import { LocalizedContentPipe } from '../core/i18n';
import { translate } from './../core/i18n';
import { Component, inject, signal } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { LocalDatePipe as DatePipe } from '../core/i18n';
import { Auth } from '../core/auth';
import { Api, PageState } from '../core/api';
import { Ticket, Visit } from '../core/models';
import { Feedback } from '../shared/feedback';
import { TranslatePipe } from '../core/i18n';
import { CashierSale } from './cashier-sale';
import { I18n } from '../core/i18n';

@Component({imports:[LocalizedContentPipe,FormsModule,DatePipe,Feedback,CashierSale,TranslatePipe],template:`
<section class="page-heading"><p class="eyebrow">{{'uiText.museum.entrance'|t}}</p><h1>{{'cashier.title'|t}}</h1><p><span class="badge">{{'role.CASHIER'|t}}</span> {{auth.currentAccount()?.email}}</p><p>{{'cashier.saleAnonymous'|t}}</p></section>
<app-cashier-sale />
<app-feedback [busy]="busy()" [error]="error()" [notice]="notice()" />
<div class="detail-layout"><section class="panel" id="validation"><h2>{{'cashier.validation'|t}}</h2><form #form="ngForm" (ngSubmit)="validate(form)">
<label>{{'ui.scanToken'|t}} <span class="required-mark" aria-hidden="true">*</span><input name="qrToken" [(ngModel)]="qrToken" required autocomplete="off" [attr.placeholder]="'uiText.paste.the.ticket.token'|t"></label>
<label>{{'uiText.time.slot.id'|t}} <span class="required-mark" aria-hidden="true">*</span><input name="termId" type="number" [(ngModel)]="termId" required min="1" [attr.placeholder]="'uiText.e.g.12'|t"></label>
<button class="button" [disabled]="busy()||form.invalid||!qrToken.trim()" type="submit">{{busy()?('uiText.checking'|t):('cashier.validate'|t)}}</button></form>
<p class="muted small-text">{{'uiText.scanning.reads.the.same.token.if.the.scanner.copies.it.into.this.field.also.en'|t}}</p></section>
<section class="panel" aria-live="polite"><h2>{{'uiText.last.validation'|t}}</h2>@if(result();as v){<span class="badge success">{{'uiText.valid.active'|t}}</span><dl><div><dt>{{'visitor.visit'|t}}</dt><dd>#{{v.id}}</dd></div><div><dt>{{'uiText.ticket'|t}}</dt><dd>#{{v.ticketId}}</dd></div><div><dt>{{'uiText.entry'|t}}</dt><dd>{{v.enteredAt|date:'dd.MM.yyyy. HH:mm'}}</dd></div></dl>@if(ticket();as t){<p><strong>{{ticketLabel(t)}}</strong><br>{{(t|localized:'category')}} {{'uiText.time.slot'|t}}{{t.termId}} {{'uiText.valid.until'|t}} {{t.validUntil|date:'dd.MM.yyyy. HH:mm'}}</p>}}@else{<p class="muted">{{'uiText.the.validation.result.will.appear.here'|t}}</p>}</section></div>`})
export class CashierPage extends PageState {
  readonly auth=inject(Auth);private api=inject(Api);private i18n=inject(I18n); qrToken=''; termId:number|null=null; result=signal<Visit|null>(null); ticket=signal<Ticket|null>(null);
  validate(form:NgForm){
    if(form.invalid||!this.termId||!this.qrToken.trim()||this.busy())return;
    void this.run(async()=>{
      this.result.set(null);this.ticket.set(null);
      const visit=await this.api.post<Visit>('/cashier/validate',{qrToken:this.qrToken.trim(),termId:Number(this.termId)});
      this.result.set(visit);this.qrToken='';
      this.notice.set(this.i18n.t('cashier.validationSuccess'));
      try{this.ticket.set(await this.api.get<Ticket>('/tickets/'+visit.ticketId));}
      catch{this.error.set(this.i18n.t('cashier.validationDetailsError'));}
    });
  }  ticketLabel(ticket:Ticket){return ticket.status==='AKTIVNA'?translate("uiText.ticket.used.for.an.active.visit"):ticket.status;}
}
