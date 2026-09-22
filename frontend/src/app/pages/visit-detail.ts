import { Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule, NgForm } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Api, PageState } from '../core/api';
import { Criterion, Participation, Scan, Visit, VisitRoute, Ticket } from '../core/models';
import { Feedback } from '../shared/feedback';
import { StatusPipe } from '../shared/status';
import { DemoQr, LocalDemo, scanToken } from '../core/scan';
@Component({imports:[DatePipe,FormsModule,RouterLink,Feedback,StatusPipe],template:`
<a class="back" routerLink="/visits">← Moje posete</a><section class="page-heading"><p class="eyebrow">ISTRAŽIVANJE U TOKU</p><h1>Poseta #{{id}}</h1>@if(visit();as v){<p>{{v.enteredAt|date:'dd.MM.yyyy. HH:mm'}} · <span class="badge">{{v.status|status}}</span></p>}</section>
<app-feedback [busy]="busy()" [error]="error()" [notice]="notice()" /><button class="text-button refresh" [disabled]="busy()" (click)="load()">Osveži posetu ↻</button>
@if(visit()?.status==='AKTIVNA'){
<section class="panel"><h2>Skeniraj eksponat</h2><p>Kamerom telefona skenirajte QR oznaku postavke i otvorite link, ili unesite njen token ili URL u formu.</p><a routerLink="/scan">Otvori izbor aktivne posete</a>
@if(demoEntries().length){<details><summary>Demo skeniranje · samo lokalno</summary><p>Eksponati ove izložbe:</p>@for(d of demoEntries();track d.participationId){<button class="button secondary small" [disabled]="busy()" (click)="performScan(d.token)">{{d.name}}</button>}</details>}</section>}
@if(lastScan();as s){<section class="panel" aria-live="polite"><p class="eyebrow">SKENIRANI EKSPONAT</p><h2>{{s.participation.name}}</h2><p>{{s.participation.contextDescription}}</p>
@for(c of s.participation.content;track c.id){<p>{{c.type}} · {{c.language}} · verzija {{c.version}}
@if(contentLink(c.path);as href){<a [href]="href" target="_blank" rel="noopener noreferrer">Otvori sadržaj</a>}</p>}
@for(n of s.next;track n.participationId){<h3>Sledeći eksponat: {{n.name}}</h3><p>{{n.explanation}}</p>}@empty{<p>Obišli ste sve trenutno dostupne eksponate.</p>}
<button class="button" [disabled]="busy()||!s.next.length||visit()?.status!=='AKTIVNA'" (click)="generateCombined()">Generiši rutu</button></section>}
@if(visit();as v){<div class="detail-layout"><section><div class="panel"><h2>Skenirani eksponati</h2>@for(s of scans();track s.id){<div class="scan-row"><span class="step">{{ $index+1 }}</span><div><strong>{{names()[s.participationId]||'Eksponat · učešće #'+s.participationId}}</strong><small>{{s.scannedAt|date:'dd.MM.yyyy. HH:mm'}} @if(s.durationSeconds!==null){ · {{s.durationSeconds}} s}</small></div></div>}@empty{<p class="empty-inline">Još nema skeniranih eksponata za ovu posetu.</p>}</div>
<div class="panel"><h2>Preporučene rute</h2>@for(r of routes();track r.id){<div class="route-block"><div class="card-meta"><strong>{{r.criterion|status}}</strong><span class="badge">{{r.status|status}}</span></div><p class="muted small-text">{{r.generatedAt|date:'dd.MM.yyyy. HH:mm'}}</p><ol class="route-list">@for(item of r.items;track item.position){<li><span class="step" [class.done]="item.visited">{{item.visited?'✓':item.position}}</span><div><strong>{{names()[item.participationId]||'Eksponat · učešće #'+item.participationId}}</strong><p>{{item.explanation}}</p><small>{{item.visited?'Posećen':'Za istraživanje'}}</small></div></li>}</ol></div>}@empty{<p class="empty-inline">Preporučena ruta još nije kreirana.</p>}</div></section>
<aside>@if(v.status==='AKTIVNA'){<div class="panel"><p class="eyebrow">OTKRIJTE SLEDEĆI EKSPONAT</p><h2>Unesite QR token</h2><form #scanForm="ngForm" (ngSubmit)="scan(scanForm)"><label>Token eksponata<input name="qr" [(ngModel)]="qr" required placeholder="Token sa oznake eksponata" autocomplete="off"></label><p class="muted small-text">Unesite tekst QR tokena eksponata. Token ulaznice se koristi samo na ulazu.</p><button class="button full" [disabled]="busy()||scanForm.invalid">Evidentiraj skeniranje</button></form></div><div class="panel"><h2>Vaša ruta</h2><label>Kriterijum<select [(ngModel)]="criterion"><option value="KOMBINACIJA">Kombinovana preporuka</option><option value="INTERESOVANJA">Interesovanja</option><option value="BLIZINA">Blizina</option><option value="POPULARNOST">Popularnost</option></select></label><button class="button secondary full" (click)="generate()" [disabled]="busy()">Kreiraj preporučenu rutu</button></div>}
@else{<div class="panel"><h2>Poseta je završena</h2><p>Sačuvani su vaši skenirani eksponati i istorija ruta.</p><a routerLink="/recommendations">Pronađite sledeću izložbu ↗</a></div>}</aside></div>}
`})
export class VisitDetailPage extends PageState {
  private demo=inject(LocalDemo);private route=inject(ActivatedRoute);
  demoEntries=signal<DemoQr[]>([]);lastScan=signal<Scan|null>(null);
  private api=inject(Api);id=Number(inject(ActivatedRoute).snapshot.paramMap.get('id'));
  visit=signal<Visit|null>(null);scans=signal<Scan[]>([]);routes=signal<VisitRoute[]>([]);names=signal<Record<number,string>>({});qr='';criterion:Criterion='KOMBINACIJA';
  constructor(){super();this.qr=this.route.snapshot.queryParamMap.get('token')||'';this.load();}
  private async refresh(){const [v,s,r]=await Promise.all([this.api.get<Visit>('/visits/'+this.id),this.api.get<Scan[]>(`/visits/${this.id}/scans`),this.api.get<VisitRoute[]>(`/visits/${this.id}/routes`)]);this.visit.set(v);this.scans.set(s);this.routes.set(r);
    if(v.status==='AKTIVNA'){const ids=[...new Set([...s.map(x=>x.participationId),...r.flatMap(x=>x.items.map(i=>i.participationId))])];const results=await Promise.allSettled(ids.map(id=>this.api.get<Participation>(`/visits/${this.id}/content/${id}`)));const names:Record<number,string>={};for(const result of results)if(result.status==='fulfilled')names[result.value.participationId]=result.value.name;this.names.set(names);}
  }
  load(){this.demoEntries.set([]);void this.run(async()=>{await this.refresh();if(this.visit()?.status==='AKTIVNA' && await this.demo.enabled()){
    const [ticket,entries]=await Promise.all([this.api.get<Ticket>('/tickets/'+this.visit()!.ticketId),this.api.get<DemoQr[]>(`/local-demo/visits/${this.id}`)]);
    this.demoEntries.set(entries.filter(entry=>entry.exhibitionId===ticket.exhibitionId));
  }});}
  scan(form:NgForm){if(form.invalid||!this.qr.trim())return;this.performScan(this.qr);}
  performScan(value:string){let token:string;try{token=scanToken(value);}catch(e){this.error.set((e as Error).message);return;}
    void this.run(async()=>{this.lastScan.set(null);const result=await this.api.post<Scan>(`/visits/${this.id}/scans`,{qrToken:token});this.lastScan.set(result);this.qr='';await this.refresh();this.notice.set('Skeniranje je sačuvano.');});}
  generateCombined(){this.criterion='KOMBINACIJA';this.generate();}
  contentLink(path:string):string|null {try {const url=new URL(path,window.location.origin+'/');return ['https:','http:'].includes(url.protocol)?url.href:null;}catch{return null;}}
  generate(){void this.run(async()=>{await this.api.post<VisitRoute>(`/visits/${this.id}/routes`,{criterion:this.criterion});await this.refresh();this.notice.set('Preporučena ruta je kreirana.');});}
}
