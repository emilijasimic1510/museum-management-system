import { Component, inject, input, OnInit, signal } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { Api } from '../core/api';
import { Author } from '../core/catalog';
import { CuratorState, ManagedAuthorship } from '../core/curator';
import { Feedback } from '../shared/feedback';

@Component({selector:'app-curator-authorships',imports:[FormsModule,Feedback],template:`
<section class="panel"><h2>Autorstva</h2><p>Vezu određuju autor i eksponat. Uklanjanje veze ne briše autora ni eksponat.</p>
<app-feedback [busy]="busy()" [error]="error()" [notice]="notice()" />
@if(error()){<button class="button secondary" [disabled]="busy()" (click)="load()">Ponovo učitaj</button>}
@if(loaded()){
@for(a of links();track a.authorId){<article class="curator-relation"><h3>{{name(a.authorId)}}</h3><p>{{a.contributionType||'Vrsta doprinosa nije navedena'}}</p><p>{{a.note}}</p><div class="curator-actions"><button class="button secondary" [disabled]="busy()" (click)="edit(a)">Izmeni autorstvo</button><button class="button danger" [disabled]="busy()" (click)="pending.set(a.authorId)">Ukloni vezu</button></div>
@if(pending()===a.authorId){<p role="alert">Ukloniti ovo autorstvo?</p><button class="button danger" [disabled]="busy()" (click)="remove(a.authorId)">Potvrdi uklanjanje</button><button class="button secondary" [disabled]="busy()" (click)="pending.set(null)">Odustani</button>}</article>}
@empty{<p class="empty">Nijedan autor nije povezan sa eksponatom.</p>}
<h3>{{editing?'Izmena autorstva':'Poveži autora'}}</h3>
<form #form="ngForm" (ngSubmit)="save(form)"><fieldset [disabled]="busy()">
<label>Autor <span class="required-mark" aria-hidden="true">*</span><select name="authorId" [(ngModel)]="authorId" required [disabled]="editing"><option [ngValue]="null">Izaberite autora</option>@for(a of authors();track a.id){<option [ngValue]="a.id">{{a.firstName}} {{a.lastName}}</option>}</select></label>
<label>Vrsta doprinosa<input name="contributionType" [(ngModel)]="contributionType"></label>
<label>Interna napomena<textarea name="note" [(ngModel)]="note"></textarea></label>
@if(form.submitted&&!authorId){<p class="field-error">Izaberite autora.</p>}
<div class="curator-actions"><button class="button" type="submit" [disabled]="busy()">Sačuvaj autorstvo</button><button class="button secondary" type="button" (click)="reset()">Očisti formu</button></div>
</fieldset></form>}</section>
`})
export class CuratorAuthorships extends CuratorState implements OnInit {
  exhibitId=input.required<number>();private api=inject(Api);
  authors=signal<Author[]>([]);links=signal<ManagedAuthorship[]>([]);loaded=signal(false);pending=signal<number|null>(null);
  authorId:number|null=null;contributionType='';note='';editing=false;
  ngOnInit(){this.load();}
  name(id:number){const a=this.authors().find(a=>a.id===id);return a?a.firstName+' '+(a.lastName||''):'Autor';}
  load(){void this.run(async()=>{const [authors,links]=await Promise.all([this.api.get<Author[]>('/authors'),this.api.get<ManagedAuthorship[]>('/exhibits/'+this.exhibitId()+'/authorships/management')]);this.authors.set(authors);this.links.set(links);this.loaded.set(true);});}
  edit(a:ManagedAuthorship){this.authorId=a.authorId;this.contributionType=a.contributionType||'';this.note=a.note||'';this.editing=true;}
  reset(){this.authorId=null;this.contributionType='';this.note='';this.editing=false;}
  save(form:NgForm){if(form.invalid||!this.authorId)return;
    if(!this.editing&&this.links().some(a=>a.authorId===this.authorId)){this.error.set('Autor je već povezan. Koristite izmenu postojećeg autorstva.');return;}
    void this.run(async()=>{const a=await this.api.put<ManagedAuthorship>('/exhibits/'+this.exhibitId()+'/authorships/'+this.authorId,{contributionType:this.contributionType.trim(),note:this.note.trim()});this.links.update(rows=>[...rows.filter(r=>r.authorId!==a.authorId),a]);this.reset();this.notice.set('Autorstvo je sačuvano.');});
  }
  remove(id:number){if(this.pending()!==id)return;void this.run(async()=>{await this.api.delete('/exhibits/'+this.exhibitId()+'/authorships/'+id);this.links.update(rows=>rows.filter(a=>a.authorId!==id));this.pending.set(null);if(this.authorId===id)this.reset();this.notice.set('Autorstvo je uklonjeno.');});}
}
