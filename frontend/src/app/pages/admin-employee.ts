import { Component, inject, signal } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Api } from '../core/api';
import { Account, Museum } from '../core/models';
import { AdminState, Employee, EmployeeType, adminStyles, subtypeFields } from '../core/admin';
import { Feedback } from '../shared/feedback';

@Component({imports:[FormsModule,RouterLink,Feedback],styles:[adminStyles],template:`
<a routerLink="/admin/employees">← Zaposleni</a><section class="page-heading"><h1>{{creating?'Novi zaposleni':'Profil zaposlenog'}}</h1><p>Jedan Employee pripada tačno jednom poslovnom podtipu. Ova forma ne dodeljuje sistemske uloge.</p></section>
<app-feedback [busy]="busy()" [error]="error()" [notice]="notice()" /><button class="text-button" [disabled]="busy()" (click)="load()">Ponovo učitaj</button>
@if(loaded()){
 @if(!availableAccounts().length&&creating){<p class="empty">Nema naloga bez zaposlenog profila. <a routerLink="/admin/accounts/new">Kreirajte nalog</a>.</p>}
 <form #form="ngForm" class="panel" (ngSubmit)="save(form)"><fieldset class="admin-fields" [disabled]="busy()"><div class="admin-grid">
 <label>Nalog <span class="required-mark" aria-hidden="true">*</span><select name="accountId" [(ngModel)]="model.accountId" required [disabled]="!creating"><option [ngValue]="null">Izaberite nalog</option>@for(a of availableAccounts();track a.id){<option [ngValue]="a.id">{{a.email}} (#{{a.id}})</option>}</select></label>
 <label>Poslovni podtip <span class="required-mark" aria-hidden="true">*</span><select name="type" [(ngModel)]="model.type" (ngModelChange)="detail=''" required [disabled]="!creating">@for(t of types;track t){<option [ngValue]="t">{{t}}</option>}</select></label>
 <label>Muzej <span class="required-mark" aria-hidden="true">*</span><select name="museumId" [(ngModel)]="model.museumId" required><option [ngValue]="null">Izaberite muzej</option>@for(m of museums();track m.id){<option [ngValue]="m.id">{{m.name}}{{m.active?'':' (neaktivan)'}}</option>}</select></label>
 <label>Ime <span class="required-mark" aria-hidden="true">*</span><input name="firstName" [(ngModel)]="model.firstName" required></label><label>Prezime <span class="required-mark" aria-hidden="true">*</span><input name="lastName" [(ngModel)]="model.lastName" required></label>
 <label>Telefon (opciono)<input type="tel" name="phone" [(ngModel)]="model.phone"></label>
 <label>Datum zaposlenja <span class="required-mark" aria-hidden="true">*</span><input type="date" name="hiredOn" [(ngModel)]="model.hiredOn" required></label>
 <label>Nadređeni<select name="supervisorId" [(ngModel)]="model.supervisorId"><option [ngValue]="null">Bez nadređenog</option>@for(e of supervisors();track e.id){<option [ngValue]="e.id">{{e.firstName}} {{e.lastName}} (#{{e.id}})</option>}</select></label>
 <label>{{subtype.label}} (opciono)<input name="subtypeDetail" [(ngModel)]="detail"></label>
 </div>@if(!creating){<p>Nalog i podtip postojećeg zaposlenog se ne menjaju.</p>}
 @if(form.invalid&&form.touched){<p class="message error">Popunite obavezna polja i ispravan datum.</p>}
 <div class="admin-actions"><button class="button" [disabled]="form.invalid||busy()">Sačuvaj zaposlenog</button>@if(model.accountId){<a [routerLink]="['/admin/accounts',model.accountId]">Pregled i dodela sistemskih uloga</a>}</div>
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
  load(){void this.run(async()=>{this.loaded.set(false);if(!this.creating&&(!Number.isSafeInteger(this.id)||this.id<=0)){this.error.set('Neispravan ID zaposlenog.');return;}
    const [a,m,e,current]=await Promise.all([this.api.get<Account[]>('/admin/accounts',10000),this.api.get<Museum[]>('/museums',10000),this.api.get<Employee[]>('/employees',10000),this.creating?Promise.resolve(null):this.api.get<Employee>('/employees/'+this.id,10000)]);
    this.accounts.set(a);this.museums.set(m);this.employees.set(e);
    if(current){this.model={accountId:current.accountId,museumId:current.museumId,firstName:current.firstName,lastName:current.lastName,phone:current.phone,hiredOn:current.hiredOn,supervisorId:current.supervisorId,type:current.type};this.detail=String(current[this.subtype.key as keyof Employee]||'');}
    else{const selected=Number(this.route.snapshot.queryParamMap.get('accountId'));if(this.availableAccounts().some(a=>a.id===selected))this.model.accountId=selected;}
    this.loaded.set(true);if(this.route.snapshot.queryParamMap.has('created'))this.notice.set('Zaposleni je kreiran. Uloge se dodeljuju zasebno na nalogu.');});}
  save(form:NgForm){if(form.invalid||this.busy()||!this.loaded())return;void this.run(async()=>{
    if(!this.model.firstName.trim()||!this.model.lastName.trim()){this.error.set('Ime i prezime su obavezni.');return;}
    const body={...this.model,firstName:this.model.firstName.trim(),lastName:this.model.lastName.trim(),phone:this.model.phone?.trim()||null,[this.subtype.key]:this.detail.trim()||null};
    const saved=this.creating?await this.api.post<Employee>('/employees',body):await this.api.put<Employee>('/employees/'+this.id,body);
    if(this.creating)await this.router.navigate(['/admin/employees',saved.id,'edit'],{queryParams:{created:1}});else this.notice.set('Profil zaposlenog je sačuvan. Sistemske uloge nisu promenjene.');
  });}
}
