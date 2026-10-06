import { translate } from './../core/i18n';
import { ImageEditor } from '../shared/image-editor';
import { Component, inject, signal } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Api } from '../core/api';
import { Author, CatalogExhibit, validCatalogId } from '../core/catalog';
import { CuratorState, localDate, ManagedParticipation } from '../core/curator';
import { Exhibition } from '../core/models';
import { Feedback } from '../shared/feedback';
import { TranslatePipe } from '../core/i18n';
import { CuratorAuthorships } from './curator-authorships';
import { ParticipationQr } from '../shared/participation-qr';

interface Field {key:string;label:string;required?:boolean;type?:string;min?:number}
@Component({imports:[ImageEditor,FormsModule,RouterLink,Feedback,CuratorAuthorships,ParticipationQr,TranslatePipe],template:`
<section class="page-heading"><p class="eyebrow">{{'uiText.catalog.management'|t}}</p><h1>{{id?('uiText.edit'|t):('uiText.new.record'|t)}} · {{title|t}}</h1><a [routerLink]="base">{{'uiText.back.to.list'|t}}</a></section>
<app-feedback [busy]="busy()" [error]="error()" [notice]="notice()" />
@if(!loaded()&&error()){<button class="button" [disabled]="busy()" (click)="load()">{{'ui.retry'|t}}</button>}
@if(loaded()){<section class="panel"><form #form="ngForm" (ngSubmit)="save(form)"><fieldset [disabled]="busy()">
@if(kind==='participations'){<div class="form-row"><label>{{'uiText.exhibition.2'|t}}<select name="exhibitionId" [(ngModel)]="model['exhibitionId']" required [disabled]="!!id"><option [ngValue]="null">{{'ui.chooseExhibition'|t}}</option>@for(e of exhibitions();track e.id){<option [ngValue]="e.id">{{e.name}}</option>}</select></label>
<label>{{'uiText.exhibit'|t}}<select name="exhibitId" [(ngModel)]="model['exhibitId']" required [disabled]="!!id"><option [ngValue]="null">{{'uiText.choose.an.exhibit'|t}}</option>@for(e of exhibits();track e.id){<option [ngValue]="e.id">{{e.name}}</option>}</select></label></div>
@if(id){<p class="muted">{{'uiText.an.existing.installation.s.exhibition.and.exhibit.cannot.change.because.of.sca'|t}}</p>}}
<div class="curator-form-grid">@for(field of fields;track field.key){<label [class.full-width]="field.type==='textarea'">{{field.label}}{{field.required?' *':''}}
@if(field.type==='textarea'){<textarea [name]="field.key" [(ngModel)]="model[field.key]" [required]="!!field.required" rows="4"></textarea>}
@else{<input [name]="field.key" [type]="field.type||'text'" [(ngModel)]="model[field.key]" [required]="!!field.required" [attr.min]="field.min??null" [min]="field.min??null" [step]="field.type==='number'?1:null">}
</label>}</div>
@if(form.submitted&&form.invalid){<p class="field-error">{{'uiText.complete.the.required.fields.and.check.the.numbers'|t}}</p>}
<div class="curator-actions"><button class="button" type="submit" [disabled]="busy()">{{busy()?('uiText.saving'|t):('uiText.save'|t)}}</button><a class="button secondary" [routerLink]="base">{{'uiText.back.to.list.2'|t}}</a></div>
</fieldset></form></section>
@if(participation();as p){<section class="panel"><h2>{{'uiText.installation.qr.data'|t}}</h2><p>{{'uiText.the.server.generates.the.token.for.this.installation.editing.does.not.change.i'|t}}</p><app-participation-qr [token]="p.qrToken" /><label>{{'ui.scanToken'|t}}<input readonly [value]="p.qrToken"></label><button class="button secondary" (click)="copyToken()">{{'uiText.copy'|t}}</button><p role="status">{{copyStatus()}}</p><p class="muted">{{'uiText.scanning.is.available.only.during.an.active.visit'|t}}</p></section>}
@if(kind==='exhibits'&&id){<app-image-editor [endpoint]="'/curator/media/exhibits/'+id" [preview]="'/curator/media/exhibits/'+id" /><app-curator-authorships [exhibitId]="id" />}
}
`})
export class CuratorEditor extends CuratorState {
  private api=inject(Api);private route=inject(ActivatedRoute);private router=inject(Router);
  kind=this.route.snapshot.data['kind'] as string;id=Number(this.route.snapshot.paramMap.get('id'))||null;
  base='/curator/'+this.kind;title=this.kind==='exhibits'?translate("uiText.exhibit.2"):this.kind==='authors'?translate("uiText.author.2"):translate("uiText.installation");
  endpoint='/'+(this.kind==='participations'?'exhibit-participations':this.kind);
  fields:Field[]=this.kind==='exhibits'?[
    {key:'name',label:translate("uiText.name"),required:true},{key:'inventoryNumber',label:translate("uiText.inventory.number"),required:true},
    {key:'creationPeriod',label:translate("uiText.creation.period.year")},{key:'type',label:translate("uiText.type.2")},{key:'material',label:translate("uiText.material")},{key:'origin',label:translate("uiText.origin")},{key:'description',label:translate("uiText.description"),type:'textarea'}
  ]:this.kind==='authors'?[
    {key:'firstName',label:translate("login.firstName"),required:true},{key:'lastName',label:translate("login.lastName")},{key:'birthYear',label:translate("uiText.birth.year"),type:'number'},{key:'deathYear',label:translate("uiText.death.year"),type:'number'},{key:'biography',label:translate("public.biography"),type:'textarea'}
  ]:[
    {key:'installedAt',label:translate("uiText.installation.date"),type:'datetime-local'},{key:'removedAt',label:translate("uiText.removal.date"),type:'datetime-local'},{key:'position',label:translate("uiText.position")},{key:'tourOrder',label:translate("qrBoard.order"),type:'number',min:1},{key:'contextDescription',label:translate("uiText.contextual.description"),type:'textarea'}
  ];
  model:Record<string,string|number|null>={};loaded=signal(false);exhibits=signal<CatalogExhibit[]>([]);exhibitions=signal<Exhibition[]>([]);
  participation=signal<ManagedParticipation|null>(null);copyStatus=signal('');
  constructor(){super();for(const f of this.fields)this.model[f.key]=f.type==='number'?null:'';this.model['exhibitId']=null;this.model['exhibitionId']=null;this.load();}
  load(){void this.run(async()=>{
    const rawId=this.route.snapshot.paramMap.get('id');
    if(rawId&&!validCatalogId(rawId)){this.error.set(translate("uiText.the.requested.record.was.not.found"));return;}
    if(this.kind==='participations'){const [e,x]=await Promise.all([this.api.get<CatalogExhibit[]>('/exhibits'),this.api.get<Exhibition[]>('/exhibitions')]);this.exhibits.set(e);this.exhibitions.set(x);}
    if(this.id){
      if(this.kind==='exhibits'){const r=await this.api.get<{exhibit:CatalogExhibit;inventoryNumber:string}>(this.endpoint+'/'+this.id+'/management');this.model={...r.exhibit,inventoryNumber:r.inventoryNumber} as unknown as typeof this.model;}
      else if(this.kind==='authors')this.model=await this.api.get<Author>(this.endpoint+'/'+this.id) as unknown as typeof this.model;
      else{const p=await this.api.get<ManagedParticipation>(this.endpoint+'/'+this.id);this.participation.set(p);this.model={exhibitId:p.exhibitId,exhibitionId:p.exhibitionId,installedAt:localDate(p.installedAt),removedAt:localDate(p.removedAt),position:p.position,tourOrder:p.tourOrder,contextDescription:p.contextDescription};}
    }
    this.loaded.set(true);
    if(this.route.snapshot.queryParamMap.has('saved'))this.notice.set(translate("uiText.record.saved"));
  });}
  save(form:NgForm){
    if(form.invalid)return;
    if(this.fields.some(f=>f.required&&!String(this.model[f.key]??'').trim())){this.error.set(translate("uiText.required.fields.cannot.be.empty"));return;}
    const body:Record<string,unknown>={};
    for(const f of this.fields){const v=this.model[f.key];body[f.key]=typeof v==='string'?v.trim():v;}
    if(this.kind==='authors'&&body['birthYear']!=null&&body['deathYear']!=null&&Number(body['deathYear'])<Number(body['birthYear'])){this.error.set(translate("uiText.death.year.cannot.precede.birth.year"));return;}
    if(this.kind==='participations'){
      body['exhibitId']=this.model['exhibitId'];body['exhibitionId']=this.model['exhibitionId'];
      if(!body['exhibitId']||!body['exhibitionId']){this.error.set(translate("uiText.choose.an.exhibition.and.exhibit"));return;}
      for(const key of ['installedAt','removedAt']){
        const value=this.model[key];
        if(value&&!Number.isFinite(Date.parse(String(value)))){this.error.set(translate("uiText.enter.a.valid.date.and.time"));return;}
        body[key]=value?new Date(String(value)).toISOString():null;
      }
      if(body['installedAt']&&body['removedAt']&&String(body['removedAt'])<=String(body['installedAt'])){this.error.set(translate("uiText.removal.must.be.after.installation"));return;}
      // Omit qrToken: backend generates on create and preserves on update.
    }
    void this.run(async()=>{
      const creating=!this.id;
      type Saved=Author|ManagedParticipation|{exhibit:CatalogExhibit;inventoryNumber:string};
      const r=await (this.id?this.api.put<Saved>(this.endpoint+'/'+this.id,body):this.api.post<Saved>(this.endpoint,body));
      this.id='exhibit' in r?r.exhibit.id:r.id;
      if(this.kind==='participations')this.participation.set(r as ManagedParticipation);
      this.notice.set(translate("uiText.record.saved.continue.editing.or.return.to.the.list"));
      if(creating)await this.router.navigate([this.base,this.id,'edit'],{queryParams:{saved:1},replaceUrl:true});
    });
  }
  async copyToken(){try{await navigator.clipboard.writeText(this.participation()?.qrToken||'');this.copyStatus.set(translate("uiText.token.copied"));}catch{this.copyStatus.set(translate("uiText.copying.is.unavailable.select.and.copy.the.text.manually"));}}
}
