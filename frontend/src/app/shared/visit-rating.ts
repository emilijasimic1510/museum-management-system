import { translate } from './../core/i18n';
import { Component, effect, inject, input, signal } from '@angular/core';
import { LocalDatePipe as DatePipe } from '../core/i18n';
import { HttpErrorResponse } from '@angular/common/http';
import { FormsModule, NgForm } from '@angular/forms';
import { Api, errorMessage } from '../core/api';
import { VisitRating } from '../core/models';
import { Feedback } from './feedback';
import { I18n, TranslatePipe } from '../core/i18n';

@Component({selector:'app-visit-rating',imports:[DatePipe,FormsModule,Feedback,TranslatePipe],template:`
<section [attr.aria-label]="'uiText.visit.rating'|t">
  <h3>{{'visitor.rating'|t}}</h3>
  <app-feedback [busy]="busy()" [error]="error()" [notice]="notice()" />
  @if(rating();as r){
    <p><strong>{{r.rating}} / 5</strong> · {{r.ratedAt|date:'dd.MM.yyyy. HH:mm'}}</p>
    @if(r.comment){<p class="description">{{r.comment}}</p>}
  }@else if(loaded()&&!busy()){
    @if(editable()){
      <form #form="ngForm" (ngSubmit)="save(form)">
        <label>{{'visitor.ratingChoose'|t}} <span class="required-mark" aria-hidden="true">*</span><select name="rating" [(ngModel)]="score" required #field="ngModel">
          <option [ngValue]="null">{{'visitor.ratingChoose'|t}}</option>
          @for(value of [1,2,3,4,5];track value){<option [ngValue]="value">{{value}}</option>}
        </select>@if(field.invalid&&(field.touched||form.submitted)){<small class="field-error">{{'visitor.ratingChoose'|t}} {{'uiText.from.1.to.5'|t}}</small>}</label>
        <label>{{'visitor.comment'|t}} {{'uiText.optional'|t}}<textarea name="comment" [(ngModel)]="comment" rows="4"></textarea></label>
        <button class="button" type="submit" [disabled]="form.invalid||busy()">{{'visitor.saveRating'|t}}</button>
      </form>
    }@else{<p class="empty-inline">{{'visitor.noRating'|t}}</p>}
  }
  @if(error()&&!busy()){<button class="text-button" type="button" (click)="load()">{{'uiText.check.saved.rating'|t}}</button>}
</section>`})
export class VisitRatingPanel {
  visitId=input.required<number>();editable=input(true);private api=inject(Api);private i18n=inject(I18n);private revision=0;
  rating=signal<VisitRating|null>(null);loaded=signal(false);busy=signal(false);error=signal('');notice=signal('');
  score:number|null=null;comment='';
  constructor(){effect(()=>{this.visitId();this.score=null;this.comment='';this.notice.set('');void this.load();});}
  async load(){
    const id=this.visitId(),revision=++this.revision;
    this.busy.set(true);this.loaded.set(false);this.error.set('');this.rating.set(null);
    try{const r=await this.api.get<VisitRating>(`/visits/${id}/rating`,10000);if(revision===this.revision){this.rating.set(r);this.loaded.set(true);}}
    catch(e){if(revision!==this.revision)return;if(e instanceof HttpErrorResponse&&e.status===404)this.loaded.set(true);else this.error.set(errorMessage(e));}
    finally{if(revision===this.revision)this.busy.set(false);}
  }
  async save(form:NgForm){
    if(form.invalid||this.busy()||!this.loaded()||this.rating()||!this.editable()||this.score===null||!Number.isInteger(this.score)||this.score<1||this.score>5)return;
    const id=this.visitId(),revision=++this.revision;this.busy.set(true);this.error.set('');this.notice.set('');
    try{const r=await this.api.post<VisitRating>(`/visits/${id}/rating`,{rating:this.score,comment:this.comment.trim()||null},10000);
      if(revision===this.revision){this.rating.set(r);this.notice.set(this.i18n.t('visitor.ratingSaved'));}
    }catch(e){if(revision!==this.revision)return;
      // Reconcile before allowing another POST, including an uncertain network outcome.
      this.loaded.set(false);
      this.error.set(e instanceof HttpErrorResponse&&e.status===409
        ?translate("uiText.this.visit.is.already.rated.or.has.not.finished.check.the.saved.rating.and.ref")
        :errorMessage(e));
    }finally{if(revision===this.revision)this.busy.set(false);}
  }
}
