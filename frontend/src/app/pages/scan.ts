import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Api, PageState } from '../core/api';
import { Visit, Ticket } from '../core/models';
import { scanToken } from '../core/scan';
import { Feedback } from '../shared/feedback';

@Component({imports:[FormsModule,RouterLink,Feedback],template:`
<section class="page-heading"><h1>Skeniraj eksponat</h1><p>Izaberite aktivnu posetu izložbi u kojoj se nalazite.</p></section>
<app-feedback [busy]="busy()" [error]="error()" />
@if(visits().length){<form class="panel" (ngSubmit)="open()">
<label>Aktivna poseta<select name="visit" [(ngModel)]="selected" required>@for(v of visits();track v.id){<option [ngValue]="v.id">Poseta #{{v.id}} · izložba #{{exhibitions()[v.id]}}</option>}</select></label>
<label>QR token ili URL<input name="token" [(ngModel)]="token" required autocomplete="off"></label>
<button class="button" [disabled]="busy()||!token.trim()">Nastavi na skeniranje</button></form>}
@else if(!busy()&&!error()){<p class="message">Nemate aktivnu posetu. Blagajnik prvo mora validirati vašu ulaznicu.</p><a routerLink="/tickets">Moje ulaznice</a>}
`})
export class ScanPage extends PageState {
  private api=inject(Api); private router=inject(Router);
  token=inject(ActivatedRoute).snapshot.queryParamMap.get('token')||'';
  visits=signal<Visit[]>([]);exhibitions=signal<Record<number,number>>({});selected:number|null=null;
  constructor(){super();void this.run(async()=>{
    const visits=(await this.api.get<Visit[]>('/visits/me')).filter(v=>v.status==='AKTIVNA');
    const tickets=await this.api.get<Ticket[]>('/tickets/me');
    const current=visits.filter(v=>tickets.some(t=>t.id===v.ticketId && Date.parse(t.validUntil)>Date.now()));
    this.visits.set(current);this.selected=current[0]?.id||null;
    this.exhibitions.set(Object.fromEntries(current.map(v=>[v.id,tickets.find(t=>t.id===v.ticketId)!.exhibitionId])));
  });}
  open(){try {const token=scanToken(this.token);if(this.selected)void this.router.navigate(['/visits',this.selected],{queryParams:{token},replaceUrl:true});}catch(e){this.error.set((e as Error).message);}}
}
