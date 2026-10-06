import {UiLabelPipe} from '../core/i18n';
import { translate } from './../core/i18n';
import { MediaImage } from '../shared/media-image';
import { ImageEditor } from '../shared/image-editor';
import { LocalDatePipe as DatePipe } from '../core/i18n';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Api } from '../core/api';
import { CatalogExhibit, validCatalogId } from '../core/catalog';
import { CuratorState, DigitalContent, ManagedParticipation, validReference } from '../core/curator';
import { Exhibition } from '../core/models';
import { Feedback } from '../shared/feedback';
import { TranslatePipe } from '../core/i18n';

@Component({imports:[UiLabelPipe,MediaImage,ImageEditor,FormsModule,RouterLink,Feedback,DatePipe,TranslatePipe],template:`
<section class="page-heading"><p class="eyebrow">{{'uiText.curator.workspace'|t}}</p><h1>{{mode==='new'?('uiText.add.digital.content'|t):mode==='edit'?('uiText.edit.digital.content'|t):('nav.digitalContent'|t)}}</h1><p>{{'uiText.content.belongs.to.a.specific.exhibit.installation.in.an.exhibition'|t}}</p>
@if(mode){<a routerLink="/curator/digital-content" [queryParams]="{participationId:participationId}">{{'uiText.back.to.content'|t}}</a>}@else{<a class="button" routerLink="/curator/digital-content/new" [queryParams]="{participationId:participationId}">{{'uiText.add.digital.content'|t}}</a>}</section>
<app-feedback [busy]="busy()" [error]="error()" [notice]="notice()" />
@if(error()){<button class="button secondary" [disabled]="busy()" (click)="retry()">{{'ui.retry'|t}}</button>}
@if(loaded()){<label>{{'uiText.installation'|t}}<select [attr.aria-label]="'uiText.installation'|t" [ngModel]="participationId" (ngModelChange)="select($event)" [disabled]="busy()||mode==='edit'"><option [ngValue]="null">{{'uiText.choose.an.installation'|t}}</option>@for(p of parts();track p.id){<option [ngValue]="p.id">{{label(p)}}</option>}</select></label>}
@if(mode==='edit'&&loaded()){<p class="muted">{{'uiText.content.cannot.move.to.another.installation.create.new.content.for.that.instal'|t}}</p>}
@if(loaded()&&!parts().length){<p class="empty">{{'uiText.first.create.an.exhibit.installation.in.the.exhibition'|t}}</p>}
@if(!mode&&participationId&&contentLoaded()){
<app-image-editor [endpoint]="'/curator/participations/'+participationId+'/images'" [contextual]="true" (saved)="select(participationId)" />
<section class="panel bilingual-content"><h2>{{'uiText.text.content'|t}}</h2><label>{{'nav.language'|t}}<select [(ngModel)]="textLanguage" (ngModelChange)="loadText()" [disabled]="busy()"><option value="sr">SR</option><option value="en">EN</option></select></label><textarea [(ngModel)]="textValue" rows="6" [attr.placeholder]="'uiText.enter.text.in.the.selected.language'|t"></textarea><button class="button" [disabled]="busy()||!textValue.trim()" (click)="saveText()">{{'uiText.save.text'|t}}</button></section>
<section class="panel audio-generation"><h2>{{'visitor.audio'|t}}</h2><label>{{'nav.language'|t}}<select [(ngModel)]="audioLanguage" [disabled]="busy()"><option value="sr">SR</option><option value="en">EN</option></select></label>
@if(!hasContext()){<p>{{'uiText.first.enter.and.save.this.installation.s.contextual.description'|t}}</p>}
<button class="button" [disabled]="busy()||!hasContext()" (click)="generateAudio()">{{busy()?('uiText.processing'|t):('uiText.generate.audio'|t)}}</button></section>
<div class="cards">@for(c of content();track c.id){<article class="panel"><h2>{{c.type|uiLabel:'content'}} {{'uiText.version'|t}} {{c.version}}</h2>@if(c.type==='SLIKA'&&!c.path){<app-media-image [path]="'/curator/participations/'+participationId+'/images/'+c.id" [protected]="true" [alt]="'media.contextTitle'|t" [contain]="true" />}<p class="curator-reference">{{c.path}}</p><p>{{'uiText.generated'|t}} {{c.createdAt|date:'dd.MM.yyyy. HH:mm'}} · {{c.automaticallyGenerated?('uiText.automatically.generated.content'|t):('uiText.manually.added.content'|t)}}</p><p>{{'uiText.language'|t}} {{c.language||('uiText.not.specified'|t)}}</p><div class="curator-actions">@if(c.path){<a class="button secondary" [routerLink]="['/curator/digital-content',c.id,'edit']">{{'uiText.edit'|t}}</a>}<button class="button danger" [disabled]="busy()" (click)="pending.set(c.id)">{{'uiText.delete'|t}}</button></div>
@if(pending()===c.id){<p role="alert">{{'uiText.delete.this.content.reference.this.cannot.be.undone'|t}}</p><button class="button danger" [disabled]="busy()" (click)="remove(c.id)">{{'uiText.confirm.deletion'|t}}</button><button class="button secondary" [disabled]="busy()" (click)="pending.set(null)">{{'uiText.cancel'|t}}</button>}</article>}@empty{<p class="empty">{{'uiText.this.installation.has.no.digital.content.yet'|t}}</p>}</div>
}
@if(mode&&loaded()&&(mode==='new'||contentLoaded())){
<section class="panel"><h2>{{editId?('uiText.edit.content'|t):('uiText.add.content'|t)}}</h2><p>{{'uiText.enter.a.reference.to.an.existing.resource.this.form.does.not.upload.or.check.r'|t}}</p>
<form #form="ngForm" (ngSubmit)="save(form)"><fieldset [disabled]="busy()"><div class="curator-form-grid">
<label>{{'uiText.type'|t}}<select name="type" [(ngModel)]="type" required><option>TEKST</option><option>SLIKA</option><option>AUDIO</option></select></label>
<label>{{'nav.language'|t}}<input name="language" [(ngModel)]="language" placeholder="sr"></label>
<label class="full-width">{{'uiText.content.path.or.url'|t}}<input name="path" [(ngModel)]="path" required placeholder="demo/grad-001.txt"><small>{{'uiText.a.relative.path.or.http.s.url.without.credentials.use.a.resource.intended.for.'|t}}</small></label>
<label>{{'uiText.version.2'|t}}<input name="version" type="number" [(ngModel)]="version" required min="1" step="1"></label>
<label class="checkbox"><input type="checkbox" name="generated" [(ngModel)]="generated">{{'uiText.automatically.generated.content'|t}}</label></div>
@if(form.submitted&&form.invalid){<p class="field-error">{{'uiText.check.required.fields.and.the.version'|t}}</p>}
<div class="curator-actions"><button class="button" [disabled]="busy()||!participationId" type="submit">{{'uiText.save.content'|t}}</button>@if(mode==='new'){<button class="button secondary" type="button" (click)="reset()">{{'uiText.clear.form'|t}}</button>}</div>
</fieldset></form></section>}
`})
export class CuratorContent extends CuratorState {
  private route=inject(ActivatedRoute);private router=inject(Router);mode=this.route.snapshot.data['contentMode'] as 'new'|'edit'|undefined;
  private api=inject(Api);parts=signal<ManagedParticipation[]>([]);exhibits=signal<CatalogExhibit[]>([]);exhibitions=signal<Exhibition[]>([]);
  content=signal<DigitalContent[]>([]);loaded=signal(false);contentLoaded=signal(false);pending=signal<number|null>(null);
  participationId:number|null=null;editId:number|null=null;type:DigitalContent['type']='TEKST';language='sr';path='';version:number|null=1;generated=false;
  audioLanguage='sr';
  textLanguage:'sr'|'en'='sr'; textValue='';
  hasContext(){return !!this.parts().find(p=>p.id===this.participationId)?.contextDescription?.trim();}
  generateAudio(){if(this.busy()||!this.participationId||!this.hasContext())return;
    void this.run(async()=>{try{
      const created=await this.api.post<DigitalContent>(`/curator/participations/${this.participationId}/audio`,{language:this.audioLanguage});
      this.content.update(rows=>[created,...rows]);this.notice.set((translate("uiText.audio.guide.generated.version")+' ')+created.version+'.');
    }catch(e){if(e instanceof HttpErrorResponse&&e.error?.code==='LOCAL_AUDIO_ERROR'){console.error('LOCAL_AUDIO_ERROR', {status:e.status,code:e.error.code});this.error.set(translate('error.audioGeneration'));}else throw e;}});
  }
  loadText(){if(!this.participationId)return;this.textValue='';void this.run(async()=>{try{const r=await this.api.get<{text:string}>(`/curator/participations/${this.participationId}/texts/${this.textLanguage}`);this.textValue=r.text;}catch{}});}
  saveText(){if(!this.participationId||!this.textValue.trim())return;void this.run(async()=>{await this.api.post(`/curator/participations/${this.participationId}/texts`,{language:this.textLanguage,text:this.textValue});this.notice.set(translate("uiText.text.content.saved"));this.content.set(await this.api.get<DigitalContent[]>(`/exhibit-participations/${this.participationId}/digital-content`));});}
  constructor(){super();this.load();}
  label(p:ManagedParticipation){return (this.exhibits().find(e=>e.id===p.exhibitId)?.name||translate("uiText.exhibit.2"))+' — '+(this.exhibitions().find(e=>e.id===p.exhibitionId)?.name||translate("ui.exhibition"))+(" "+translate("uiText.installation.2")+' ')+p.id;}
  load(){void this.run(async()=>{
    const rawId=this.route.snapshot.paramMap.get('id');
    if(this.mode==='edit'&&(!rawId||!validCatalogId(rawId))){this.error.set(translate("uiText.content.not.found"));return;}
    const [p,e,x,c]=await Promise.all([this.api.get<ManagedParticipation[]>('/exhibit-participations'),this.api.get<CatalogExhibit[]>('/exhibits'),this.api.get<Exhibition[]>('/exhibitions'),this.mode==='edit'?this.api.get<DigitalContent>('/digital-content/'+rawId):Promise.resolve(null)]);
    this.parts.set(p);this.exhibits.set(e);this.exhibitions.set(x);
    if(c){this.participationId=c.participationId;this.edit(c);this.contentLoaded.set(true);}
    else{const selected=Number(this.route.snapshot.queryParamMap.get('participationId'));this.participationId=p.some(row=>row.id===selected)?selected:null;}
    if(!this.mode&&this.participationId){this.content.set(await this.api.get<DigitalContent[]>('/exhibit-participations/'+this.participationId+'/digital-content'));this.contentLoaded.set(true);}
    this.loaded.set(true);
    if(this.route.snapshot.queryParamMap.has('saved'))this.notice.set(translate("uiText.digital.content.saved"));
  });}
  retry(){if(!this.loaded()||this.mode)this.load();else this.select(this.participationId);}
  select(id:number|null){if(this.busy()||this.mode==='edit')return;this.participationId=id;this.content.set([]);this.contentLoaded.set(false);this.pending.set(null);if(!this.mode){this.reset();if(id)void this.run(async()=>{this.content.set(await this.api.get<DigitalContent[]>('/exhibit-participations/'+id+'/digital-content'));this.contentLoaded.set(true);});}}
  edit(c:DigitalContent){this.editId=c.id;this.type=c.type;this.language=c.language||'';this.path=c.path;this.version=c.version;this.generated=c.automaticallyGenerated;}
  reset(){this.editId=null;this.type='TEKST';this.language='sr';this.path='';this.version=1;this.generated=false;}
  save(form:NgForm){
    if(form.invalid||!this.participationId||!this.mode)return;
    if(!validReference(this.path)||!Number.isInteger(this.version)||Number(this.version)<1){this.error.set(translate("uiText.enter.a.safe.http.s.url.or.relative.path.and.a.positive.integer.version"));return;}
    void this.run(async()=>{const body={participationId:this.participationId,type:this.type,path:this.path.trim(),language:this.language.trim(),version:this.version,automaticallyGenerated:this.generated};
      const c=await (this.editId?this.api.put<DigitalContent>('/digital-content/'+this.editId,body):this.api.post<DigitalContent>('/digital-content',body));
      const creating=!this.editId;this.edit(c);this.notice.set(translate("uiText.digital.content.saved"));
      if(creating)await this.router.navigate(['/curator/digital-content',c.id,'edit'],{queryParams:{saved:1},replaceUrl:true});
    });
  }
  remove(id:number){if(this.pending()!==id)return;void this.run(async()=>{await this.api.delete('/digital-content/'+id);this.content.update(rows=>rows.filter(c=>c.id!==id));this.pending.set(null);if(this.editId===id)this.reset();this.notice.set(translate("uiText.content.deleted"));});}
}
