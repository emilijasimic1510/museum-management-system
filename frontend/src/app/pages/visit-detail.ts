import { Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule, NgForm } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Api, PageState } from '../core/api';
import { Criterion, Participation, Scan, Visit, VisitRoute } from '../core/models';
import { Feedback } from '../shared/feedback';
import { StatusPipe } from '../shared/status';
@Component({imports:[DatePipe,FormsModule,RouterLink,Feedback,StatusPipe],template:`
<a class="back" routerLink="/visits">← Moje posete</a><section class="page-heading"><p class="eyebrow">ISTRAŽIVANJE U TOKU</p><h1>Poseta #{{id}}</h1>@if(visit();as v){<p>{{v.enteredAt|date:'dd.MM.yyyy. HH:mm'}} · <span class="badge">{{v.status|status}}</span></p>}</section>
<app-feedback [busy]="busy()" [error]="error()" [notice]="notice()" /><button class="text-button refresh" [disabled]="busy()" (click)="load()">Osveži posetu ↻</button>
@if(visit();as v){<div class="detail-layout"><section><div class="panel"><h2>Skenirani eksponati</h2>@for(s of scans();track s.id){<div class="scan-row"><span class="step">{{ $index+1 }}</span><div><strong>{{names()[s.participationId]||'Eksponat · učešće #'+s.participationId}}</strong><small>{{s.scannedAt|date:'dd.MM.yyyy. HH:mm'}} @if(s.durationSeconds!==null){ · {{s.durationSeconds}} s}</small></div></div>}@empty{<p class="empty-inline">Još nema skeniranih eksponata za ovu posetu.</p>}</div>
<div class="panel"><h2>Preporučene rute</h2>@for(r of routes();track r.id){<div class="route-block"><div class="card-meta"><strong>{{r.criterion|status}}</strong><span class="badge">{{r.status|status}}</span></div><p class="muted small-text">{{r.generatedAt|date:'dd.MM.yyyy. HH:mm'}}</p><ol class="route-list">@for(item of r.items;track item.position){<li><span class="step" [class.done]="item.visited">{{item.visited?'✓':item.position}}</span><div><strong>{{names()[item.participationId]||'Eksponat · učešće #'+item.participationId}}</strong><p>{{item.explanation}}</p><small>{{item.visited?'Posećen':'Za istraživanje'}}</small></div></li>}</ol></div>}@empty{<p class="empty-inline">Preporučena ruta još nije kreirana.</p>}</div></section>
<aside>@if(v.status==='AKTIVNA'){<div class="panel"><p class="eyebrow">OTKRIJTE SLEDEĆI EKSPONAT</p><h2>Unesite QR token</h2><form #scanForm="ngForm" (ngSubmit)="scan(scanForm)"><label>Token eksponata<input name="qr" [(ngModel)]="qr" required placeholder="Token sa oznake eksponata" autocomplete="off"></label><p class="muted small-text">Unesite tekst QR tokena eksponata. Token ulaznice se koristi samo na ulazu.</p><button class="button full" [disabled]="busy()||scanForm.invalid">Evidentiraj skeniranje</button></form></div><div class="panel"><h2>Vaša ruta</h2><label>Kriterijum<select [(ngModel)]="criterion"><option value="KOMBINACIJA">Kombinovana preporuka</option><option value="INTERESOVANJA">Interesovanja</option><option value="BLIZINA">Blizina</option><option value="POPULARNOST">Popularnost</option></select></label><button class="button secondary full" (click)="generate()" [disabled]="busy()">Kreiraj preporučenu rutu</button></div>}
@else{<div class="panel"><h2>Poseta je završena</h2><p>Sačuvani su vaši skenirani eksponati i istorija ruta.</p><a routerLink="/recommendations">Pronađite sledeću izložbu ↗</a></div>}</aside></div>}
`})
export class VisitDetailPage extends PageState {
  private api=inject(Api);id=Number(inject(ActivatedRoute).snapshot.paramMap.get('id'));
  visit=signal<Visit|null>(null);scans=signal<Scan[]>([]);routes=signal<VisitRoute[]>([]);names=signal<Record<number,string>>({});qr='';criterion:Criterion='KOMBINACIJA';
  constructor(){super();this.load();}
  private async refresh(){const [v,s,r]=await Promise.all([this.api.get<Visit>('/visits/'+this.id),this.api.get<Scan[]>(`/visits/${this.id}/scans`),this.api.get<VisitRoute[]>(`/visits/${this.id}/routes`)]);this.visit.set(v);this.scans.set(s);this.routes.set(r);
    if(v.status==='AKTIVNA'){const ids=[...new Set([...s.map(x=>x.participationId),...r.flatMap(x=>x.items.map(i=>i.participationId))])];const results=await Promise.allSettled(ids.map(id=>this.api.get<Participation>(`/visits/${this.id}/content/${id}`)));const names:Record<number,string>={};for(const result of results)if(result.status==='fulfilled')names[result.value.participationId]=result.value.name;this.names.set(names);}
  }
  load(){void this.run(()=>this.refresh());}
  scan(form:NgForm){if(form.invalid||!this.qr.trim())return;void this.run(async()=>{await this.api.post<Scan>(`/visits/${this.id}/scans`,{qrToken:this.qr.trim()});this.qr='';form.resetForm();await this.refresh();this.notice.set('Skeniranje je sačuvano.');});}
  generate(){void this.run(async()=>{await this.api.post<VisitRoute>(`/visits/${this.id}/routes`,{criterion:this.criterion});await this.refresh();this.notice.set('Preporučena ruta je kreirana.');});}
}
