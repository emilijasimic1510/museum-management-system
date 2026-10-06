import { TranslatePipe } from './../core/i18n';
import { Component, inject, signal } from '@angular/core';
import { Api, PageState } from '../core/api';
import { DemoQr, scanUrl } from '../core/scan';
import { Feedback } from '../shared/feedback';
import { toDataURL } from 'qrcode';
@Component({imports:[TranslatePipe,Feedback],template:`
<section class="page-heading"><p class="eyebrow">{{'uiText.local.demonstration.only'|t}}</p><h1>{{'uiText.demo.qr.board'|t}}</h1><p>{{'uiText.scan.a.label.for.an.exhibition.where.the.visitor.has.an.active.visit.each.inst'|t}}</p><p class="muted">{{'uiText.the.phone.must.be.able.to.open.the.app.address.localhost.works.only.on.the.dev'|t}}</p></section>
<app-feedback [busy]="busy()" [error]="error()" />
<div class="cards">@for(q of codes();track q.token){<article class="panel"><p class="eyebrow">{{q.exhibitionName}} · #{{q.exhibitionId}}</p><h2>{{q.name}}</h2><img [src]="q.image" [alt]="('uiText.qr.for'|t)+' '+q.name" width="240" height="240" style="max-width:100%;height:auto"><p><code>{{q.token}}</code></p><a [href]="q.url">{{'uiText.open.scanning'|t}}</a></article>}</div>`})
export class DemoQrBoard extends PageState {
  private api=inject(Api);codes=signal<(DemoQr & {image:string;url:string})[]>([]);
  constructor(){super();void this.run(async()=>{
    const entries=await this.api.get<DemoQr[]>('/local-demo/board');
    this.codes.set(await Promise.all(entries.map(async q=>{
      const url=scanUrl(q.token);
      return {...q,url,image:await toDataURL(url,{width:300,margin:4,errorCorrectionLevel:'M'})};
    })));
  });}
}
