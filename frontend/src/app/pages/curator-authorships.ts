import { translate } from './../core/i18n';
import { Component, inject, input, OnInit, signal } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { Api } from '../core/api';
import { Author } from '../core/catalog';
import { CuratorState, ManagedAuthorship } from '../core/curator';
import { Feedback } from '../shared/feedback';
import { TranslatePipe } from '../core/i18n';

@Component({selector:'app-curator-authorships',imports:[FormsModule,Feedback,TranslatePipe],template:`
<section class="panel"><h2>{{'uiText.authorships'|t}}</h2><p>{{'uiText.an.authorship.links.an.author.to.an.exhibit.removing.the.link.does.not.delete.'|t}}</p>
<app-feedback [busy]="busy()" [error]="error()" [notice]="notice()" />
@if(error()){<button class="button secondary" [disabled]="busy()" (click)="load()">{{'qrBoard.retry'|t}}</button>}
@if(loaded()){
@for(a of links();track a.authorId){<article class="curator-relation"><h3>{{name(a.authorId)}}</h3><p>{{a.contributionType||('uiText.contribution.type.not.specified'|t)}}</p><p>{{a.note}}</p><div class="curator-actions"><button class="button secondary" [disabled]="busy()" (click)="edit(a)">{{'uiText.edit.authorship'|t}}</button><button class="button danger" [disabled]="busy()" (click)="pending.set(a.authorId)">{{'uiText.remove.link'|t}}</button></div>
@if(pending()===a.authorId){<p role="alert">{{'uiText.remove.this.authorship'|t}}</p><button class="button danger" [disabled]="busy()" (click)="remove(a.authorId)">{{'uiText.confirm.removal'|t}}</button><button class="button secondary" [disabled]="busy()" (click)="pending.set(null)">{{'uiText.cancel'|t}}</button>}</article>}
@empty{<p class="empty">{{'uiText.no.authors.are.linked.to.this.exhibit'|t}}</p>}
<h3>{{editing?('uiText.edit.authorship.2'|t):('uiText.link.author'|t)}}</h3>
<form #form="ngForm" (ngSubmit)="save(form)"><fieldset [disabled]="busy()">
<label>{{'uiText.author.2'|t}} <span class="required-mark" aria-hidden="true">*</span><select name="authorId" [(ngModel)]="authorId" required [disabled]="editing"><option [ngValue]="null">{{'uiText.choose.an.author'|t}}</option>@for(a of authors();track a.id){<option [ngValue]="a.id">{{a.firstName}} {{a.lastName}}</option>}</select></label>
<label>{{'uiText.contribution.type'|t}}<input name="contributionType" [(ngModel)]="contributionType"></label>
<label>{{'uiText.internal.note'|t}}<textarea name="note" [(ngModel)]="note"></textarea></label>
@if(form.submitted&&!authorId){<p class="field-error">{{'uiText.choose.an.author.2'|t}}</p>}
<div class="curator-actions"><button class="button" type="submit" [disabled]="busy()">{{'uiText.save.authorship'|t}}</button><button class="button secondary" type="button" (click)="reset()">{{'uiText.clear.form'|t}}</button></div>
</fieldset></form>}</section>
`})
export class CuratorAuthorships extends CuratorState implements OnInit {
  exhibitId=input.required<number>();private api=inject(Api);
  authors=signal<Author[]>([]);links=signal<ManagedAuthorship[]>([]);loaded=signal(false);pending=signal<number|null>(null);
  authorId:number|null=null;contributionType='';note='';editing=false;
  ngOnInit(){this.load();}
  name(id:number){const a=this.authors().find(a=>a.id===id);return a?a.firstName+' '+(a.lastName||''):translate("uiText.author.2");}
  load(){void this.run(async()=>{const [authors,links]=await Promise.all([this.api.get<Author[]>('/authors'),this.api.get<ManagedAuthorship[]>('/exhibits/'+this.exhibitId()+'/authorships/management')]);this.authors.set(authors);this.links.set(links);this.loaded.set(true);});}
  edit(a:ManagedAuthorship){this.authorId=a.authorId;this.contributionType=a.contributionType||'';this.note=a.note||'';this.editing=true;}
  reset(){this.authorId=null;this.contributionType='';this.note='';this.editing=false;}
  save(form:NgForm){if(form.invalid||!this.authorId)return;
    if(!this.editing&&this.links().some(a=>a.authorId===this.authorId)){this.error.set(translate("uiText.this.author.is.already.linked.edit.the.existing.authorship"));return;}
    void this.run(async()=>{const a=await this.api.put<ManagedAuthorship>('/exhibits/'+this.exhibitId()+'/authorships/'+this.authorId,{contributionType:this.contributionType.trim(),note:this.note.trim()});this.links.update(rows=>[...rows.filter(r=>r.authorId!==a.authorId),a]);this.reset();this.notice.set(translate("uiText.authorship.saved"));});
  }
  remove(id:number){if(this.pending()!==id)return;void this.run(async()=>{await this.api.delete('/exhibits/'+this.exhibitId()+'/authorships/'+id);this.links.update(rows=>rows.filter(a=>a.authorId!==id));this.pending.set(null);if(this.authorId===id)this.reset();this.notice.set(translate("uiText.authorship.removed"));});}
}
