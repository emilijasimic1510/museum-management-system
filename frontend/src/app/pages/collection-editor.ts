import { translate } from './../core/i18n';
import { ImageEditor } from '../shared/image-editor';
import { Auth } from '../core/auth';
import { Component, inject, signal } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Api } from '../core/api';
import { CollectionKind, CollectionRecord, CollectionState, Value, fields, collectionLabels, localTimestamp, toInstant } from '../core/collection-management';
import { Feedback } from '../shared/feedback';
import { TranslatePipe } from '../core/i18n';

@Component({imports:[ImageEditor,FormsModule,RouterLink,Feedback,TranslatePipe],template:`
<a class="back" [routerLink]="['/collection',kind]">{{label|t}}</a>
<section class="page-heading"><h1>{{mode==='new'?('uiText.new.record'|t):mode==='edit'?('uiText.edit'|t):('uiText.details.2'|t)}} · {{label|t}}</h1></section>
<app-feedback [busy]="busy()" [error]="error()" [notice]="notice()" />
@if(!loaded()&&!busy()){<button class="button secondary" (click)="load()">{{'ui.retry'|t}}</button>}
@if(loaded()){
<form #form="ngForm" (ngSubmit)="save(form)" class="panel">
<fieldset class="collection-fields" [disabled]="busy()||mode==='view'"><div class="collection-grid">
@for(f of formFields;track f.key){<label [class.collection-wide]="f.type==='textarea'" [class.checkbox]="f.type==='checkbox'">
  {{f.label|t}} @if(f.required){<span class="required-mark" aria-hidden="true">*</span>}
  @if(f.type==='select'){<select [name]="f.key" [attr.name]="f.key" [(ngModel)]="model[f.key]" [required]="!!f.required"><option [ngValue]="null">{{'uiText.choose'|t}}</option>@for(o of f.options||parentOptions();track $index){<option [ngValue]="o.value">{{f.options?(o.label|t):o.label}}</option>}</select>}
  @else if(f.type==='textarea'){<textarea [name]="f.key" [attr.name]="f.key" [(ngModel)]="model[f.key]" rows="4"></textarea>}
  @else if(f.type==='checkbox'){<input type="checkbox" [name]="f.key" [attr.name]="f.key" [(ngModel)]="model[f.key]">}
  @else if(f.type==='number'){<input type="number" [name]="f.key" [attr.name]="f.key" [(ngModel)]="model[f.key]" [required]="!!f.required" [min]="f.min??null" [step]="f.step||'any'">}
  @else if(f.type==='datetime-local'){<input type="datetime-local" [name]="f.key" [attr.name]="f.key" [(ngModel)]="model[f.key]" [required]="!!f.required" step="0.001">}
  @else{<input type="text" [name]="f.key" [attr.name]="f.key" [(ngModel)]="model[f.key]" [required]="!!f.required">}
</label>}
</div>
@if(kind==='exhibitions'&&mode==='new'){<h2>{{'uiText.first.time.slot'|t}}</h2><p>{{'uiText.an.exhibition.must.have.at.least.one.time.slot.within.its.date.range'|t}}</p><div class="collection-grid">
<label>{{'uiText.time.slot.start'|t}} <span class="required-mark" aria-hidden="true">*</span><input type="datetime-local" name="initialStart" [(ngModel)]="initial.start" required step="0.001"></label>
<label>{{'uiText.time.slot.end'|t}} <span class="required-mark" aria-hidden="true">*</span><input type="datetime-local" name="initialEnd" [(ngModel)]="initial.end" required step="0.001"></label>
<label>{{'event.capacity'|t}} <span class="required-mark" aria-hidden="true">*</span><input type="number" name="initialCapacity" [(ngModel)]="initial.capacity" required min="1" step="1"></label>
</div>}</fieldset>
@if(mode!=='view'){<p class="muted">{{'uiText.complete.all.required.fields.area.and.capacity.must.be.positive.when.provided.'|t}}</p>@if(form.invalid&&form.touched){<p class="message error" role="alert">{{'uiText.complete.the.required.fields.and.ensure.area.and.capacity.are.positive'|t}}</p>}<button class="button" [disabled]="busy()||form.invalid">{{'uiText.save'|t}}</button>}
</form>
@if(kind==='exhibitions'&&mode==='edit'&&canUploadImage()){<app-image-editor [endpoint]="'/curator/media/exhibitions/'+id" [preview]="'/curator/media/exhibitions/'+id" />}
@if(mode!=='new'){<div class="collection-actions">
@if(mode==='view'){<a class="button" [routerLink]="['/collection',kind,id,'edit']">{{'uiText.edit'|t}}</a>}
@if(kind==='museums'){<a routerLink="/collection/halls" [queryParams]="{museumId:id}">{{'uiText.museum.halls'|t}}</a><button class="button danger" (click)="confirm.set(true)" [disabled]="busy()||!model['active']">{{'uiText.deactivate.museum'|t}}</button>}
@else{<button class="button danger" (click)="confirm.set(true)" [disabled]="busy()">{{'uiText.remove.safely'|t}}</button>}
@if(kind==='exhibitions'){<a routerLink="/collection/exhibition-terms" [queryParams]="{exhibitionId:id}">{{'uiText.time.slots'|t}}</a><a [routerLink]="['/collection/exhibitions',id,'prices']">{{'uiText.price.list'|t}}</a>}
@if(kind==='halls'){<a routerLink="/collection/exhibitions" [queryParams]="{hallId:id}">{{'uiText.exhibitions.in.this.hall'|t}}</a>}
</div>}
@if(confirm()){<section class="panel"><h2>{{kind==='museums'?('uiText.deactivate.this.museum'|t):('uiText.remove.this.record'|t)}}</h2><p>{{kind==='museums'?('uiText.historical.data.is.retained'|t):('uiText.deletion.is.rejected.if.dependent.records.exist.or.an.exhibition.would.lose.it'|t)}}</p><div class="collection-actions"><button class="button danger" [disabled]="busy()" (click)="remove()">{{'uiText.confirm'|t}}</button><button class="button secondary" [disabled]="busy()" (click)="confirm.set(false)">{{'uiText.cancel'|t}}</button></div></section>}
}
`})
export class CollectionEditor extends CollectionState {
  private imageAuth=inject(Auth);canUploadImage(){return this.imageAuth.isCurator();}
  private api=inject(Api);private route=inject(ActivatedRoute);private router=inject(Router);
  kind=this.route.snapshot.data['kind'] as CollectionKind;mode=this.route.snapshot.data['mode'] as 'new'|'edit'|'view';
  id=Number(this.route.snapshot.paramMap.get('id'));label=collectionLabels[this.kind];formFields=fields[this.kind];
  loaded=signal(false);confirm=signal(false);model:Record<string,Value>={};original:CollectionRecord|null=null;
  parentOptions=signal<{value:number;label:string}[]>([]);parents:CollectionRecord[]=[];
  initial={start:'',end:'',capacity:1};
  constructor(){super();this.load();}
  load(){void this.run(async()=>{
    this.loaded.set(false);
    if(this.mode!=='new'&&(!Number.isSafeInteger(this.id)||this.id<=0)){this.error.set(translate("uiText.invalid.record.id"));return;}
    const parent=({halls:'museums',exhibitions:'halls','exhibition-terms':'exhibitions'} as Partial<Record<CollectionKind,string>>)[this.kind];
    const [row,parents]=await Promise.all([this.mode==='new'?Promise.resolve(null):this.api.get<CollectionRecord>('/'+this.kind+'/'+this.id,10000),parent?this.api.get<CollectionRecord[]>('/'+parent,10000):Promise.resolve([])]);
    this.parents=parents;this.parentOptions.set(parents.map(p=>({value:p.id,label:(p.name||'#'+p.id)+(p.museumId?(" "+translate("uiText.museum.3"))+p.museumId:'')})));this.original=row;
    this.model={active:true,onlineVisible:false,status:this.kind==='exhibitions'?'PLANIRANA':'OTVOREN'};
    for(const f of this.formFields){const value=row?.[f.key];if(row)this.model[f.key]=f.type==='datetime-local'&&typeof value==='string'?localTimestamp(value):(value??null) as Value;
      else if(f.type==='select'&&f.key.endsWith('Id'))this.model[f.key]=Number(this.route.snapshot.queryParamMap.get(f.key))||null;}
    this.loaded.set(true);
    if(this.route.snapshot.queryParamMap.has('saved'))this.notice.set(translate("uiText.record.created"));
  });}
  body(){const body:Record<string,unknown>={};for(const f of this.formFields){const v=this.model[f.key];body[f.key]=f.type==='datetime-local'?toInstant(v,this.original?.[f.key]):typeof v==='string'?v.trim()||null:v??null;}return body;}
  save(form:NgForm){if(form.invalid||this.busy()||!this.loaded()||this.mode==='view')return;
    void this.run(async()=>{const body=this.body();
      if(this.kind==='exhibitions'||this.kind==='exhibition-terms'){
        if(Date.parse(String(body['end']))<=Date.parse(String(body['start']))){this.error.set(translate("uiText.the.end.must.be.after.the.start"));return;}
      }
      if(this.kind==='exhibitions'&&this.mode==='new'){
        const start=toInstant(this.initial.start),end=toInstant(this.initial.end);
        if(Date.parse(end)<=Date.parse(start)||Date.parse(start)<Date.parse(String(body['start']))||Date.parse(end)>Date.parse(String(body['end']))){this.error.set(translate("uiText.the.first.time.slot.must.fall.within.the.exhibition.period.and.end.after.it.st"));return;}
        body['terms']=[{start,end,capacity:this.initial.capacity,status:'OTVOREN'}];
      }
      const saved=this.mode==='new'?await this.api.post<CollectionRecord>('/'+this.kind,body):await this.api.put<CollectionRecord>('/'+this.kind+'/'+this.id,body);
      if(this.mode==='new')await this.router.navigate(['/collection',this.kind,saved.id,'edit'],{queryParams:{saved:1}});
      else{this.original=saved;
        for(const f of this.formFields){const value=saved[f.key];this.model[f.key]=f.type==='datetime-local'&&typeof value==='string'?localTimestamp(value):(value??null) as Value;}
        this.notice.set(translate("uiText.changes.saved"));}
    });
  }
  remove(){if(!this.confirm()||this.busy())return;void this.run(async()=>{
    if(this.kind==='museums'){const body=Object.fromEntries(this.formFields.map(f=>[f.key,this.original?.[f.key]??null]));const row=await this.api.put<CollectionRecord>('/museums/'+this.id,{...body,active:false});this.original=row;this.model['active']=false;this.confirm.set(false);this.notice.set(translate("uiText.museum.deactivated.historical.data.retained"));}
    else{await this.api.delete('/'+this.kind+'/'+this.id);await this.router.navigate(['/collection',this.kind],{queryParams:{removed:1}});}
  });}
}
