import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Api, PageState } from '../core/api';
import { Visit, Ticket } from '../core/models';
import { Feedback } from '../shared/feedback';
import { LocalizedContentPipe, TranslatePipe } from '../core/i18n';

@Component({imports:[RouterLink,Feedback,TranslatePipe,LocalizedContentPipe],template:`
<section class="page-heading"><h1>{{'visitor.scanExhibit'|t}}</h1></section>
<app-feedback [busy]="busy()" [error]="error()" />
@if(visits().length>1){<section class="panel"><p>{{'uiText.choose.your.active.visit.to.the.exhibition.you.are.in'|t}}</p>
@for(v of visits();track v.id){<button class="button" (click)="open(v.id)">{{v|localized:'exhibitionName'}}</button>}</section>}
@else if(!visits().length&&!busy()&&!error()){<p class="message">{{'uiText.you.have.no.active.visit.a.cashier.must.first.validate.your.ticket'|t}}</p><a routerLink="/tickets">{{'ui.tickets'|t}}</a>}
`})
export class ScanPage extends PageState {
  private api=inject(Api);private route=inject(ActivatedRoute);private router=inject(Router);
  token=this.route.snapshot.paramMap.get('token')||this.route.snapshot.queryParamMap.get('token')||'';
  visits=signal<Visit[]>([]);
  constructor(){super();void this.run(async()=>{
    const [visits,tickets]=await Promise.all([this.api.get<Visit[]>('/visits/me'),this.api.get<Ticket[]>('/tickets/me')]);
    const current=visits.filter(v=>v.status==='AKTIVNA'&&tickets.some(t=>t.id===v.ticketId&&Date.parse(t.validUntil)>Date.now()));
    this.visits.set(current);if(current.length===1)await this.open(current[0].id);
  });}
  open(id:number){return this.router.navigate(['/visits',id],{queryParams:this.token?{token:this.token}:{},replaceUrl:true});}
}
