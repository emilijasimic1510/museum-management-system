import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Api, PageState } from '../core/api';
import { Visit, Ticket } from '../core/models';
import { scanToken } from '../core/scan';
import { Feedback } from '../shared/feedback';
import { TranslatePipe } from '../core/i18n';

@Component({imports:[FormsModule,RouterLink,Feedback,TranslatePipe],template:`
<section class="page-heading"><h1>{{'visitor.scanExhibit'|t}}</h1><p>{{'uiText.choose.your.active.visit.to.the.exhibition.you.are.in'|t}}</p></section>
<app-feedback [busy]="busy()" [error]="error()" />
@if(visits().length){<form class="panel" (ngSubmit)="open()">
<label>{{'uiText.active.visit'|t}} <span class="required-mark" aria-hidden="true">*</span><select name="visit" [(ngModel)]="selected" required>@for(v of visits();track v.id){<option [ngValue]="v.id">{{'uiText.visit'|t}}{{v.id}} {{'uiText.exhibition.5'|t}}{{exhibitions()[v.id]}}</option>}</select></label>
<label>{{'uiText.qr.token.or.url'|t}} <span class="required-mark" aria-hidden="true">*</span><input name="token" [(ngModel)]="token" required autocomplete="off"></label>
<button class="button" [disabled]="busy()||!token.trim()">{{'visitor.continueScan'|t}}</button></form>}
@else if(!busy()&&!error()){<p class="message">{{'uiText.you.have.no.active.visit.a.cashier.must.first.validate.your.ticket'|t}}</p><a routerLink="/tickets">{{'ui.tickets'|t}}</a>}
`})
export class ScanPage extends PageState {
  private api=inject(Api); private router=inject(Router);
  private route=inject(ActivatedRoute);
  token=this.route.snapshot.paramMap.get('token')||this.route.snapshot.queryParamMap.get('token')||'';
  visits=signal<Visit[]>([]);exhibitions=signal<Record<number,number>>({});selected:number|null=null;
  constructor(){super();void this.run(async()=>{
    const visits=(await this.api.get<Visit[]>('/visits/me')).filter(v=>v.status==='AKTIVNA');
    const tickets=await this.api.get<Ticket[]>('/tickets/me');
    const current=visits.filter(v=>tickets.some(t=>t.id===v.ticketId && Date.parse(t.validUntil)>Date.now()));
    this.visits.set(current);this.selected=current[0]?.id||null;
    this.exhibitions.set(Object.fromEntries(current.map(v=>[v.id,tickets.find(t=>t.id===v.ticketId)!.exhibitionId])));
    if(this.token && current.length===1) await this.router.navigate(['/visits',current[0].id],{queryParams:{token:this.token},replaceUrl:true});
  });}
  open(){try {const token=scanToken(this.token);if(this.selected)void this.router.navigate(['/visits',this.selected],{queryParams:{token},replaceUrl:true});}catch(e){this.error.set((e as Error).message);}}
}
