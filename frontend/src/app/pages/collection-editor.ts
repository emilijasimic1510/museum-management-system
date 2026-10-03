import { Component, inject, signal } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Api } from '../core/api';
import { CollectionKind, CollectionRecord, CollectionState, Value, fields, collectionLabels, localTimestamp, toInstant } from '../core/collection-management';
import { Feedback } from '../shared/feedback';

@Component({imports:[FormsModule,RouterLink,Feedback],template:`
<a class="back" [routerLink]="['/collection',kind]">← {{label}}</a>
<section class="page-heading"><h1>{{mode==='new'?'Novi zapis':mode==='edit'?'Izmena':'Detalj'}} · {{label}}</h1></section>
<app-feedback [busy]="busy()" [error]="error()" [notice]="notice()" />
@if(!loaded()&&!busy()){<button class="button secondary" (click)="load()">Pokušaj ponovo</button>}
@if(loaded()){
<form #form="ngForm" (ngSubmit)="save(form)" class="panel">
<fieldset class="collection-fields" [disabled]="busy()||mode==='view'"><div class="collection-grid">
@for(f of formFields;track f.key){<label [class.collection-wide]="f.type==='textarea'" [class.checkbox]="f.type==='checkbox'">
  {{f.label}} @if(f.required){<span class="required-mark" aria-hidden="true">*</span>}
  @if(f.type==='select'){<select [name]="f.key" [attr.name]="f.key" [(ngModel)]="model[f.key]" [required]="!!f.required"><option [ngValue]="null">Izaberite</option>@for(o of f.options||parentOptions();track $index){<option [ngValue]="o.value">{{o.label}}</option>}</select>}
  @else if(f.type==='textarea'){<textarea [name]="f.key" [attr.name]="f.key" [(ngModel)]="model[f.key]" rows="4"></textarea>}
  @else if(f.type==='checkbox'){<input type="checkbox" [name]="f.key" [attr.name]="f.key" [(ngModel)]="model[f.key]">}
  @else if(f.type==='number'){<input type="number" [name]="f.key" [attr.name]="f.key" [(ngModel)]="model[f.key]" [required]="!!f.required" [min]="f.min??null" [step]="f.step||'any'">}
  @else if(f.type==='datetime-local'){<input type="datetime-local" [name]="f.key" [attr.name]="f.key" [(ngModel)]="model[f.key]" [required]="!!f.required" step="0.001">}
  @else{<input type="text" [name]="f.key" [attr.name]="f.key" [(ngModel)]="model[f.key]" [required]="!!f.required">}
</label>}
</div>
@if(kind==='exhibitions'&&mode==='new'){<h2>Prvi termin</h2><p>Izložba mora imati najmanje jedan termin unutar svog perioda.</p><div class="collection-grid">
<label>Početak termina <span class="required-mark" aria-hidden="true">*</span><input type="datetime-local" name="initialStart" [(ngModel)]="initial.start" required step="0.001"></label>
<label>Kraj termina <span class="required-mark" aria-hidden="true">*</span><input type="datetime-local" name="initialEnd" [(ngModel)]="initial.end" required step="0.001"></label>
<label>Kapacitet <span class="required-mark" aria-hidden="true">*</span><input type="number" name="initialCapacity" [(ngModel)]="initial.capacity" required min="1" step="1"></label>
</div>}</fieldset>
@if(mode!=='view'){<p class="muted">Obavezna polja moraju biti popunjena. Površina i kapacitet moraju biti pozitivni kada su navedeni. Datumi se unose u lokalnoj vremenskoj zoni.</p>@if(form.invalid&&form.touched){<p class="message error" role="alert">Popunite obavezna polja i proverite da su površina i kapacitet pozitivni.</p>}<button class="button" [disabled]="busy()||form.invalid">Sačuvaj</button>}
</form>
@if(mode!=='new'){<div class="collection-actions">
@if(mode==='view'){<a class="button" [routerLink]="['/collection',kind,id,'edit']">Izmeni</a>}
@if(kind==='museums'){<a routerLink="/collection/halls" [queryParams]="{museumId:id}">Sale muzeja</a><button class="button danger" (click)="confirm.set(true)" [disabled]="busy()||!model['active']">Deaktiviraj muzej</button>}
@else{<button class="button danger" (click)="confirm.set(true)" [disabled]="busy()">Bezbedno ukloni</button>}
@if(kind==='exhibitions'){<a routerLink="/collection/exhibition-terms" [queryParams]="{exhibitionId:id}">Termini</a><a [routerLink]="['/collection/exhibitions',id,'prices']">Cenovnik</a>}
@if(kind==='halls'){<a routerLink="/collection/exhibitions" [queryParams]="{hallId:id}">Izložbe u sali</a>}
</div>}
@if(confirm()){<section class="panel"><h2>{{kind==='museums'?'Deaktivirati muzej?':'Ukloniti zapis?'}}</h2><p>{{kind==='museums'?'Istorijski podaci ostaju sačuvani.':'Brisanje će biti odbijeno ako postoje zavisni podaci ili bi izložba ostala bez termina. Za izložbe i termine sa istorijom koristite izmenu statusa.'}}</p><div class="collection-actions"><button class="button danger" [disabled]="busy()" (click)="remove()">Potvrdi</button><button class="button secondary" [disabled]="busy()" (click)="confirm.set(false)">Odustani</button></div></section>}
}
`})
export class CollectionEditor extends CollectionState {
  private api=inject(Api);private route=inject(ActivatedRoute);private router=inject(Router);
  kind=this.route.snapshot.data['kind'] as CollectionKind;mode=this.route.snapshot.data['mode'] as 'new'|'edit'|'view';
  id=Number(this.route.snapshot.paramMap.get('id'));label=collectionLabels[this.kind];formFields=fields[this.kind];
  loaded=signal(false);confirm=signal(false);model:Record<string,Value>={};original:CollectionRecord|null=null;
  parentOptions=signal<{value:number;label:string}[]>([]);parents:CollectionRecord[]=[];
  initial={start:'',end:'',capacity:1};
  constructor(){super();this.load();}
  load(){void this.run(async()=>{
    this.loaded.set(false);
    if(this.mode!=='new'&&(!Number.isSafeInteger(this.id)||this.id<=0)){this.error.set('Neispravan ID zapisa.');return;}
    const parent=({halls:'museums',exhibitions:'halls','exhibition-terms':'exhibitions'} as Partial<Record<CollectionKind,string>>)[this.kind];
    const [row,parents]=await Promise.all([this.mode==='new'?Promise.resolve(null):this.api.get<CollectionRecord>('/'+this.kind+'/'+this.id,10000),parent?this.api.get<CollectionRecord[]>('/'+parent,10000):Promise.resolve([])]);
    this.parents=parents;this.parentOptions.set(parents.map(p=>({value:p.id,label:(p.name||'#'+p.id)+(p.museumId?' · muzej #'+p.museumId:'')})));this.original=row;
    this.model={active:true,onlineVisible:false,status:this.kind==='exhibitions'?'PLANIRANA':'OTVOREN'};
    for(const f of this.formFields){const value=row?.[f.key];if(row)this.model[f.key]=f.type==='datetime-local'&&typeof value==='string'?localTimestamp(value):(value??null) as Value;
      else if(f.type==='select'&&f.key.endsWith('Id'))this.model[f.key]=Number(this.route.snapshot.queryParamMap.get(f.key))||null;}
    this.loaded.set(true);
    if(this.route.snapshot.queryParamMap.has('saved'))this.notice.set('Zapis je kreiran.');
  });}
  body(){const body:Record<string,unknown>={};for(const f of this.formFields){const v=this.model[f.key];body[f.key]=f.type==='datetime-local'?toInstant(v,this.original?.[f.key]):typeof v==='string'?v.trim()||null:v??null;}return body;}
  save(form:NgForm){if(form.invalid||this.busy()||!this.loaded()||this.mode==='view')return;
    void this.run(async()=>{const body=this.body();
      if(this.kind==='exhibitions'||this.kind==='exhibition-terms'){
        if(Date.parse(String(body['end']))<=Date.parse(String(body['start']))){this.error.set('Kraj mora biti nakon početka.');return;}
      }
      if(this.kind==='exhibitions'&&this.mode==='new'){
        const start=toInstant(this.initial.start),end=toInstant(this.initial.end);
        if(Date.parse(end)<=Date.parse(start)||Date.parse(start)<Date.parse(String(body['start']))||Date.parse(end)>Date.parse(String(body['end']))){this.error.set('Prvi termin mora biti unutar perioda izložbe, sa krajem nakon početka.');return;}
        body['terms']=[{start,end,capacity:this.initial.capacity,status:'OTVOREN'}];
      }
      const saved=this.mode==='new'?await this.api.post<CollectionRecord>('/'+this.kind,body):await this.api.put<CollectionRecord>('/'+this.kind+'/'+this.id,body);
      if(this.mode==='new')await this.router.navigate(['/collection',this.kind,saved.id,'edit'],{queryParams:{saved:1}});
      else{this.original=saved;
        for(const f of this.formFields){const value=saved[f.key];this.model[f.key]=f.type==='datetime-local'&&typeof value==='string'?localTimestamp(value):(value??null) as Value;}
        this.notice.set('Izmene su sačuvane.');}
    });
  }
  remove(){if(!this.confirm()||this.busy())return;void this.run(async()=>{
    if(this.kind==='museums'){const body=Object.fromEntries(this.formFields.map(f=>[f.key,this.original?.[f.key]??null]));const row=await this.api.put<CollectionRecord>('/museums/'+this.id,{...body,active:false});this.original=row;this.model['active']=false;this.confirm.set(false);this.notice.set('Muzej je deaktiviran. Istorijski podaci su sačuvani.');}
    else{await this.api.delete('/'+this.kind+'/'+this.id);await this.router.navigate(['/collection',this.kind],{queryParams:{removed:1}});}
  });}
}
