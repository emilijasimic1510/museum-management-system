import { Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule, NgForm } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Api } from '../core/api';
import { Auth } from '../core/auth';
import { Account, Registration } from '../core/models';
import { AdminRole, AdminState, adminStyles } from '../core/admin';
import { Feedback } from '../shared/feedback';

@Component({imports:[FormsModule,DatePipe,RouterLink,Feedback],styles:[adminStyles],template:`
<a routerLink="/admin/accounts">← Korisnički nalozi</a><section class="page-heading"><h1>{{creating?'Kreiranje naloga':editing?'Izmena naloga':'Detalj naloga'}}</h1></section>
<app-feedback [busy]="busy()" [error]="error()" [notice]="notice()" />
@if(!creating){<button class="text-button" [disabled]="busy()" (click)="load()">Ponovo učitaj</button>}
@if(creating){
 <form #form="ngForm" class="panel" (ngSubmit)="create(form)"><p>Postojeći API kreira aktivan nalog sa VISITOR ulogom i visitor profilom. Zatim dodelite odgovarajuću ulogu i, za zaposlenog, napravite zaseban Employee profil. Saglasnost za preporuke ostaje isključena.</p>
 <fieldset class="admin-fields" [disabled]="busy()"><div class="admin-grid">
 <label>Email <span class="required-mark" aria-hidden="true">*</span><input type="email" name="email" [(ngModel)]="draft.email" required email autocomplete="off"></label>
 <label>Početna lozinka <span class="required-mark" aria-hidden="true">*</span><input type="password" name="password" [(ngModel)]="draft.password" required minlength="12" maxlength="72" autocomplete="new-password"><small>Najmanje 12 znakova; najviše 72 UTF-8 bajta.</small></label>
 <label>Ime <span class="required-mark" aria-hidden="true">*</span><input name="firstName" [(ngModel)]="draft.firstName" required></label><label>Prezime <span class="required-mark" aria-hidden="true">*</span><input name="lastName" [(ngModel)]="draft.lastName" required></label><label>Telefon (opciono)<input type="tel" name="phone" [(ngModel)]="draft.phone"></label>
 </div><button class="button" [disabled]="form.invalid||busy()">Kreiraj nalog</button></fieldset></form>
}
@if(account();as a){
 <section class="panel admin-break"><h2>{{a.email}}</h2><p>Nalog #{{a.id}} · {{a.active?'Aktivan':'Neaktivan'}}</p><p>Registrovan {{a.registeredAt|date:'dd.MM.yyyy. HH:mm'}} · Poslednja prijava {{a.lastLogin?(a.lastLogin|date:'dd.MM.yyyy. HH:mm'):'nije evidentirana'}}</p>
 @if(editing){<form #editForm="ngForm" (ngSubmit)="saveEmail(editForm)"><label>Email <span class="required-mark" aria-hidden="true">*</span><input type="email" name="email" [(ngModel)]="email" required email [disabled]="busy()"></label><button class="button" [disabled]="editForm.invalid||busy()">Sačuvaj email</button></form>}
 @else{<a [routerLink]="['/admin/accounts',a.id,'edit']">Izmeni email</a>}
 <div class="admin-actions"><button class="button secondary" [class.danger]="a.active" [disabled]="busy()" (click)="confirm.set('status')">{{a.active?'Deaktiviraj nalog':'Aktiviraj nalog'}}</button><a routerLink="/admin/employees/new" [queryParams]="{accountId:a.id}">Kreiraj profil zaposlenog</a></div>
 <p>Deaktivacija čuva poslovnu istoriju i onemogućava pristup nalogu. Brisanje naloga nije ponuđeno.</p></section>
 <section id="roles" class="panel"><h2>Dodeljene uloge</h2><p>Nalog mora zadržati najmanje jednu ulogu. Uklanjanje uloge ne briše poslovni podtip zaposlenog.</p>
 @for(name of a.roles;track name){<div class="admin-actions"><strong class="badge">{{name}}</strong><button type="button" class="button danger" [disabled]="busy()||a.roles.length<=1" (click)="askRevoke(name)">Ukloni {{name}}</button></div>}
 @if(availableRoles().length){<form #roleForm="ngForm" (ngSubmit)="grant(roleForm)"><label>Postojeća uloga <span class="required-mark" aria-hidden="true">*</span><select name="roleId" [(ngModel)]="roleId" required [disabled]="busy()"><option [ngValue]="null">Izaberite ulogu</option>@for(r of availableRoles();track r.id){<option [ngValue]="r.id">{{r.name}}</option>}</select></label><button class="button" [disabled]="roleForm.invalid||busy()">Dodeli ulogu</button></form>}
 @else{<p>Sve postojeće uloge su dodeljene.</p>}
 </section>
 @if(confirm()){<section class="admin-confirm" role="alert"><h2>Potvrda izmene</h2><p>{{confirm()==='status'?(a.active?'Deaktivirati ovaj nalog? Aktivne sesije više neće imati pristup.':'Aktivirati ovaj nalog?'):'Ukloniti ulogu '+revokeRole()?.name+' sa ovog naloga?'}}</p>@if(isSelf()){<p>Menjate sopstveni nalog. Možete izgubiti pristup ovom radnom prostoru.</p>}<div class="admin-actions"><button class="button" [disabled]="busy()" (click)="applyConfirmation()">Potvrdi</button><button class="button secondary" [disabled]="busy()" (click)="confirm.set(null)">Odustani</button></div></section>}
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
  load(){void this.run(async()=>{this.account.set(null);this.confirm.set(null);if(!Number.isSafeInteger(this.id)||this.id<=0){this.error.set('Neispravan ID naloga.');return;}const [a,r]=await Promise.all([this.api.get<Account>('/admin/accounts/'+this.id,10000),this.api.get<AdminRole[]>('/admin/roles',10000)]);this.account.set(a);this.email=a.email;this.roles.set(r);if(this.route.snapshot.queryParamMap.has('created'))this.notice.set('Nalog je kreiran. Dodelite uloge prema potrebi.');});}
  create(form:NgForm){if(form.invalid||this.busy())return;void this.run(async()=>{
    if(new TextEncoder().encode(this.draft.password).length>72){this.error.set('Lozinka ne sme imati više od 72 UTF-8 bajta.');return;}
    const a=await this.api.post<Account>('/admin/accounts',{...this.draft,email:this.draft.email.trim(),firstName:this.draft.firstName.trim(),lastName:this.draft.lastName.trim(),recommendationsConsent:false});this.draft.password='';await this.router.navigate(['/admin/accounts',a.id],{queryParams:{created:1}});
  });}
  saveEmail(form:NgForm){if(form.invalid||this.busy()||!this.account())return;void this.run(async()=>{const a=await this.api.put<Account>('/admin/accounts/'+this.id,{email:this.email.trim(),active:this.account()!.active});this.sync(a);this.notice.set('Email je sačuvan.');});}
  grant(form:NgForm){if(form.invalid||this.busy()||!this.roleId||!this.availableRoles().some(r=>r.id===this.roleId))return;void this.run(async()=>{const a=await this.api.post<Account>(`/admin/accounts/${this.id}/roles/${this.roleId}`,{});this.sync(a);this.roleId=null;this.notice.set('Uloga je dodeljena.');});}
  askRevoke(name:string){const r=this.roles().find(r=>r.name===name);if(!r||!this.account()||this.account()!.roles.length<=1)return;this.revokeRole.set(r);this.confirm.set('role');}
  applyConfirmation(){if(this.busy()||!this.account()||!this.confirm())return;void this.run(async()=>{
    if(this.confirm()==='status'){const a=await this.api.put<Account>('/admin/accounts/'+this.id,{email:this.account()!.email,active:!this.account()!.active});this.sync(a);this.notice.set(a.active?'Nalog je aktiviran.':'Nalog je deaktiviran; istorija je sačuvana.');}
    else{const role=this.revokeRole();if(!role||this.account()!.roles.length<=1)return;await this.api.delete(`/admin/accounts/${this.id}/roles/${role.id}`);
      if(this.isSelf()&&role.name==='ADMINISTRATOR'){this.auth.currentAccount.set({...this.account()!,roles:this.account()!.roles.filter(r=>r!==role.name)});await this.router.navigateByUrl('/forbidden');}
      else this.sync(await this.api.get<Account>('/admin/accounts/'+this.id,10000));this.notice.set('Uloga je uklonjena.');}
    this.confirm.set(null);
  });}
  private sync(a:Account){this.account.set(a);this.email=a.email;if(this.isSelf()){this.auth.currentAccount.set(a);if(!a.active){this.auth.clear();void this.router.navigateByUrl('/login');}}}
}
