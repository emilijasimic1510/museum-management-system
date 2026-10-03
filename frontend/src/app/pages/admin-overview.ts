import { Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Api } from '../core/api';
import { Account } from '../core/models';
import { AdminRole, AdminState, Employee, adminStyles, subtypeFields } from '../core/admin';
import { Feedback } from '../shared/feedback';

@Component({imports:[RouterLink,Feedback,DatePipe],styles:[adminStyles],template:`
<section class="page-heading"><p class="eyebrow">ADMINISTRATOR</p><h1>{{profile?'Moj profil':'Pregled sistema'}}</h1></section>
<app-feedback [busy]="busy()" [error]="error()" /><button class="button secondary" [disabled]="busy()" (click)="load()">Osveži</button>
@if(loaded()){
 @if(profile){
   @if(me();as a){<article class="panel admin-break"><h2>{{a.email}}</h2><p>Nalog #{{a.id}} · {{a.active?'Aktivan':'Neaktivan'}}</p><p>Uloge: {{a.roles.join(', ')}}</p><p>Registrovan: {{a.registeredAt|date:'dd.MM.yyyy.'}}</p><p>Poslednja prijava: {{a.lastLogin?(a.lastLogin|date:'dd.MM.yyyy. HH:mm'):'Nije evidentirana'}}</p><a [routerLink]="['/admin/accounts',a.id]">Administracija sopstvenog naloga</a></article>}
   @if(ownEmployee();as e){<article class="panel"><h2>{{e.firstName}} {{e.lastName}}</h2><p>Poslovni podtip: {{e.type}}</p><p>Muzej #{{e.museumId}} · Zaposlen od {{e.hiredOn|date:'dd.MM.yyyy.'}}</p><p>{{e.phone||'Telefon nije unet'}}</p><p>{{subtypeLabel(e)}}: {{subtypeValue(e)||'Nije uneto'}}</p><a [routerLink]="['/admin/employees',e.id,'edit']">Izmeni profil zaposlenog</a></article>}
   @else{<p class="empty">Nalog nema povezan profil zaposlenog. Sistemska uloga i poslovni podtip su odvojeni.</p>}
 }@else{
   <div class="admin-kpis"><article class="panel"><h2>Nalozi</h2><strong>{{accounts().length}}</strong></article><article class="panel"><h2>Aktivni</h2><strong>{{activeCount()}}</strong></article><article class="panel"><h2>Neaktivni</h2><strong>{{accounts().length-activeCount()}}</strong></article><article class="panel"><h2>Zaposleni</h2><strong>{{employees().length}}</strong></article></div>
   <section class="panel"><h2>Nalozi po ulozi</h2><p>Jedan nalog može imati više uloga.</p>@for(r of roles();track r.id){<p>{{r.name}}: <strong>{{roleCount(r.name)}}</strong></p>}@empty{<p class="empty">Nema konfigurisanih uloga.</p>}</section>
   <section class="panel"><h2>Sistem</h2><p>{{health().application}} · {{health().status}}</p><p>Stanje postojećeg health endpointa; nije detaljan monitoring baza.</p></section>
   <div class="admin-actions"><a class="button" routerLink="/admin/accounts">Korisnički nalozi</a><a class="button secondary" routerLink="/admin/employees">Zaposleni</a><a routerLink="/collection">Prostori i program</a></div>
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
