import {UiLabelPipe} from '../core/i18n';
import { TranslatePipe, translate } from './../core/i18n';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Api } from '../core/api';
import { Account, Museum } from '../core/models';
import { AdminRole, AdminState, Employee, adminStyles } from '../core/admin';
import { Feedback } from '../shared/feedback';

@Component({imports:[UiLabelPipe,TranslatePipe,FormsModule,RouterLink,Feedback],styles:[adminStyles],template:`
<section class="page-heading"><p class="eyebrow">ADMINISTRATOR</p><h1>{{kind==='accounts'?('nav.accounts'|t):kind==='roles'?('nav.roles'|t):('nav.employees'|t)}}</h1></section>
<app-feedback [busy]="busy()" [error]="error()" /><div class="admin-actions"><button class="button secondary" [disabled]="busy()" (click)="load()">{{'uiText.refresh'|t}}</button>@if(kind!=='roles'){<a class="button" [routerLink]="['/admin',kind,'new']">{{kind==='accounts'?('login.createSubmit'|t):('uiText.add.employee'|t)}}</a>}</div>
@if(kind==='accounts'||kind==='roles'){<label>{{'uiText.search.accounts.by.email'|t}}<input type="search" name="search" [(ngModel)]="search" autocomplete="off"></label>}
@if(loaded()){
 @if(kind==='roles'){<section class="panel"><h2>{{'uiText.existing.system.roles'|t}}</h2>@for(r of roles();track r.id){<p><strong>{{r.name|uiLabel:'role'}}</strong> — {{r.description||('uiText.no.description'|t)}}</p>}<p>{{'uiText.roles.grant.access.permissions.employee.subtypes.describe.jobs.and.do.not.auto'|t}}</p></section>}
 @if(kind==='employees'){
 <div class="cards">@for(e of employees();track e.id){<article class="panel admin-break"><h2>{{e.firstName}} {{e.lastName}}</h2><p>{{e.type|uiLabel:'role'}} · {{museumName(e.museumId)}}</p><p>{{'uiText.account'|t}}{{e.accountId}}</p><a [routerLink]="['/admin/employees',e.id,'edit']">{{'uiText.view.and.edit'|t}}</a></article>}@empty{<p class="empty">{{'uiText.no.employees'|t}}</p>}</div>
 }@else{
 <div class="cards">@for(a of filtered();track a.id){<article class="panel admin-break"><h2>{{a.email}}</h2><p><span class="badge" [class.success]="a.active" [class.warning]="!a.active">{{a.active?('uiText.active.2'|t):('uiText.inactive.2'|t)}}</span> · #{{a.id}}</p><p class="role-badges">@for(role of a.roles;track role){<span class="badge">{{role}}</span>}</p><a [routerLink]="['/admin/accounts',a.id]" [fragment]="kind==='roles'?'roles':undefined">{{kind==='roles'?('uiText.manage.roles'|t):('uiText.account.details'|t)}}</a></article>}@empty{<p class="empty">{{'uiText.no.accounts.match.your.search'|t}}</p>}</div>
 }
}
`})
export class AdminLists extends AdminState {
  private api=inject(Api);kind=inject(ActivatedRoute).snapshot.data['kind'] as string;
  accounts=signal<Account[]>([]);employees=signal<Employee[]>([]);roles=signal<AdminRole[]>([]);museums=signal<Museum[]>([]);loaded=signal(false);search='';
  constructor(){super();this.load();}
  filtered(){return this.accounts().filter(a=>a.email.toLowerCase().includes(this.search.trim().toLowerCase()));}
  museumName(id:number){return this.museums().find(m=>m.id===id)?.name||translate("uiText.museum.2")+id;}
  load(){void this.run(async()=>{this.loaded.set(false);
    if(this.kind==='employees'){const [e,m]=await Promise.all([this.api.get<Employee[]>('/employees',10000),this.api.get<Museum[]>('/museums',10000)]);this.employees.set(e);this.museums.set(m);}
    else{const a=await this.api.get<Account[]>('/admin/accounts',10000);this.accounts.set(a);if(this.kind==='roles')this.roles.set(await this.api.get<AdminRole[]>('/admin/roles',10000));}
    this.loaded.set(true);});}
}
