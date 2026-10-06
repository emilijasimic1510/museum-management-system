import { Component, computed, inject, signal, ViewEncapsulation } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Api } from '../core/api';
import { Museum, Hall, Exhibition } from '../core/models';
import { ManagedParticipation } from '../core/curator';
import { CatalogExhibit } from '../core/catalog';
import { hasPublicBaseUrl } from '../core/runtime-config';
import { TranslatePipe } from '../core/i18n';
import { ParticipationQr } from '../shared/participation-qr';
type Card=ManagedParticipation & {name:string; exhibitionName:string};
@Component({selector:'app-qr-board',imports:[FormsModule,TranslatePipe,ParticipationQr],
  encapsulation:ViewEncapsulation.None,styleUrl:'./qr-board.css',template:`
<section class="qr-board-controls">
  <h1>{{'qrBoard.title'|t}}</h1><p>{{'qrBoard.intro'|t}}</p>
  @if(!configured){<p class="message" role="alert">{{'qrBoard.missingBase'|t}}</p>}
  @if(error()){<p role="alert">{{'qrBoard.error'|t}}</p><button (click)="load()">{{'qrBoard.retry'|t}}</button>}
  <div class="qr-board-filters">
    <label>{{'qrBoard.museum'|t}}<select [ngModel]="museumId" (ngModelChange)="changeMuseum($event)" [disabled]="busy()">
      <option [ngValue]="null">{{'qrBoard.chooseMuseum'|t}}</option>@for(m of museums();track m.id){<option [ngValue]="m.id">{{m.name}}</option>}
    </select></label>
    <label>{{'qrBoard.exhibition'|t}}<select [ngModel]="exhibitionId" (ngModelChange)="changeExhibition($event)" [disabled]="busy()||!museumId">
      <option [ngValue]="null">{{'qrBoard.chooseExhibition'|t}}</option>@for(e of exhibitionsForMuseum();track e.id){<option [ngValue]="e.id">{{e.name}}</option>}
    </select></label>
  </div>
  @if(busy()){<p role="status">{{'qrBoard.loading'|t}}</p>}
  @if(exhibitionId&&!busy()&&!error()&&!cards().length){<p>{{'qrBoard.empty'|t}}</p>}
  <div class="qr-board-actions"><button (click)="selectAll()" [disabled]="busy()||!cards().length">{{'qrBoard.all'|t}}</button><button (click)="clear()" [disabled]="!selected().size">{{'qrBoard.clear'|t}}</button><span role="status">{{'qrBoard.count'|t}}: {{selected().size}}</span></div>
  <div class="qr-board-selection">@for(c of cards();track c.id){<label><input type="checkbox" [checked]="selected().has(c.id)" (change)="toggle(c.id)">{{c.name}} @if(c.tourOrder!==null){ · {{'qrBoard.order'|t}} {{c.tourOrder}}}</label>}</div>
  <div class="qr-board-actions"><button class="button" (click)="preview()" [disabled]="busy()||!selected().size">{{'qrBoard.preview'|t}}</button><button class="button secondary" (click)="print()" [disabled]="!printReady()">{{'qrBoard.print'|t}}</button></div>
  @if(pages().length&&!printReady()){<p role="status">{{'qrBoard.preparing'|t}}</p>}
  <p>{{'qrBoard.printHint'|t}}</p>
</section>
<section class="qr-board-preview" [class.qr-board-pending]="!printReady()">
@for(page of pages();track page){<div class="qr-board-sheet">
  <p class="qr-board-page-label">{{'qrBoard.page'|t}} {{$index+1}}</p>
  <div class="qr-board-grid">@for(c of page;track c.id){<article class="qr-board-card">
    <p class="qr-board-exhibition">{{c.exhibitionName}}</p><h2>{{c.name}}</h2>
    <app-participation-qr [token]="c.qrToken" [label]="'qrBoard.scan'|t" [failureText]="'qrBoard.qrError'|t" (ready)="qrReady(c.id,$event)" />
    @if(c.tourOrder!==null){<p>{{'qrBoard.order'|t}} {{c.tourOrder}}</p>}
    <p>{{'qrBoard.scan'|t}}</p>
  </article>}</div>
</div>}
</section>`})
export class QrBoard {
  private api=inject(Api);configured=hasPublicBaseUrl();
  museums=signal<Museum[]>([]);private halls:Hall[]=[];private exhibitions:Exhibition[]=[];private exhibits:CatalogExhibit[]=[];
  museumId:number|null=null;exhibitionId:number|null=null;busy=signal(false);error=signal(false);
  cards=signal<Card[]>([]);selected=signal(new Set<number>());pages=signal<Card[][]>([]);private ready=signal(new Set<number>());
  printReady=computed(()=>this.pages().flat().length>0&&this.pages().flat().every(c=>this.ready().has(c.id)));
  constructor(){void this.load();}
  exhibitionsForMuseum(){return this.exhibitions.filter(e=>this.halls.some(h=>h.id===e.hallId&&h.museumId===this.museumId));}
  async load(){
    this.busy.set(true);this.error.set(false);this.changeMuseum(null);
    try {
      const [m,h,e,x]=await Promise.all([this.api.get<Museum[]>('/museums'),this.api.get<Hall[]>('/halls'),this.api.get<Exhibition[]>('/exhibitions'),this.api.get<CatalogExhibit[]>('/exhibits')]);
      this.museums.set(m);this.halls=h;this.exhibitions=e;this.exhibits=x;
    } catch {this.error.set(true);} finally {this.busy.set(false);}
  }
  changeMuseum(id:number|null){this.museumId=id;this.exhibitionId=null;this.cards.set([]);this.clear();}
  async changeExhibition(id:number|null){
    this.exhibitionId=id;this.cards.set([]);this.clear();this.error.set(false);
    if(id===null)return;
    this.busy.set(true);
    try {
      const parts=await this.api.get<ManagedParticipation[]>('/exhibit-participations?exhibitionId='+id);
      if(this.exhibitionId!==id)return;
      const now=Date.now(),exhibition=this.exhibitionsForMuseum().find(e=>e.id===id);
      if(!exhibition)return;
      this.cards.set(parts.filter(p=>p.exhibitionId===id&&(!p.installedAt||Date.parse(p.installedAt)<=now)&&(!p.removedAt||Date.parse(p.removedAt)>now))
        .map(p=>({...p,name:this.exhibits.find(e=>e.id===p.exhibitId)?.name||'',exhibitionName:exhibition.name}))
        .sort((a,b)=>(a.tourOrder??Infinity)-(b.tourOrder??Infinity)||a.name.localeCompare(b.name)));
    } catch {this.error.set(true);} finally {this.busy.set(false);}
  }
  private invalidate(){this.pages.set([]);this.ready.set(new Set());}
  clear(){this.selected.set(new Set());this.invalidate();}
  selectAll(){this.selected.set(new Set(this.cards().map(c=>c.id)));this.invalidate();}
  toggle(id:number){const next=new Set(this.selected());next.has(id)?next.delete(id):next.add(id);this.selected.set(next);this.invalidate();}
  preview(){this.ready.set(new Set());const selected=this.cards().filter(c=>this.selected().has(c.id));const pages:Card[][]=[];for(let i=0;i<selected.length;i+=4)pages.push(selected.slice(i,i+4));this.pages.set(pages);}
  qrReady(id:number,value:boolean){this.ready.update(previous=>{const next=new Set(previous);value?next.add(id):next.delete(id);return next;});}
  print(){if(this.printReady())window.print();}
}
