import { Component, effect, inject, input, signal } from '@angular/core';
import { Api } from '../core/api';
import { Auth } from '../core/auth';
import { TranslatePipe } from '../core/i18n';

/** Public images use anonymous URLs. Private images use authenticated blob requests. */
@Component({selector:'app-media-image',imports:[TranslatePipe],template:`
<figure class="media-frame" [class.contain]="contain()">
@if(source()&&!failed()){<img [src]="source()" [alt]="alt()" loading="lazy" decoding="async" (error)="failed.set(true)" (load)="loadCredit()">}
@else{<div class="media-placeholder" role="img" [attr.aria-label]="alt()+' — '+('media.missing'|t)"><span aria-hidden="true">◇</span><small>{{'media.missing'|t}}</small></div>}
</figure>@if(credit();as c){<p class="small-text muted">{{'media.reference'|t}} <a [href]="c.source" target="_blank" rel="noopener noreferrer">{{c.title}}</a> · {{c.artist}} · The Metropolitan Museum of Art · {{c.license}}</p>}`,styles:[`:host{display:block;width:100%}.media-frame{margin:0;aspect-ratio:4/3;overflow:hidden;background:#e9e6de;border-radius:12px}.media-frame img{display:block;width:100%;height:100%;object-fit:cover}.media-frame.contain{aspect-ratio:auto;min-height:180px}.contain img{height:auto;max-height:65vh;object-fit:contain}.media-placeholder{min-height:180px;height:100%;display:flex;flex-direction:column;align-items:center;justify-content:center;color:#637065;gap:12px}.media-placeholder span{font-size:48px}`]})
export class MediaImage {
  path=input.required<string>();alt=input.required<string>();protected=input(false);contain=input(false);revision=input(0);
  credit=signal<{title:string;artist:string;source:string;license:string}|null>(null);
  loadCredit(){const path=this.path();void (this.protected()?this.api.get<any>(path+'?attribution=true'):this.api.publicGet<any>(path+'?attribution=true')).then(c=>{if(this.path()===path)this.credit.set(c.source?c:null);}).catch(()=>{});}
  source=signal('');failed=signal(false);private api=inject(Api);private auth=inject(Auth);
  constructor(){effect(cleanup=>{const path=this.path(),privateImage=this.protected(),revision=this.revision();if(privateImage)this.auth.session();
    this.source.set('');this.credit.set(null);this.failed.set(false);let disposed=false,url='';
    cleanup(()=>{disposed=true;if(url)URL.revokeObjectURL(url);});
    if(!path)return;
    if(!privateImage){this.source.set('/api'+path+'?v='+revision);return;}
    if(!this.auth.token())return;
    void this.api.blob(path).then(blob=>{if(!disposed){url=URL.createObjectURL(blob);this.source.set(url);}}).catch(()=>{if(!disposed)this.failed.set(true);});
  });}
}
