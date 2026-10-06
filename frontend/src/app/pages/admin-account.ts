import {UiLabelPipe} from '../core/i18n';
import { TranslatePipe, translate } from './../core/i18n';
import { Component, inject, signal } from '@angular/core';
import { LocalDatePipe as DatePipe } from '../core/i18n';
import { FormsModule, NgForm } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Api } from '../core/api';
import { Auth } from '../core/auth';
import { Account, Registration } from '../core/models';
import { AdminRole, AdminState, adminStyles } from '../core/admin';
import { Feedback } from '../shared/feedback';

@Component({imports:[UiLabelPipe,TranslatePipe,FormsModule,DatePipe,RouterLink,Feedback],styles:[adminStyles],template:`
<a routerLink="/admin/accounts">{{'uiText.user.accounts'|t}}</a><section class="page-heading"><h1>{{creating?('uiText.create.account'|t):editing?('uiText.edit.account'|t):('uiText.account.details'|t)}}</h1></section>
<app-feedback [busy]="busy()" [error]="error()" [notice]="notice()" />
@if(!creating){<button class="text-button" [disabled]="busy()" (click)="load()">{{'qrBoard.retry'|t}}</button>}
@if(creating){
 <form #form="ngForm" class="panel" (ngSubmit)="create(form)"><p>{{'uiText.a.new.account.starts.active.with.the.visitor.role.and.profile.assign.the.appro'|t}}</p>
 <fieldset class="admin-fields" [disabled]="busy()"><div class="admin-grid">
 <label>{{'login.email'|t}} <span class="required-mark" aria-hidden="true">*</span><input type="email" name="email" [(ngModel)]="draft.email" required email autocomplete="off"></label>
 <label>{{'uiText.initial.password'|t}} <span class="required-mark" aria-hidden="true">*</span><input type="password" name="password" [(ngModel)]="draft.password" required minlength="12" maxlength="72" autocomplete="new-password"><small>{{'uiText.at.least.12.characters.at.most.72.utf.8.bytes'|t}}</small></label>
 <label>{{'login.firstName'|t}} <span class="required-mark" aria-hidden="true">*</span><input name="firstName" [(ngModel)]="draft.firstName" required></label><label>{{'login.lastName'|t}} <span class="required-mark" aria-hidden="true">*</span><input name="lastName" [(ngModel)]="draft.lastName" required></label><label>{{'uiText.phone.optional'|t}}<input type="tel" name="phone" [(ngModel)]="draft.phone"></label>
 </div><button class="button" [disabled]="form.invalid||busy()">{{'login.createSubmit'|t}}</button></fieldset></form>
}
@if(account();as a){
 <section class="panel admin-break"><h2>{{a.email}}</h2><p>{{'uiText.account'|t}}{{a.id}} · {{a.active?('uiText.active.2'|t):('uiText.inactive.2'|t)}}</p><p>{{'uiText.registered'|t}} {{a.registeredAt|date:'dd.MM.yyyy. HH:mm'}} {{'uiText.last.sign.in'|t}} {{a.lastLogin?(a.lastLogin|date:'dd.MM.yyyy. HH:mm'):('uiText.not.recorded'|t)}}</p>
 @if(editing){<form #editForm="ngForm" (ngSubmit)="saveEmail(editForm)"><label>{{'login.email'|t}} <span class="required-mark" aria-hidden="true">*</span><input type="email" name="email" [(ngModel)]="email" required email [disabled]="busy()"></label><button class="button" [disabled]="editForm.invalid||busy()">{{'uiText.save.email'|t}}</button></form>}
 @else{<a [routerLink]="['/admin/accounts',a.id,'edit']">{{'uiText.edit.email'|t}}</a>}
 <div class="admin-actions"><button class="button secondary" [class.danger]="a.active" [disabled]="busy()" (click)="confirm.set('status')">{{a.active?('uiText.deactivate.account'|t):('uiText.activate.account'|t)}}</button><a routerLink="/admin/employees/new" [queryParams]="{accountId:a.id}">{{'uiText.create.employee.profile'|t}}</a></div>
 <p>{{'uiText.deactivation.preserves.business.history.and.blocks.account.access.account.dele'|t}}</p></section>
 <section id="roles" class="panel"><h2>{{'uiText.assigned.roles'|t}}</h2><p>{{'uiText.an.account.must.retain.at.least.one.role.removing.a.role.does.not.delete.the.e'|t}}</p>
 @for(name of a.roles;track name){<div class="admin-actions"><strong class="badge">{{name|uiLabel:'role'}}</strong><button type="button" class="button danger" [disabled]="busy()||a.roles.length<=1" (click)="askRevoke(name)">{{'uiText.remove'|t}} {{name|uiLabel:'role'}}</button></div>}
 @if(availableRoles().length){<form #roleForm="ngForm" (ngSubmit)="grant(roleForm)"><label>{{'uiText.existing.role'|t}} <span class="required-mark" aria-hidden="true">*</span><select name="roleId" [(ngModel)]="roleId" required [disabled]="busy()"><option [ngValue]="null">{{'uiText.choose.a.role'|t}}</option>@for(r of availableRoles();track r.id){<option [ngValue]="r.id">{{r.name|uiLabel:'role'}}</option>}</select></label><button class="button" [disabled]="roleForm.invalid||busy()">{{'uiText.assign.role'|t}}</button></form>}
 @else{<p>{{'uiText.all.existing.roles.are.assigned'|t}}</p>}
 </section>
 @if(confirm()){<section class="admin-confirm" role="alert"><h2>{{'uiText.confirm.change'|t}}</h2><p>{{confirm()==='status'?(a.active?('uiText.deactivate.this.account.active.sessions.will.lose.access'|t):('uiText.activate.this.account'|t)):('uiText.remove.role'|t)+' '+revokeRole()?.name+' '+('uiText.from.this.account'|t)}}</p>@if(isSelf()){<p>{{'uiText.you.are.changing.your.own.account.you.may.lose.access.to.this.workspace'|t}}</p>}<div class="admin-actions"><button class="button" [disabled]="busy()" (click)="applyConfirmation()">{{'uiText.confirm'|t}}</button><button class="button secondary" [disabled]="busy()" (click)="confirm.set(null)">{{'uiText.cancel'|t}}</button></div></section>}
}
`})
export class AdminAccount extends AdminState {
  private api=inject(Api);private auth=inject(Auth);private router=inject(Router);private route=inject(ActivatedRoute);
  creating=this.route.snapshot.data['mode']==='new';editing=this.route.snapshot.data['mode']==='edit';id=Number(this.route.snapshot.paramMap.get('id'));
  account=signal<Account|null>(null);roles=signal<AdminRole[]>([]);email='';roleId:number|null=null;confirm=signal<'status'|'role'|null>(null);revokeRole=signal<AdminRole|null>(null);
  draft:Registration={email:'',password:'',firstName:'',lastName:'',phone:'',recommendationsConsent:false};
  constructor(){super();if(!this.creating)this.load();}
  isSelf(){return this.auth.currentAccount()?.id===this.id;}
  availableRoles(){return this.roles().filter(r=>!this.account()?.roles.includes(r.name));}
  load(){void this.run(async()=>{this.account.set(null);this.confirm.set(null);if(!Number.isSafeInteger(this.id)||this.id<=0){this.error.set(translate("uiText.invalid.account.id"));return;}const [a,r]=await Promise.all([this.api.get<Account>('/admin/accounts/'+this.id,10000),this.api.get<AdminRole[]>('/admin/roles',10000)]);this.account.set(a);this.email=a.email;this.roles.set(r);if(this.route.snapshot.queryParamMap.has('created'))this.notice.set(translate("uiText.account.created.assign.roles.as.needed"));});}
  create(form:NgForm){if(form.invalid||this.busy())return;void this.run(async()=>{
    if(new TextEncoder().encode(this.draft.password).length>72){this.error.set(translate("uiText.the.password.must.not.exceed.72.utf.8.bytes"));return;}
    const a=await this.api.post<Account>('/admin/accounts',{...this.draft,email:this.draft.email.trim(),firstName:this.draft.firstName.trim(),lastName:this.draft.lastName.trim(),recommendationsConsent:false});this.draft.password='';await this.router.navigate(['/admin/accounts',a.id],{queryParams:{created:1}});
  });}
  saveEmail(form:NgForm){if(form.invalid||this.busy()||!this.account())return;void this.run(async()=>{const a=await this.api.put<Account>('/admin/accounts/'+this.id,{email:this.email.trim(),active:this.account()!.active});this.sync(a);this.notice.set(translate("uiText.email.saved"));});}
  grant(form:NgForm){if(form.invalid||this.busy()||!this.roleId||!this.availableRoles().some(r=>r.id===this.roleId))return;void this.run(async()=>{const a=await this.api.post<Account>(`/admin/accounts/${this.id}/roles/${this.roleId}`,{});this.sync(a);this.roleId=null;this.notice.set(translate("uiText.role.assigned"));});}
  askRevoke(name:string){const r=this.roles().find(r=>r.name===name);if(!r||!this.account()||this.account()!.roles.length<=1)return;this.revokeRole.set(r);this.confirm.set('role');}
  applyConfirmation(){if(this.busy()||!this.account()||!this.confirm())return;void this.run(async()=>{
    if(this.confirm()==='status'){const a=await this.api.put<Account>('/admin/accounts/'+this.id,{email:this.account()!.email,active:!this.account()!.active});this.sync(a);this.notice.set(a.active?translate("uiText.account.activated"):translate("uiText.account.deactivated.history.retained"));}
    else{const role=this.revokeRole();if(!role||this.account()!.roles.length<=1)return;await this.api.delete(`/admin/accounts/${this.id}/roles/${role.id}`);
      if(this.isSelf()&&role.name==='ADMINISTRATOR'){this.auth.currentAccount.set({...this.account()!,roles:this.account()!.roles.filter(r=>r!==role.name)});await this.router.navigateByUrl('/forbidden');}
      else this.sync(await this.api.get<Account>('/admin/accounts/'+this.id,10000));this.notice.set(translate("uiText.role.removed"));}
    this.confirm.set(null);
  });}
  private sync(a:Account){this.account.set(a);this.email=a.email;if(this.isSelf()){this.auth.currentAccount.set(a);if(!a.active){this.auth.clear();void this.router.navigateByUrl('/login');}}}
}
