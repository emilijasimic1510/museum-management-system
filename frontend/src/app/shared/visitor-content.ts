import {UiLabelPipe} from '../core/i18n';
import { TranslatePipe } from './../core/i18n';
import { MediaImage } from './media-image';
import { AudioGuide } from './audio-guide';
import { Component, effect, inject, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Api } from '../core/api';
import { Auth } from '../core/auth';
import { CatalogResource } from '../core/catalog';
import { Participation } from '../core/models';
import { Feedback } from './feedback';
import { VisitorText } from './visitor-text';

export interface ContentAccess {
  participationId:number; exhibitionId:number; exhibitionName:string; visitId:number|null;
  state:'NO_ACTIVE_VISIT'|'SCAN_REQUIRED'|'UNLOCKED'; participation:Participation|null;
}
@Component({selector:'app-visitor-content',imports:[UiLabelPipe,TranslatePipe,MediaImage,RouterLink,Feedback,AudioGuide,VisitorText],template:`
<section class="panel"><h2>{{'uiText.additional.content'|t}}</h2>
<app-feedback [busy]="resource.busy()" [error]="resource.error()" />
@if(auth.token()){
  <button class="text-button" [disabled]="resource.busy()" (click)="load()">{{resource.error()?('ui.retry'|t):('uiText.refresh.scan.status'|t)}}</button>
  @for(row of resource.data();track row.participationId){
    <article class="route-block"><h3>{{row.exhibitionName}} {{'uiText.installation.4'|t}}{{row.participationId}}</h3>
    @if(row.state==='NO_ACTIVE_VISIT'){<p>{{'uiText.digital.content.is.available.during.a.visit'|t}}</p>}
    @else if(row.state==='SCAN_REQUIRED'){
      <p>{{'uiText.scan.the.installation.s.qr.code.to.unlock.content'|t}}</p>
      <a [routerLink]="['/visits',row.visitId]">{{'uiText.open.active.visit.and.scan.qr'|t}}</a>
    }@else if(row.participation;as p){
      <app-visitor-text [visitId]="row.visitId!" [participation]="p" />
      @if(row.visitId){<app-audio-guide [visitId]="row.visitId" [participation]="p" />}
      @for(c of p.content;track c.id){@if(c.type==='SLIKA'&&!c.path&&row.visitId){<app-media-image [path]="'/visits/'+row.visitId+'/content/'+p.participationId+'/images/'+c.id" [protected]="true" [alt]="p.name" [contain]="true" />}<p>{{c.type|uiLabel:'content'}} · {{c.language || ('uiText.language.not.specified'|t)}} {{'uiText.version'|t}} {{c.version}}
        @if(contentLink(c.path);as href){<a [href]="href" target="_blank" rel="noopener noreferrer">{{'uiText.open.content'|t}}</a>}
      </p>}@empty{<p>{{'uiText.this.scanned.installation.has.no.digital.content.yet'|t}}</p>}
    }</article>
  }@empty{@if(!resource.busy()&&!resource.error()){<p>{{'uiText.digital.content.is.available.during.a.visit'|t}}</p>}}
}@else{<p>{{'uiText.digital.content.is.available.during.a.visit'|t}}</p>}
</section>`})
export class VisitorContent {
  exhibitId=input.required<number>();readonly auth=inject(Auth);private api=inject(Api);
  readonly resource=new CatalogResource<ContentAccess[]>();
  constructor(){effect(()=>{this.exhibitId();this.auth.session();this.load();});}
  load(){const id=this.exhibitId(),token=this.auth.token();void this.resource.load(()=>token
    ?this.api.get<ContentAccess[]>(`/visits/me/exhibits/${id}/content`,10000):Promise.resolve([]));}
  contentLink(path:string):string|null {try{if(!path)return null;const url=new URL(path,window.location.origin+'/');return ['http:','https:'].includes(url.protocol)?url.href:null;}catch{return null;}}
}
