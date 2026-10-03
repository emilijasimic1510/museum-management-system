import { AudioGuide } from './audio-guide';
import { Component, effect, inject, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Api } from '../core/api';
import { Auth } from '../core/auth';
import { CatalogResource } from '../core/catalog';
import { Participation } from '../core/models';
import { Feedback } from './feedback';

export interface ContentAccess {
  participationId:number; exhibitionId:number; exhibitionName:string; visitId:number|null;
  state:'NO_ACTIVE_VISIT'|'SCAN_REQUIRED'|'UNLOCKED'; participation:Participation|null;
}
@Component({selector:'app-visitor-content',imports:[RouterLink,Feedback,AudioGuide],template:`
<section class="panel"><h2>Dodatni sadržaj</h2>
<app-feedback [busy]="resource.busy()" [error]="resource.error()" />
@if(auth.token()){
  <button class="text-button" [disabled]="resource.busy()" (click)="load()">{{resource.error()?'Pokušaj ponovo':'Osveži status skeniranja'}}</button>
  @for(row of resource.data();track row.participationId){
    <article class="route-block"><h3>{{row.exhibitionName}} · postavka #{{row.participationId}}</h3>
    @if(row.state==='NO_ACTIVE_VISIT'){<p>Digitalni sadržaj dostupan tokom obilaska</p>}
    @else if(row.state==='SCAN_REQUIRED'){
      <p>Skenirajte QR oznaku postavke da otključate sadržaj</p>
      <a [routerLink]="['/visits',row.visitId]">Otvori aktivnu posetu i skeniraj QR</a>
    }@else if(row.participation;as p){
      <p>{{p.contextDescription || 'Kontekstualni tekst još nije dodat.'}}</p>
      @if(row.visitId){<app-audio-guide [visitId]="row.visitId" [participation]="p" />}
      @for(c of p.content;track c.id){<p>{{c.type}} · {{c.language || 'Jezik nije naveden'}} · verzija {{c.version}}
        @if(contentLink(c.path);as href){<a [href]="href" target="_blank" rel="noopener noreferrer">Otvori sadržaj</a>}
      </p>}@empty{<p>Za ovu skeniranu postavku još nema digitalnog sadržaja.</p>}
    }</article>
  }@empty{@if(!resource.busy()&&!resource.error()){<p>Digitalni sadržaj dostupan tokom obilaska</p>}}
}@else{<p>Digitalni sadržaj dostupan tokom obilaska</p>}
</section>`})
export class VisitorContent {
  exhibitId=input.required<number>();readonly auth=inject(Auth);private api=inject(Api);
  readonly resource=new CatalogResource<ContentAccess[]>();
  constructor(){effect(()=>{this.exhibitId();this.auth.session();this.load();});}
  load(){const id=this.exhibitId(),token=this.auth.token();void this.resource.load(()=>token
    ?this.api.get<ContentAccess[]>(`/visits/me/exhibits/${id}/content`,10000):Promise.resolve([]));}
  contentLink(path:string):string|null {try{if(!path)return null;const url=new URL(path,window.location.origin+'/');return ['http:','https:'].includes(url.protocol)?url.href:null;}catch{return null;}}
}
