import { Component, inject, signal } from '@angular/core';
import { Api, PageState } from '../core/api';
import { DemoQr } from '../core/scan';
import { Feedback } from '../shared/feedback';
import { toDataURL } from 'qrcode';
@Component({imports:[Feedback],template:`
<section class="page-heading"><p class="eyebrow">SAMO LOKALNA DEMONSTRACIJA</p><h1>Demo QR tabla</h1><p>Skenirajte oznaku izložbe za koju posetilac ima aktivnu posetu. Svaka postavka ima svoj QR.</p><p class="muted">Telefon mora moći da otvori adresu aplikacije. Adresa localhost radi samo na uređaju na kojem je aplikacija pokrenuta.</p></section>
<app-feedback [busy]="busy()" [error]="error()" />
<div class="cards">@for(q of codes();track q.token){<article class="panel"><p class="eyebrow">{{q.exhibitionName}} · #{{q.exhibitionId}}</p><h2>{{q.name}}</h2><img [src]="q.image" [alt]="'QR za '+q.name" width="240" height="240" style="max-width:100%;height:auto"><p><code>{{q.token}}</code></p><a [href]="q.url">Otvori skeniranje</a></article>}</div>`})
export class DemoQrBoard extends PageState {
  private api=inject(Api);codes=signal<(DemoQr & {image:string;url:string})[]>([]);
  constructor(){super();void this.run(async()=>{
    const entries=await this.api.get<DemoQr[]>('/local-demo/board');
    this.codes.set(await Promise.all(entries.map(async q=>{
      const url=new URL('/scan',window.location.origin);url.searchParams.set('token',q.token);
      return {...q,url:url.href,image:await toDataURL(url.href,{width:300,margin:4,errorCorrectionLevel:'M'})};
    })));
  });}
}
