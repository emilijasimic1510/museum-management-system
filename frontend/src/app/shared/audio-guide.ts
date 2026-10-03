import { Component, effect, inject, input, signal } from '@angular/core';
import { Api } from '../core/api';
import { Participation } from '../core/models';
@Component({selector:'app-audio-guide',template:`
<section><h3>Audio vodič</h3>
@for(c of latest();track c.id){<p>{{c.language}} · aktuelna verzija {{c.version}}</p>}
@if(busy()){<p class="loading" role="status">Učitavanje audija…</p>}
@if(error()){<p class="message error" role="alert">{{error()}}</p>}
@for(source of sources();track source.id){<audio controls [src]="source.url" [attr.aria-label]="'Audio vodič '+source.language"></audio>}
@if(!latest().length){<p>Audio trenutno nije dostupan.</p>}
</section>`})
export class AudioGuide {
  visitId=input.required<number>();participation=input.required<Participation>();private api=inject(Api);
  sources=signal<{id:number;language:string|null;url:string}[]>([]);busy=signal(false);error=signal('');
  latest(){const rows=this.participation().content.filter(c=>c.type==='AUDIO');return rows.filter(c=>!rows.some(other=>other.language===c.language&&(other.version>c.version||(other.version===c.version&&other.id>c.id))));}
  constructor(){effect(onCleanup=>{
    const visit=this.visitId(),p=this.participation(),rows=this.latest();let disposed=false;const urls:string[]=[];
    this.sources.set([]);this.error.set('');this.busy.set(rows.length>0);
    onCleanup(()=>{disposed=true;urls.forEach(url=>URL.revokeObjectURL(url));});
    void Promise.all(rows.map(async c=>{try{
      const blob=await this.api.blob(`/visits/${visit}/content/${p.participationId}/audio/${c.id}`);
      if(disposed)return;const url=URL.createObjectURL(blob);urls.push(url);
      this.sources.update(s=>[...s,{id:c.id,language:c.language,url}]);
    }catch{if(!disposed)this.error.set('Audio trenutno nije dostupan. Tekst možete pročitati iznad.');}})).finally(()=>{if(!disposed)this.busy.set(false);});
  });}
}
