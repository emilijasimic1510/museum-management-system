import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Api } from '../core/api';
import { CatalogExhibit, validCatalogId } from '../core/catalog';
import { CuratorState, DigitalContent, ManagedParticipation, validReference } from '../core/curator';
import { Exhibition } from '../core/models';
import { Feedback } from '../shared/feedback';

@Component({imports:[FormsModule,RouterLink,Feedback,DatePipe],template:`
<section class="page-heading"><p class="eyebrow">KUSTOSKI PROSTOR</p><h1>{{mode==='new'?'Dodaj digitalni sadržaj':mode==='edit'?'Izmeni digitalni sadržaj':'Digitalni sadržaj'}}</h1><p>Sadržaj pripada konkretnom učešću eksponata na izložbi.</p>
@if(mode){<a routerLink="/curator/digital-content" [queryParams]="{participationId:participationId}">← Nazad na sadržaj</a>}@else{<a class="button" routerLink="/curator/digital-content/new" [queryParams]="{participationId:participationId}">Dodaj digitalni sadržaj</a>}</section>
<app-feedback [busy]="busy()" [error]="error()" [notice]="notice()" />
@if(error()){<button class="button secondary" [disabled]="busy()" (click)="retry()">Pokušaj ponovo</button>}
@if(loaded()){<label>Učešće<select aria-label="Učešće" [ngModel]="participationId" (ngModelChange)="select($event)" [disabled]="busy()||mode==='edit'"><option [ngValue]="null">Izaberite učešće</option>@for(p of parts();track p.id){<option [ngValue]="p.id">{{label(p)}}</option>}</select></label>}
@if(mode==='edit'&&loaded()){<p class="muted">Sadržaj se ne premešta na drugo učešće. Za drugo učešće kreirajte novi sadržaj.</p>}
@if(loaded()&&!parts().length){<p class="empty">Prvo kreirajte učešće eksponata na izložbi.</p>}
@if(!mode&&participationId&&contentLoaded()){
<section class="panel audio-generation"><h2>Audio vodič</h2><label>Jezik<select [(ngModel)]="audioLanguage" [disabled]="busy()"><option value="sr">Srpski</option></select></label>
@if(!hasContext()){<p>Prvo unesite i sačuvajte kontekstualni opis ove postavke.</p>}
<button class="button" [disabled]="busy()||!hasContext()" (click)="generateAudio()">{{busy()?'Obrada…':'Generiši audio'}}</button></section>
<div class="cards">@for(c of content();track c.id){<article class="panel"><h2>{{c.type}} · verzija {{c.version}}</h2><p class="curator-reference">{{c.path}}</p><p>Generisano: {{c.createdAt|date:'dd.MM.yyyy. HH:mm'}} · {{c.automaticallyGenerated?'Automatski generisan sadržaj':'Ručno dodat sadržaj'}}</p><p>Jezik: {{c.language||'Nije naveden'}}</p><div class="curator-actions">@if(c.path){<a class="button secondary" [routerLink]="['/curator/digital-content',c.id,'edit']">Izmeni</a>}<button class="button danger" [disabled]="busy()" (click)="pending.set(c.id)">Obriši</button></div>
@if(pending()===c.id){<p role="alert">Obrisati ovu referencu sadržaja? Radnja se ne može opozvati.</p><button class="button danger" [disabled]="busy()" (click)="remove(c.id)">Potvrdi brisanje</button><button class="button secondary" [disabled]="busy()" (click)="pending.set(null)">Odustani</button>}</article>}@empty{<p class="empty">Ovo učešće još nema digitalni sadržaj.</p>}</div>
}
@if(mode&&loaded()&&(mode==='new'||contentLoaded())){
<section class="panel"><h2>{{editId?'Izmeni sadržaj':'Dodaj sadržaj'}}</h2><p>Unosi se referenca ka postojećem resursu. Upload i provera dostupnosti udaljenog resursa nisu uvedeni. TEKST takođe koristi referencu; model nema posebno polje za unos teksta.</p>
<form #form="ngForm" (ngSubmit)="save(form)"><fieldset [disabled]="busy()"><div class="curator-form-grid">
<label>Tip *<select name="type" [(ngModel)]="type" required><option>TEKST</option><option>SLIKA</option><option>AUDIO</option></select></label>
<label>Jezik<input name="language" [(ngModel)]="language" placeholder="sr"></label>
<label class="full-width">Putanja ili URL sadržaja *<input name="path" [(ngModel)]="path" required placeholder="demo/grad-001.txt"><small>Relativna putanja ili HTTP(S) URL bez pristupnih podataka. Unesite resurs namenjen posetiocima tokom aktivne posete.</small></label>
<label>Verzija *<input name="version" type="number" [(ngModel)]="version" required min="1" step="1"></label>
<label class="checkbox"><input type="checkbox" name="generated" [(ngModel)]="generated">Automatski generisan sadržaj</label></div>
@if(form.submitted&&form.invalid){<p class="field-error">Proverite obavezna polja i verziju.</p>}
<div class="curator-actions"><button class="button" [disabled]="busy()||!participationId" type="submit">Sačuvaj sadržaj</button>@if(mode==='new'){<button class="button secondary" type="button" (click)="reset()">Očisti formu</button>}</div>
</fieldset></form></section>}
`})
export class CuratorContent extends CuratorState {
  private route=inject(ActivatedRoute);private router=inject(Router);mode=this.route.snapshot.data['contentMode'] as 'new'|'edit'|undefined;
  private api=inject(Api);parts=signal<ManagedParticipation[]>([]);exhibits=signal<CatalogExhibit[]>([]);exhibitions=signal<Exhibition[]>([]);
  content=signal<DigitalContent[]>([]);loaded=signal(false);contentLoaded=signal(false);pending=signal<number|null>(null);
  participationId:number|null=null;editId:number|null=null;type:DigitalContent['type']='TEKST';language='sr';path='';version:number|null=1;generated=false;
  audioLanguage='sr';
  hasContext(){return !!this.parts().find(p=>p.id===this.participationId)?.contextDescription?.trim();}
  generateAudio(){if(this.busy()||!this.participationId||!this.hasContext())return;
    void this.run(async()=>{try{
      const created=await this.api.post<DigitalContent>(`/curator/participations/${this.participationId}/audio`,{language:this.audioLanguage});
      this.content.update(rows=>[created,...rows]);this.notice.set('Audio vodič je generisan. Verzija '+created.version+'.');
    }catch(e){if(e instanceof HttpErrorResponse&&e.error?.code==='LOCAL_AUDIO_ERROR')this.error.set(e.error.detail);else throw e;}});
  }
  constructor(){super();this.load();}
  label(p:ManagedParticipation){return (this.exhibits().find(e=>e.id===p.exhibitId)?.name||'Eksponat')+' — '+(this.exhibitions().find(e=>e.id===p.exhibitionId)?.name||'Izložba')+' · učešće '+p.id;}
  load(){void this.run(async()=>{
    const rawId=this.route.snapshot.paramMap.get('id');
    if(this.mode==='edit'&&(!rawId||!validCatalogId(rawId))){this.error.set('Sadržaj nije pronađen.');return;}
    const [p,e,x,c]=await Promise.all([this.api.get<ManagedParticipation[]>('/exhibit-participations'),this.api.get<CatalogExhibit[]>('/exhibits'),this.api.get<Exhibition[]>('/exhibitions'),this.mode==='edit'?this.api.get<DigitalContent>('/digital-content/'+rawId):Promise.resolve(null)]);
    this.parts.set(p);this.exhibits.set(e);this.exhibitions.set(x);
    if(c){this.participationId=c.participationId;this.edit(c);this.contentLoaded.set(true);}
    else{const selected=Number(this.route.snapshot.queryParamMap.get('participationId'));this.participationId=p.some(row=>row.id===selected)?selected:null;}
    if(!this.mode&&this.participationId){this.content.set(await this.api.get<DigitalContent[]>('/exhibit-participations/'+this.participationId+'/digital-content'));this.contentLoaded.set(true);}
    this.loaded.set(true);
    if(this.route.snapshot.queryParamMap.has('saved'))this.notice.set('Digitalni sadržaj je sačuvan.');
  });}
  retry(){if(!this.loaded()||this.mode)this.load();else this.select(this.participationId);}
  select(id:number|null){if(this.busy()||this.mode==='edit')return;this.participationId=id;this.content.set([]);this.contentLoaded.set(false);this.pending.set(null);if(!this.mode){this.reset();if(id)void this.run(async()=>{this.content.set(await this.api.get<DigitalContent[]>('/exhibit-participations/'+id+'/digital-content'));this.contentLoaded.set(true);});}}
  edit(c:DigitalContent){this.editId=c.id;this.type=c.type;this.language=c.language||'';this.path=c.path;this.version=c.version;this.generated=c.automaticallyGenerated;}
  reset(){this.editId=null;this.type='TEKST';this.language='sr';this.path='';this.version=1;this.generated=false;}
  save(form:NgForm){
    if(form.invalid||!this.participationId||!this.mode)return;
    if(!validReference(this.path)||!Number.isInteger(this.version)||Number(this.version)<1){this.error.set('Unesite bezbedan HTTP(S) URL ili relativnu putanju i pozitivan ceo broj verzije.');return;}
    void this.run(async()=>{const body={participationId:this.participationId,type:this.type,path:this.path.trim(),language:this.language.trim(),version:this.version,automaticallyGenerated:this.generated};
      const c=await (this.editId?this.api.put<DigitalContent>('/digital-content/'+this.editId,body):this.api.post<DigitalContent>('/digital-content',body));
      const creating=!this.editId;this.edit(c);this.notice.set('Digitalni sadržaj je sačuvan.');
      if(creating)await this.router.navigate(['/curator/digital-content',c.id,'edit'],{queryParams:{saved:1},replaceUrl:true});
    });
  }
  remove(id:number){if(this.pending()!==id)return;void this.run(async()=>{await this.api.delete('/digital-content/'+id);this.content.update(rows=>rows.filter(c=>c.id!==id));this.pending.set(null);if(this.editId===id)this.reset();this.notice.set('Sadržaj je obrisan.');});}
}
