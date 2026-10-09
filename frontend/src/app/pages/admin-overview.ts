import {UiLabelPipe} from '../core/i18n';
import { TranslatePipe } from './../core/i18n';
import { Component, inject, signal } from '@angular/core';
import { LocalDatePipe as DatePipe } from '../core/i18n';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Api } from '../core/api';
import { Account } from '../core/models';
import { AdminRole, AdminState, Employee, adminStyles, subtypeFields } from '../core/admin';
import { Feedback } from '../shared/feedback';

@Component({imports:[UiLabelPipe,TranslatePipe,RouterLink,Feedback,DatePipe],styles:[adminStyles],template:`
<section class="page-heading"><p class="eyebrow">{{'role.ADMINISTRATOR'|t}}</p><h1>{{profile?('ui.profile'|t):('uiText.system.overview'|t)}}</h1></section>
<app-feedback [busy]="busy()" [error]="error()" /><button class="button secondary" [disabled]="busy()" (click)="load()">{{'uiText.refresh'|t}}</button>
@if(loaded()){
 @if(profile){
   @if(me();as a){<article class="panel admin-break"><h2>{{a.email}}</h2><p>{{'uiText.account'|t}}{{a.id}} · {{a.active?('uiText.active.2'|t):('uiText.inactive.2'|t)}}</p><p>{{'uiText.roles'|t}} @for(role of a.roles;track role){<span>{{role|uiLabel:'role'}}{{$last?'':', '}}</span>}</p><p>{{'uiText.registered.2'|t}} {{a.registeredAt|date:'dd.MM.yyyy.'}}</p><p>{{'uiText.last.sign.in.2'|t}} {{a.lastLogin?(a.lastLogin|date:'dd.MM.yyyy. HH:mm'):('uiText.not.recorded.2'|t)}}</p><a [routerLink]="['/admin/accounts',a.id]">{{'uiText.manage.your.own.account'|t}}</a></article>}
   @if(ownEmployee();as e){<article class="panel"><h2>{{e.firstName}} {{e.lastName}}</h2><p>{{'uiText.business.subtype.2'|t}} {{e.type|uiLabel:'role'}}</p><p>{{'uiText.museum.2'|t}}{{e.museumId}} {{'uiText.employed.since'|t}} {{e.hiredOn|date:'dd.MM.yyyy.'}}</p><p>{{e.phone||('uiText.no.phone.number'|t)}}</p><p>{{subtypeLabel(e)|t}}: {{subtypeValue(e)||('uiText.not.entered'|t)}}</p><a [routerLink]="['/admin/employees',e.id,'edit']">{{'uiText.edit.employee.profile'|t}}</a></article>}
   @else{<p class="empty">{{'uiText.this.account.has.no.employee.profile.the.system.role.and.business.subtype.are.'|t}}</p>}
 }@else{
   <div class="admin-kpis"><article class="panel"><h2>{{'uiText.accounts'|t}}</h2><strong>{{accounts().length}}</strong></article><article class="panel"><h2>{{'uiText.active'|t}}</h2><strong>{{activeCount()}}</strong></article><article class="panel"><h2>{{'uiText.inactive'|t}}</h2><strong>{{accounts().length-activeCount()}}</strong></article><article class="panel"><h2>{{'nav.employees'|t}}</h2><strong>{{employees().length}}</strong></article></div>
   <section class="panel"><h2>{{'uiText.accounts.by.role'|t}}</h2><p>{{'uiText.an.account.may.have.multiple.roles'|t}}</p>@for(r of roles();track r.id){<p>{{r.name|uiLabel:'role'}}: <strong>{{roleCount(r.name)}}</strong></p>}@empty{<p class="empty">{{'uiText.no.roles.configured'|t}}</p>}</section>
   <section class="panel"><h2>{{'uiText.system'|t}}</h2><p>{{health().application}} · {{health().status|uiLabel:'ui.health'}}</p><p>{{'uiText.current.service.health.detailed.database.monitoring.is.not.included'|t}}</p></section>
   <div class="admin-actions"><a class="button" routerLink="/admin/accounts">{{'nav.accounts'|t}}</a><a class="button secondary" routerLink="/admin/employees">{{'nav.employees'|t}}</a><a routerLink="/collection">{{'nav.collection'|t}}</a></div>
 }
}
`})
export class AdminOverview extends AdminState {
  private api=inject(Api);profile=!!inject(ActivatedRoute).snapshot.data['profile'];loaded=signal(false);
  accounts=signal<Account[]>([]);employees=signal<Employee[]>([]);roles=signal<AdminRole[]>([]);me=signal<Account|null>(null);health=signal({status:'',application:''});
  constructor(){super();this.load();}
  activeCount(){return this.accounts().filter(a=>a.active).length;}
  roleCount(name:string){return this.accounts().filter(a=>a.roles.includes(name)).length;}
  ownEmployee(){return this.employees().find(e=>e.accountId===this.me()?.id);}
  subtypeLabel(e:Employee){return subtypeFields[e.type].label;}
  subtypeValue(e:Employee){return e[subtypeFields[e.type].key as keyof Employee];}
  load(){void this.run(async()=>{this.loaded.set(false);
    if(this.profile){const [me,employees]=await Promise.all([this.api.get<Account>('/accounts/me',10000),this.api.get<Employee[]>('/employees',10000)]);this.me.set(me);this.employees.set(employees);}
    else{const [a,e,r,h]=await Promise.all([this.api.get<Account[]>('/admin/accounts',10000),this.api.get<Employee[]>('/employees',10000),this.api.get<AdminRole[]>('/admin/roles',10000),this.api.get<{status:string;application:string}>('/health',10000)]);this.accounts.set(a);this.employees.set(e);this.roles.set(r);this.health.set(h);}
    this.loaded.set(true);});}
}
