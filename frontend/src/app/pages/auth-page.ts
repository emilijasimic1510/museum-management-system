import { Component, inject, signal } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Auth, safeReturnUrl } from '../core/auth';
import { PageState } from '../core/api';
import { Feedback } from '../shared/feedback';
import { TranslatePipe } from '../core/i18n';

@Component({imports:[FormsModule,RouterLink,Feedback,TranslatePipe],template:`
<section class="auth-layout"><div class="auth-story"><p class="eyebrow">{{'login.eyebrow'|t}}</p><h1>{{'uiText.every.visit'|t}}<br>{{'uiText.a.new.perspective'|t}}</h1><p>{{'login.intro'|t}}</p><div class="arch-art" aria-hidden="true"><span></span><span></span><span></span></div></div>
<div class="auth-form"><p class="eyebrow">{{'login.welcome'|t}}</p><h2>{{registerMode ? ('login.create'|t) : ('login.signIn'|t)}}</h2><p class="muted">{{registerMode ? ('login.next'|t) : ('login.continue'|t)}}</p>
<app-feedback [busy]="busy()" [error]="error()" [notice]="notice()" />
@if(expired){<p class="message">{{'login.expired'|t}}</p>}
@if(registered()){<p class="message success">{{'login.registered'|t}}</p><a class="button" routerLink="/login">{{'login.toLogin'|t}}</a>}
@else{<form #form="ngForm" (ngSubmit)="submit(form)">
@if(registerMode){<div class="form-row"><label>{{'login.firstName'|t}} <span class="required-mark" aria-hidden="true">*</span><input name="firstName" [(ngModel)]="firstName" required autocomplete="given-name" #first="ngModel">@if(first.invalid && (first.touched || form.submitted)){<small class="field-error">{{'uiText.enter.your.first.name'|t}}</small>}</label><label>{{'login.lastName'|t}} <span class="required-mark" aria-hidden="true">*</span><input name="lastName" [(ngModel)]="lastName" required autocomplete="family-name" #last="ngModel">@if(last.invalid && (last.touched || form.submitted)){<small class="field-error">{{'uiText.enter.your.last.name'|t}}</small>}</label></div>}
<label>{{'login.email'|t}} <span class="required-mark" aria-hidden="true">*</span><input name="email" type="email" [(ngModel)]="email" required email autocomplete="username" #mail="ngModel" [attr.placeholder]="'uiText.name.example.com'|t">@if(mail.invalid && (mail.touched || form.submitted)){<small class="field-error">{{'login.invalidEmail'|t}}</small>}</label>
<label>{{'login.password'|t}} <span class="required-mark" aria-hidden="true">*</span><input name="password" type="password" [(ngModel)]="password" required [minlength]="registerMode?12:1" [maxlength]="72" [autocomplete]="registerMode?'new-password':'current-password'" #pass="ngModel">@if(registerMode){<small>{{'login.passwordHint'|t}}</small>}@if(pass.invalid && (pass.touched || form.submitted)){<small class="field-error">{{registerMode?('login.minPassword'|t):('login.required'|t)}}</small>}</label>
@if(registerMode){<label>{{'login.phone'|t}} <span class="muted">{{'uiText.optional'|t}}</span><input name="phone" type="tel" [(ngModel)]="phone" autocomplete="tel"></label><label class="checkbox"><input name="consent" type="checkbox" [(ngModel)]="consent"><span>{{'login.recommendations'|t}}</span></label>}
<button class="button full" type="submit" [disabled]="busy()">{{busy()?('login.wait'|t):registerMode?('login.createSubmit'|t):('login.submit'|t)}}</button>
</form>}
<p class="auth-switch">{{registerMode?('login.hasAccount'|t):('login.newHere'|t)}} <a [routerLink]="registerMode?'/login':'/register'">{{registerMode?('login.signIn'|t):('login.create'|t)}}</a></p></div></section>
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
      await this.auth.account();
      await this.router.navigateByUrl(this.auth.isAdmin()?'/admin':this.auth.isCashier()?'/cashier':this.auth.isCurator()?'/curator':this.auth.isGuide()?'/guide':this.auth.isManager()?'/manager':safeReturnUrl(returnUrl));
    });
  }
}

