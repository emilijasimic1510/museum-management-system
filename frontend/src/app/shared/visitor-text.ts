import { Component, effect, inject, input, signal } from '@angular/core';
import { Api } from '../core/api';
import { I18n, TranslatePipe } from '../core/i18n';
import { Participation } from '../core/models';
@Component({selector:'app-visitor-text',imports:[TranslatePipe],template:`
@if(busy()){<p role="status">{{'visitor.textLoading'|t}}</p>}
@if(error()){<p class="message error" role="alert">{{'visitor.textError'|t}}</p>}
@if(!busy()&&!error()&&text()){<section><p>{{text()}}</p><small>{{language()==='en'?('uiText.english'|t):('uiText.serbian'|t)}} · {{'visitor.textVersion'|t}} {{version()}}</small></section>}
@if(!busy()&&!error()&&!text()){<p>{{'visitor.textUnavailable'|t}}</p>}`})
export class VisitorText {
 visitId=input.required<number>(); participation=input.required<Participation>(); private api=inject(Api); private i18n=inject(I18n);
 busy=signal(false);error=signal(false);text=signal('');language=signal<'sr'|'en'>('sr');version=signal(0);
 constructor(){effect(()=>{const visit=this.visitId(),p=this.participation(),lang=this.i18n.language();this.busy.set(true);this.error.set(false);this.text.set('');
  const rows=p.content.filter(c=>c.type==='TEKST');const pick=(l:string)=>rows.filter(c=>c.language===l).sort((a,b)=>b.version-a.version||b.id-a.id)[0];const chosen=lang==='en'?(pick('en')||pick('sr')):pick('sr');
  if(!chosen){this.busy.set(false);return;} this.language.set(chosen.language==='en'?'en':'sr');this.version.set(chosen.version);
  this.api.get<{text:string}>(`/visits/${visit}/content/${p.participationId}/text/${chosen.id}`).then(r=>this.text.set(r.text)).catch(()=>this.error.set(true)).finally(()=>this.busy.set(false));
 });}
}
