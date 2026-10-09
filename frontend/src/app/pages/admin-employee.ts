import { LocalizedContentPipe } from '../core/i18n';
import {UiLabelPipe} from '../core/i18n';
import { TranslatePipe, translate } from './../core/i18n';
import { Component, inject, signal } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Api } from '../core/api';
import { Account, Museum } from '../core/models';
import { AdminState, Employee, EmployeeType, adminStyles, subtypeFields } from '../core/admin';
import { Feedback } from '../shared/feedback';

@Component({imports:[LocalizedContentPipe,UiLabelPipe,TranslatePipe,FormsModule,RouterLink,Feedback],styles:[adminStyles],template:`
<a routerLink="/admin/employees">{{'uiText.employees'|t}}</a><section class="page-heading"><h1>{{creating?('uiText.new.employee'|t):('uiText.employee.profile'|t)}}</h1><p>{{'uiText.each.employee.has.exactly.one.business.subtype.this.form.does.not.assign.syste'|t}}</p></section>
<app-feedback [busy]="busy()" [error]="error()" [notice]="notice()" /><button class="text-button" [disabled]="busy()" (click)="load()">{{'qrBoard.retry'|t}}</button>
@if(loaded()){
 @if(!availableAccounts().length&&creating){<p class="empty">{{'uiText.no.accounts.without.an.employee.profile.are.available'|t}} <a routerLink="/admin/accounts/new">{{'login.create'|t}}</a>.</p>}
 <form #form="ngForm" class="panel" (ngSubmit)="save(form)"><fieldset class="admin-fields" [disabled]="busy()"><div class="admin-grid">
 <label>{{'uiText.account.2'|t}} <span class="required-mark" aria-hidden="true">*</span><select name="accountId" [(ngModel)]="model.accountId" required [disabled]="!creating"><option [ngValue]="null">{{'uiText.choose.an.account'|t}}</option>@for(a of availableAccounts();track a.id){<option [ngValue]="a.id">{{a.email}} (#{{a.id}})</option>}</select></label>
 <label>{{'uiText.business.subtype'|t}} <span class="required-mark" aria-hidden="true">*</span><select name="type" [(ngModel)]="model.type" (ngModelChange)="detail=''" required [disabled]="!creating">@for(t of types;track t){<option [ngValue]="t">{{t|uiLabel:'role'}}</option>}</select></label>
 <label>{{'catalog.museum'|t}} <span class="required-mark" aria-hidden="true">*</span><select name="museumId" [(ngModel)]="model.museumId" required><option [ngValue]="null">{{'qrBoard.chooseMuseum'|t}}</option>@for(m of museums();track m.id){<option [ngValue]="m.id">{{(m|localized:'name')}}{{m.active?'':" "+('uiText.inactive.label'|t)}}</option>}</select></label>
 <label>{{'login.firstName'|t}} <span class="required-mark" aria-hidden="true">*</span><input name="firstName" [(ngModel)]="model.firstName" required></label><label>{{'login.lastName'|t}} <span class="required-mark" aria-hidden="true">*</span><input name="lastName" [(ngModel)]="model.lastName" required></label>
 <label>{{'uiText.phone.optional'|t}}<input type="tel" name="phone" [(ngModel)]="model.phone"></label>
 <label>{{'uiText.employment.date'|t}} <span class="required-mark" aria-hidden="true">*</span><input type="date" name="hiredOn" [(ngModel)]="model.hiredOn" required></label>
 <label>{{'uiText.supervisor'|t}}<select name="supervisorId" [(ngModel)]="model.supervisorId"><option [ngValue]="null">{{'uiText.no.supervisor'|t}}</option>@for(e of supervisors();track e.id){<option [ngValue]="e.id">{{e.firstName}} {{e.lastName}} (#{{e.id}})</option>}</select></label>
 <label>{{subtype.label|t}} {{'uiText.optional'|t}}<input name="subtypeDetail" [(ngModel)]="detail"></label>
 </div>@if(!creating){<p>{{'uiText.an.existing.employee.s.account.and.subtype.cannot.change'|t}}</p>}
 @if(form.invalid&&form.touched){<p class="message error">{{'uiText.complete.the.required.fields.and.enter.a.valid.date'|t}}</p>}
 <div class="admin-actions"><button class="button" [disabled]="form.invalid||busy()">{{'uiText.save.employee'|t}}</button>@if(model.accountId){<a [routerLink]="['/admin/accounts',model.accountId]">{{'uiText.view.and.assign.system.roles'|t}}</a>}</div>
 </fieldset></form>
}
`})
export class AdminEmployee extends AdminState {
  private api=inject(Api);private route=inject(ActivatedRoute);private router=inject(Router);
  creating=this.route.snapshot.data['mode']==='new';id=Number(this.route.snapshot.paramMap.get('id'));loaded=signal(false);
  accounts=signal<Account[]>([]);museums=signal<Museum[]>([]);employees=signal<Employee[]>([]);types=Object.keys(subtypeFields) as EmployeeType[];detail='';
  model:{accountId:number|null;museumId:number|null;firstName:string;lastName:string;phone:string|null;hiredOn:string;supervisorId:number|null;type:EmployeeType}={accountId:null,museumId:null,firstName:'',lastName:'',phone:'',hiredOn:'',supervisorId:null,type:'CURATOR'};
  constructor(){super();this.load();}
  get subtype(){return subtypeFields[this.model.type];}
  availableAccounts(){return this.accounts().filter(a=>a.id===this.model.accountId&&!this.creating||!this.employees().some(e=>e.accountId===a.id));}
  supervisors(){return this.employees().filter(e=>e.id!==this.id);}
  load(){void this.run(async()=>{this.loaded.set(false);if(!this.creating&&(!Number.isSafeInteger(this.id)||this.id<=0)){this.error.set(translate("uiText.invalid.employee.id"));return;}
    const [a,m,e,current]=await Promise.all([this.api.get<Account[]>('/admin/accounts',10000),this.api.get<Museum[]>('/museums',10000),this.api.get<Employee[]>('/employees',10000),this.creating?Promise.resolve(null):this.api.get<Employee>('/employees/'+this.id,10000)]);
    this.accounts.set(a);this.museums.set(m);this.employees.set(e);
    if(current){this.model={accountId:current.accountId,museumId:current.museumId,firstName:current.firstName,lastName:current.lastName,phone:current.phone,hiredOn:current.hiredOn,supervisorId:current.supervisorId,type:current.type};this.detail=String(current[this.subtype.key as keyof Employee]||'');}
    else{const selected=Number(this.route.snapshot.queryParamMap.get('accountId'));if(this.availableAccounts().some(a=>a.id===selected))this.model.accountId=selected;}
    this.loaded.set(true);if(this.route.snapshot.queryParamMap.has('created'))this.notice.set(translate("uiText.employee.created.roles.are.assigned.separately.on.the.account"));});}
  save(form:NgForm){if(form.invalid||this.busy()||!this.loaded())return;void this.run(async()=>{
    if(!this.model.firstName.trim()||!this.model.lastName.trim()){this.error.set(translate("uiText.first.and.last.names.are.required"));return;}
    const body={...this.model,firstName:this.model.firstName.trim(),lastName:this.model.lastName.trim(),phone:this.model.phone?.trim()||null,[this.subtype.key]:this.detail.trim()||null};
    const saved=this.creating?await this.api.post<Employee>('/employees',body):await this.api.put<Employee>('/employees/'+this.id,body);
    if(this.creating)await this.router.navigate(['/admin/employees',saved.id,'edit'],{queryParams:{created:1}});else this.notice.set(translate("uiText.employee.profile.saved.system.roles.are.unchanged"));
  });}
}
