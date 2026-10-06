import { Component, effect, inject, input, signal } from '@angular/core';
import { Api } from '../core/api';
import { Participation } from '../core/models';
import { I18n, TranslatePipe } from '../core/i18n';
@Component({imports:[TranslatePipe],selector:'app-audio-guide',template:`
<section><h3>{{'visitor.audio'|t}}</h3>
@for(c of latest();track c.id){<p>{{c.language==='en'?('uiText.english'|t):('uiText.serbian'|t)}} · {{'visitor.textVersion'|t}} {{c.version}}</p>}
@if(busy()){<p class="loading" role="status">{{'visitor.audioLoading'|t}}</p>}
@if(error()){<p class="message error" role="alert">{{error()|t}}</p>}
@for(source of sources();track source.id){<audio controls [src]="source.url" [attr.aria-label]="('visitor.audio'|t)+' '+source.language"></audio>}
@if(!latest().length){<p>{{'visitor.audioUnavailable'|t}}</p>}
</section>`})
export class AudioGuide {
  visitId=input.required<number>();participation=input.required<Participation>();private api=inject(Api);
  sources=signal<{id:number;language:string|null;url:string}[]>([]);busy=signal(false);error=signal(''); readonly i18n=inject(I18n);
  latest(){const rows=this.participation().content.filter(c=>c.type==='AUDIO');const newest=(language:string)=>rows.filter(c=>c.language===language).sort((a,b)=>b.version-a.version||b.id-a.id)[0];const preferred=this.i18n.language()==='en'?(newest('en')||newest('sr')):newest('sr');return preferred?[preferred]:[];}
  constructor(){effect(onCleanup=>{
    const visit=this.visitId(),p=this.participation(),rows=this.latest();let disposed=false;const urls:string[]=[];
    this.sources.set([]);this.error.set('');this.busy.set(rows.length>0);
    onCleanup(()=>{disposed=true;urls.forEach(url=>URL.revokeObjectURL(url));});
    void Promise.all(rows.map(async c=>{try{
      const blob=await this.api.blob(`/visits/${visit}/content/${p.participationId}/audio/${c.id}`);
      if(disposed)return;const url=URL.createObjectURL(blob);urls.push(url);
      this.sources.update(s=>[...s,{id:c.id,language:c.language,url}]);
    }catch{if(!disposed)this.error.set(this.i18n.t('visitor.audioError'));}})).finally(()=>{if(!disposed)this.busy.set(false);});
  });}
}
