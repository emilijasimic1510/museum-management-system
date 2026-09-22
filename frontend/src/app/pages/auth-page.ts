import { Component, inject, signal } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Auth, safeReturnUrl } from '../core/auth';
import { PageState } from '../core/api';
import { Feedback } from '../shared/feedback';

@Component({imports:[FormsModule,RouterLink,Feedback],template:`
<section class="auth-layout"><div class="auth-story"><p class="eyebrow">VAŠ PROSTOR ZA KULTURU</p><h1>Svaka poseta.<br>Nova perspektiva.</h1><p>Sačuvajte svoje ulaznice, istražite eksponate i otkrijte izložbe koje vas inspirišu.</p><div class="arch-art" aria-hidden="true"><span></span><span></span><span></span></div></div>
<div class="auth-form"><p class="eyebrow">DOBRO DOŠLI</p><h2>{{registerMode ? 'Kreirajte nalog' : 'Prijavite se'}}</h2><p class="muted">{{registerMode ? 'Vaša sledeća muzejska priča počinje ovde.' : 'Nastavite tamo gde je počela vaša radoznalost.'}}</p>
<app-feedback [busy]="busy()" [error]="error()" [notice]="notice()" />
@if(expired){<p class="message">Sesija je istekla. Prijavite se ponovo.</p>}
@if(registered()){<p class="message success">Nalog je kreiran. Sada se prijavite.</p><a class="button" routerLink="/login">Na prijavu</a>}
@else{<form #form="ngForm" (ngSubmit)="submit(form)">
@if(registerMode){<div class="form-row"><label>Ime<input name="firstName" [(ngModel)]="firstName" required autocomplete="given-name" #first="ngModel">@if(first.invalid && (first.touched || form.submitted)){<small class="field-error">Unesite ime.</small>}</label><label>Prezime<input name="lastName" [(ngModel)]="lastName" required autocomplete="family-name" #last="ngModel">@if(last.invalid && (last.touched || form.submitted)){<small class="field-error">Unesite prezime.</small>}</label></div>}
<label>Email<input name="email" type="email" [(ngModel)]="email" required email autocomplete="username" #mail="ngModel" placeholder="ime@primer.rs">@if(mail.invalid && (mail.touched || form.submitted)){<small class="field-error">Unesite ispravnu email adresu.</small>}</label>
<label>Lozinka<input name="password" type="password" [(ngModel)]="password" required [minlength]="registerMode?12:1" [maxlength]="72" [autocomplete]="registerMode?'new-password':'current-password'" #pass="ngModel">@if(registerMode){<small>Od 12 do 72 karaktera.</small>}@if(pass.invalid && (pass.touched || form.submitted)){<small class="field-error">{{registerMode?'Lozinka mora imati najmanje 12 karaktera.':'Unesite lozinku.'}}</small>}</label>
@if(registerMode){<label>Telefon <span class="muted">(opciono)</span><input name="phone" type="tel" [(ngModel)]="phone" autocomplete="tel"></label><label class="checkbox"><input name="consent" type="checkbox" [(ngModel)]="consent"><span>Želim personalizovane preporuke izložbi i eksponata.</span></label>}
<button class="button full" type="submit" [disabled]="busy()">{{busy()?'Sačekajte…':registerMode?'Kreiraj nalog':'Prijavi se'}} <span aria-hidden="true">↗</span></button>
</form>}
<p class="auth-switch">{{registerMode?'Već imate nalog?':'Prvi put ste ovde?'}} <a [routerLink]="registerMode?'/login':'/register'">{{registerMode?'Prijavite se':'Kreirajte nalog'}}</a></p></div></section>
`})
export class AuthPage extends PageState {
  private auth=inject(Auth); private router=inject(Router); private route=inject(ActivatedRoute);
  registerMode=this.route.snapshot.routeConfig?.path==='register'; expired=this.route.snapshot.queryParamMap.has('expired');
  email='';password='';firstName='';lastName='';phone='';consent=false; registered=signal(false);
  submit(form:NgForm) {
    if(form.invalid)return;
    void this.run(async()=>{
      if(this.registerMode){
        await this.auth.register({email:this.email.trim(),password:this.password,firstName:this.firstName.trim(),lastName:this.lastName.trim(),phone:this.phone.trim(),recommendationsConsent:this.consent});
        this.registered.set(true);
      }
      await this.auth.login(this.email.trim(),this.password); this.password='';
      const returnUrl=this.route.snapshot.queryParamMap.get('returnUrl');
      await this.router.navigateByUrl(await this.auth.hasRole('CASHIER') ? '/cashier' : safeReturnUrl(returnUrl));
    });
  }
}

