import { Component, inject, signal } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Api } from '../core/api';
import { Author, CatalogExhibit, validCatalogId } from '../core/catalog';
import { CuratorState, localDate, ManagedParticipation } from '../core/curator';
import { Exhibition } from '../core/models';
import { Feedback } from '../shared/feedback';
import { CuratorAuthorships } from './curator-authorships';
import { ParticipationQr } from '../shared/participation-qr';

interface Field {key:string;label:string;required?:boolean;type?:string;min?:number}
@Component({imports:[FormsModule,RouterLink,Feedback,CuratorAuthorships,ParticipationQr],template:`
<section class="page-heading"><p class="eyebrow">UPRAVLJANJE KATALOGOM</p><h1>{{id?'Izmena':'Novi zapis'}} · {{title}}</h1><a [routerLink]="base">← Nazad na listu</a></section>
<app-feedback [busy]="busy()" [error]="error()" [notice]="notice()" />
@if(!loaded()&&error()){<button class="button" [disabled]="busy()" (click)="load()">Pokušaj ponovo</button>}
@if(loaded()){<section class="panel"><form #form="ngForm" (ngSubmit)="save(form)"><fieldset [disabled]="busy()">
@if(kind==='participations'){<div class="form-row"><label>Izložba *<select name="exhibitionId" [(ngModel)]="model['exhibitionId']" required [disabled]="!!id"><option [ngValue]="null">Izaberite izložbu</option>@for(e of exhibitions();track e.id){<option [ngValue]="e.id">{{e.name}}</option>}</select></label>
<label>Eksponat *<select name="exhibitId" [(ngModel)]="model['exhibitId']" required [disabled]="!!id"><option [ngValue]="null">Izaberite eksponat</option>@for(e of exhibits();track e.id){<option [ngValue]="e.id">{{e.name}}</option>}</select></label></div>
@if(id){<p class="muted">Izložba i eksponat postojećeg učešća se ne menjaju zbog istorije skeniranja. Za drugu kombinaciju kreirajte novo učešće.</p>}}
<div class="curator-form-grid">@for(field of fields;track field.key){<label [class.full-width]="field.type==='textarea'">{{field.label}}{{field.required?' *':''}}
@if(field.type==='textarea'){<textarea [name]="field.key" [(ngModel)]="model[field.key]" [required]="!!field.required" rows="4"></textarea>}
@else{<input [name]="field.key" [type]="field.type||'text'" [(ngModel)]="model[field.key]" [required]="!!field.required" [attr.min]="field.min??null" [min]="field.min??null" [step]="field.type==='number'?1:null">}
</label>}</div>
@if(form.submitted&&form.invalid){<p class="field-error">Popunite obavezna polja i proverite brojeve.</p>}
<div class="curator-actions"><button class="button" type="submit" [disabled]="busy()">{{busy()?'Čuvanje…':'Sačuvaj'}}</button><a class="button secondary" [routerLink]="base">Nazad na listu</a></div>
</fieldset></form></section>
@if(participation();as p){<section class="panel"><h2>QR podatak učešća</h2><p>Token generiše server i pripada ovom učešću. Ne menja se pri uređivanju.</p><app-participation-qr [token]="p.qrToken" /><label>QR token<input readonly [value]="p.qrToken"></label><button class="button secondary" (click)="copyToken()">Kopiraj</button><p role="status">{{copyStatus()}}</p><p class="muted">Skeniranje ostaje dostupno posetiocu isključivo tokom aktivne posete.</p></section>}
@if(kind==='exhibits'&&id){<app-curator-authorships [exhibitId]="id" />}
}
`})
export class CuratorEditor extends CuratorState {
  private api=inject(Api);private route=inject(ActivatedRoute);private router=inject(Router);
  kind=this.route.snapshot.data['kind'] as string;id=Number(this.route.snapshot.paramMap.get('id'))||null;
  base='/curator/'+this.kind;title=this.kind==='exhibits'?'Eksponat':this.kind==='authors'?'Autor':'Učešće';
  endpoint='/'+(this.kind==='participations'?'exhibit-participations':this.kind);
  fields:Field[]=this.kind==='exhibits'?[
    {key:'name',label:'Naziv',required:true},{key:'inventoryNumber',label:'Inventarski broj',required:true},
    {key:'creationPeriod',label:'Period / godina nastanka'},{key:'type',label:'Tip'},{key:'material',label:'Materijal'},{key:'origin',label:'Poreklo'},{key:'description',label:'Opis',type:'textarea'}
  ]:this.kind==='authors'?[
    {key:'firstName',label:'Ime',required:true},{key:'lastName',label:'Prezime'},{key:'birthYear',label:'Godina rođenja',type:'number'},{key:'deathYear',label:'Godina smrti',type:'number'},{key:'biography',label:'Biografija',type:'textarea'}
  ]:[
    {key:'installedAt',label:'Datum postavljanja',type:'datetime-local'},{key:'removedAt',label:'Datum uklanjanja',type:'datetime-local'},{key:'position',label:'Pozicija'},{key:'tourOrder',label:'Redosled obilaska',type:'number',min:1},{key:'contextDescription',label:'Kontekstualni opis',type:'textarea'}
  ];
  model:Record<string,string|number|null>={};loaded=signal(false);exhibits=signal<CatalogExhibit[]>([]);exhibitions=signal<Exhibition[]>([]);
  participation=signal<ManagedParticipation|null>(null);copyStatus=signal('');
  constructor(){super();for(const f of this.fields)this.model[f.key]=f.type==='number'?null:'';this.model['exhibitId']=null;this.model['exhibitionId']=null;this.load();}
  load(){void this.run(async()=>{
    const rawId=this.route.snapshot.paramMap.get('id');
    if(rawId&&!validCatalogId(rawId)){this.error.set('Traženi zapis nije pronađen.');return;}
    if(this.kind==='participations'){const [e,x]=await Promise.all([this.api.get<CatalogExhibit[]>('/exhibits'),this.api.get<Exhibition[]>('/exhibitions')]);this.exhibits.set(e);this.exhibitions.set(x);}
    if(this.id){
      if(this.kind==='exhibits'){const r=await this.api.get<{exhibit:CatalogExhibit;inventoryNumber:string}>(this.endpoint+'/'+this.id+'/management');this.model={...r.exhibit,inventoryNumber:r.inventoryNumber} as unknown as typeof this.model;}
      else if(this.kind==='authors')this.model=await this.api.get<Author>(this.endpoint+'/'+this.id) as unknown as typeof this.model;
      else{const p=await this.api.get<ManagedParticipation>(this.endpoint+'/'+this.id);this.participation.set(p);this.model={exhibitId:p.exhibitId,exhibitionId:p.exhibitionId,installedAt:localDate(p.installedAt),removedAt:localDate(p.removedAt),position:p.position,tourOrder:p.tourOrder,contextDescription:p.contextDescription};}
    }
    this.loaded.set(true);
    if(this.route.snapshot.queryParamMap.has('saved'))this.notice.set('Zapis je sačuvan.');
  });}
  save(form:NgForm){
    if(form.invalid)return;
    if(this.fields.some(f=>f.required&&!String(this.model[f.key]??'').trim())){this.error.set('Obavezna polja ne mogu biti prazna.');return;}
    const body:Record<string,unknown>={};
    for(const f of this.fields){const v=this.model[f.key];body[f.key]=typeof v==='string'?v.trim():v;}
    if(this.kind==='authors'&&body['birthYear']!=null&&body['deathYear']!=null&&Number(body['deathYear'])<Number(body['birthYear'])){this.error.set('Godina smrti ne može prethoditi godini rođenja.');return;}
    if(this.kind==='participations'){
      body['exhibitId']=this.model['exhibitId'];body['exhibitionId']=this.model['exhibitionId'];
      if(!body['exhibitId']||!body['exhibitionId']){this.error.set('Izaberite izložbu i eksponat.');return;}
      for(const key of ['installedAt','removedAt']){
        const value=this.model[key];
        if(value&&!Number.isFinite(Date.parse(String(value)))){this.error.set('Unesite ispravan datum i vreme.');return;}
        body[key]=value?new Date(String(value)).toISOString():null;
      }
      if(body['installedAt']&&body['removedAt']&&String(body['removedAt'])<=String(body['installedAt'])){this.error.set('Uklanjanje mora biti nakon postavljanja.');return;}
      // Omit qrToken: backend generates on create and preserves on update.
    }
    void this.run(async()=>{
      const creating=!this.id;
      type Saved=Author|ManagedParticipation|{exhibit:CatalogExhibit;inventoryNumber:string};
      const r=await (this.id?this.api.put<Saved>(this.endpoint+'/'+this.id,body):this.api.post<Saved>(this.endpoint,body));
      this.id='exhibit' in r?r.exhibit.id:r.id;
      if(this.kind==='participations')this.participation.set(r as ManagedParticipation);
      this.notice.set('Zapis je sačuvan. Možete nastaviti uređivanje ili se vratiti na listu.');
      if(creating)await this.router.navigate([this.base,this.id,'edit'],{queryParams:{saved:1},replaceUrl:true});
    });
  }
  async copyToken(){try{await navigator.clipboard.writeText(this.participation()?.qrToken||'');this.copyStatus.set('Token je kopiran.');}catch{this.copyStatus.set('Kopiranje nije dostupno. Označite tekst i kopirajte ga ručno.');}}
}
