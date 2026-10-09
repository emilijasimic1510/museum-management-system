import { QrCamera } from '../shared/qr-camera';
import { LocalizedContentPipe } from '../core/i18n';
import {UiLabelPipe} from '../core/i18n';
import { MediaImage } from '../shared/media-image';
import { AudioGuide } from '../shared/audio-guide';
import { VisitorText } from '../shared/visitor-text';
import { Component, inject, signal } from '@angular/core';
import { LocalDatePipe as DatePipe } from '../core/i18n';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Api, PageState } from '../core/api';
import { Criterion, Participation, Scan, Visit, VisitRoute } from '../core/models';
import { Feedback } from '../shared/feedback';
import { I18n, TranslatePipe } from '../core/i18n';
import { StatusPipe } from '../shared/status';
import { scanToken } from '../core/scan';
import { VisitRatingPanel } from '../shared/visit-rating';
@Component({imports:[QrCamera,LocalizedContentPipe,UiLabelPipe,MediaImage,DatePipe,FormsModule,RouterLink,Feedback,StatusPipe,VisitRatingPanel,AudioGuide,VisitorText,TranslatePipe],template:`
<a class="back" routerLink="/visits">{{'uiText.my.visits'|t}}</a><section class="page-heading tour-heading"><p class="eyebrow">{{'nav.visits'|t}}</p><h1>{{'uiText.visit'|t}}{{id}}</h1>@if(visit();as v){<h2>{{(v|localized:'exhibitionName')}}</h2><p>{{'uiText.entry'|t}} {{v.enteredAt|date:'dd.MM.yyyy. HH:mm'}} · <span class="badge">{{v.status|status}}</span></p>@if(v.exitedAt){<p>{{'uiText.exit'|t}} {{v.exitedAt|date:'dd.MM.yyyy. HH:mm'}}</p>}}</section>
<app-feedback [busy]="busy()" [error]="error()" [notice]="notice()" /><button class="text-button refresh" [disabled]="busy()" (click)="load()">{{error()?('ui.retry'|t):('uiText.refresh.visit'|t)}}</button>
@if(visit()?.status==='AKTIVNA'){
<section class="panel"><h2>{{'visitor.finishVisit'|t}}</h2><p>{{'visitor.finishDescription'|t}}</p>
@if(confirmFinish()){<p role="status">{{'visitor.confirmFinish'|t}}</p><div class="visit-actions"><button class="button" [disabled]="busy()" (click)="finish()">{{'visitor.confirm'|t}}</button><button class="button secondary" [disabled]="busy()" (click)="confirmFinish.set(false)">{{'visitor.continue'|t}}</button></div>}
@else{<button class="button secondary" [disabled]="busy()" (click)="confirmFinish.set(true)">{{'visitor.finish'|t}}</button>}</section>
<section class="panel"><h2>{{'visitor.scanExhibitTitle'|t}}</h2><p>{{'uiText.point.camera.at.exhibit.qr'|t}}</p>@if(!busy()){<app-qr-camera (decoded)="performScan($event)" />}</section>}
@if(lastScan();as s){<section class="panel scanned-exhibit" aria-live="polite"><p class="eyebrow">{{'uiText.scanned.exhibit'|t}}</p><h2>{{(s.participation|localized:'name')}}</h2><app-visitor-text [visitId]="id" [participation]="s.participation" />
@if(visit()?.status==='AKTIVNA'){<app-audio-guide [visitId]="id" [participation]="s.participation" />}
@for(c of s.participation.content;track c.id){@if(c.type==='SLIKA'&&!c.path&&visit()?.status==='AKTIVNA'){<app-media-image [path]="'/visits/'+id+'/content/'+s.participation.participationId+'/images/'+c.id" [protected]="true" [alt]="s.participation|localized:'name'" [contain]="true" />}<p>{{c.type|uiLabel:'content'}} · {{c.language|uiLabel:'language'}} {{'uiText.version'|t}} {{c.version}}
@if(contentLink(c.path);as href){<a [href]="href" target="_blank" rel="noopener noreferrer">{{'uiText.open.content'|t}}</a>}</p>}
@for(n of s.next;track n.participationId){<h3>{{'uiText.next.exhibit'|t}} {{(n|localized:'name')}}</h3><p>{{n.explanation}}</p>}@empty{<p>{{'uiText.you.have.visited.all.currently.available.exhibits'|t}}</p>}
<button class="button" [disabled]="busy()||!s.next.length||visit()?.status!=='AKTIVNA'" (click)="generateCombined()">{{'uiText.generate.route'|t}}</button></section>}
@if(visit();as v){<div class="detail-layout"><section><div class="panel"><h2>{{'visitor.scanned'|t}}</h2>@for(s of scans();track s.id){<div class="scan-row"><span class="step">{{ $index+1 }}</span><div><strong>{{names()[s.participationId]||('uiText.exhibit.installation'|t)+s.participationId}}</strong><small>{{s.scannedAt|date:'dd.MM.yyyy. HH:mm'}} @if(s.durationSeconds!==null){ · {{s.durationSeconds}} s}</small></div></div>}@empty{<p class="empty-inline">{{'visitor.noScans'|t}}</p>}</div>
<div class="panel"><h2>{{'visitor.routes'|t}}</h2>@for(r of routes();track r.id){<div class="route-block"><div class="card-meta"><strong>{{r.criterion|status}}</strong><span class="badge">{{r.status|status}}</span></div><p class="muted small-text">{{r.generatedAt|date:'dd.MM.yyyy. HH:mm'}}</p><ol class="route-list">@for(item of r.items;track item.position){<li><span class="step" [class.done]="item.visited">{{item.visited?'✓':item.position}}</span><div><strong>{{names()[item.participationId]||('uiText.exhibit.installation'|t)+item.participationId}}</strong><p>{{item.explanation}}</p><small>{{item.visited?('uiText.visited'|t):('uiText.to.explore'|t)}}</small></div></li>}</ol></div>}@empty{<p class="empty-inline">{{'visitor.noRoutes'|t}}</p>}</div></section>
<aside>@if(v.status==='AKTIVNA'){<div class="panel"><h2>{{'uiText.your.route'|t}}</h2><label>{{'uiText.criterion'|t}}<select [(ngModel)]="criterion"><option value="KOMBINACIJA">{{'uiText.combined.recommendation'|t}}</option><option value="INTERESOVANJA">{{'status.INTERESOVANJA'|t}}</option><option value="BLIZINA">{{'status.BLIZINA'|t}}</option><option value="POPULARNOST">{{'status.POPULARNOST'|t}}</option></select></label><button class="button secondary full" (click)="generate()" [disabled]="busy()">{{'uiText.create.recommended.route'|t}}</button></div>}
@else if(v.status==='ZAVRSENA'){<div class="panel"><h2>{{'uiText.visit.finished'|t}}</h2><p>{{'uiText.your.scanned.exhibits.and.route.history.have.been.saved'|t}}</p><app-visit-rating [visitId]="v.id" /><a routerLink="/recommendations">{{'uiText.find.your.next.exhibition'|t}}</a></div>}</aside></div>}
`})
export class VisitDetailPage extends PageState {
  private route=inject(ActivatedRoute);
  lastScan=signal<Scan|null>(null);
  confirmFinish=signal(false);
  private api=inject(Api);private i18n=inject(I18n);id=Number(inject(ActivatedRoute).snapshot.paramMap.get('id'));
  visit=signal<Visit|null>(null);scans=signal<Scan[]>([]);routes=signal<VisitRoute[]>([]);names=signal<Record<number,string>>({});qr='';criterion:Criterion='KOMBINACIJA';
  constructor(){super();this.qr=this.route.snapshot.queryParamMap.get('token')||'';this.load();}
  private async refresh(){const [v,s,r]=await Promise.all([this.api.get<Visit>('/visits/'+this.id,10000),this.api.get<Scan[]>(`/visits/${this.id}/scans`,10000),this.api.get<VisitRoute[]>(`/visits/${this.id}/routes`,10000)]);this.visit.set(v);this.scans.set(s);this.routes.set(r);
    this.names.set(Object.fromEntries(s.filter(scan=>scan.participation?.name).map(scan=>[scan.participationId,scan.participation.name])));
    if(v.status==='ZAVRSENA'){this.lastScan.set(null);this.confirmFinish.set(false);}
    if(v.status==='AKTIVNA'){const ids=[...new Set([...s.map(x=>x.participationId),...r.flatMap(x=>x.items.map(i=>i.participationId))])];const results=await Promise.allSettled(ids.map(id=>this.api.get<Participation>(`/visits/${this.id}/content/${id}`,10000)));const names:Record<number,string>={};for(const result of results)if(result.status==='fulfilled')names[result.value.participationId]=result.value.name;this.names.set(names);}
  }
  finish(){if(this.visit()?.status!=='AKTIVNA'||!this.confirmFinish()||this.busy())return;
    void this.run(async()=>{const closed=await this.api.post<Visit>(`/visits/${this.id}/finish`,{},10000);
      this.visit.set(closed);this.lastScan.set(null);this.confirmFinish.set(false);
      this.notice.set(this.i18n.t('visitor.ratingSaved'));await this.refresh();
    });
  }
  load(){void this.run(async()=>{await this.refresh();}).then(()=>{if(this.visit()?.status==='AKTIVNA'&&this.qr){const token=this.qr;this.qr='';this.performScan(token);}});}
  performScan(value:string){if(this.busy()||this.visit()?.status!=='AKTIVNA')return;let token:string;try{token=scanToken(value);}catch(e){this.error.set((e as Error).message);return;}
    void this.run(async()=>{this.lastScan.set(null);const result=await this.api.post<Scan>(`/visits/${this.id}/scans`,{qrToken:token},10000);this.lastScan.set(result);this.qr='';await this.refresh();this.notice.set(this.i18n.t('visitor.scanSaved'));});}
  generateCombined(){this.criterion='KOMBINACIJA';this.generate();}
  contentLink(path:string):string|null {try {if(!path)return null;const url=new URL(path,window.location.origin+'/');return ['https:','http:'].includes(url.protocol)?url.href:null;}catch{return null;}}
  generate(){void this.run(async()=>{await this.api.post<VisitRoute>(`/visits/${this.id}/routes`,{criterion:this.criterion},10000);await this.refresh();this.notice.set(this.i18n.t('visitor.routeCreated'));});}
}
