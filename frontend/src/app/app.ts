import { Component, inject, effect, signal } from '@angular/core';
import { Router, NavigationEnd, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Auth } from './core/auth';
@Component({selector:'app-root',imports:[RouterLink,RouterLinkActive,RouterOutlet],template:`
  <a class="skip" href="#main">Pređi na sadržaj</a>
  <header class="site-header"><a class="brand" [routerLink]="auth.isCashier()?'/cashier':'/'" aria-label="Muzej, početna"><span class="brand-icon">M</span><span>MUZEJ<small>Prostor za otkrivanje</small></span></a>
    <button class="menu-toggle button secondary" type="button" aria-controls="main-nav" [attr.aria-expanded]="menuOpen()" (click)="menuOpen.set(!menuOpen())">{{menuOpen()?'Zatvori meni':'☰ Meni'}}</button>
    <nav id="main-nav" aria-label="Glavna navigacija" [class.open]="menuOpen()" (keydown.escape)="menuOpen.set(false)">
      @if(auth.isCashier()){
        <a routerLink="/cashier" routerLinkActive="active">Blagajna</a>
        <a routerLink="/cashier" fragment="validation">Validacija ulaznice</a>
        <span class="badge">Blagajnik</span>
      }@else if(!auth.session() || auth.isVisitor()){
        <a routerLink="/" routerLinkActive="active" [routerLinkActiveOptions]="{exact:true}">Istražite</a>
        <a routerLink="/exhibits" routerLinkActive="active">Digitalni katalog</a>
        @if(auth.isVisitor()){<a routerLink="/tickets" routerLinkActive="active">Ulaznice</a><a routerLink="/visits" routerLinkActive="active">Posete</a><a routerLink="/recommendations" routerLinkActive="active">Za vas</a><a routerLink="/profile" routerLinkActive="active">Profil</a>}
      }
      @if(auth.session()){<button class="text-button" (click)="auth.logout()">Odjava</button>}
      @else{<a routerLink="/login" routerLinkActive="active">Prijava</a><a class="button small" routerLink="/register">Kreirajte nalog</a>}
    </nav></header>
  <main id="main" tabindex="-1"><router-outlet /></main>
  <footer><span class="wordmark">MUZEJ</span><span>Otkrijte priče. Pronađite svoj put.</span><span>{{auth.isCashier()?'Blagajnik':'Portal za posetioce'}}</span></footer>
`})
export class App {
  readonly auth=inject(Auth);menuOpen=signal(false);
  constructor(){
    inject(Router).events.pipe(takeUntilDestroyed()).subscribe(event=>{if(event instanceof NavigationEnd)this.menuOpen.set(false);});
    effect(()=>{if(this.auth.session())void this.auth.account().catch(()=>{});});
  }
}
