import { Component, inject, effect, signal } from '@angular/core';
import { Router, NavigationEnd, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Auth } from './core/auth';
import { hasCollectionRole } from './core/collection-management';
@Component({selector:'app-root',imports:[RouterLink,RouterLinkActive,RouterOutlet],template:`
  <a class="skip" href="#main">Pređi na sadržaj</a>
  <header class="site-header"><a class="brand" [routerLink]="auth.isAdmin()?'/admin':auth.isCashier()?'/cashier':auth.isCurator()?'/curator':auth.isGuide()?'/guide':'/'" aria-label="Muzej, početna"><span class="brand-icon">M</span><span>MUZEJ<small>Prostor za otkrivanje</small></span></a>
    <button class="menu-toggle button secondary" type="button" aria-controls="main-nav" [attr.aria-expanded]="menuOpen()" (click)="menuOpen.set(!menuOpen())">{{menuOpen()?'Zatvori meni':'☰ Meni'}}</button>
    <nav id="main-nav" aria-label="Glavna navigacija" [class.open]="menuOpen()" (keydown.escape)="menuOpen.set(false)">
      @if(auth.isAdmin()){
        <a routerLink="/admin" routerLinkActive="active" ariaCurrentWhenActive="page" [routerLinkActiveOptions]="{exact:true}">Pregled</a>
        <a routerLink="/admin/accounts" routerLinkActive="active" ariaCurrentWhenActive="page">Korisnički nalozi</a>
        <a routerLink="/admin/roles" routerLinkActive="active" ariaCurrentWhenActive="page">Uloge</a>
        <a routerLink="/admin/employees" routerLinkActive="active" ariaCurrentWhenActive="page">Zaposleni</a>
        <a routerLink="/collection" routerLinkActive="active" ariaCurrentWhenActive="page">Prostori i program</a>
        <a routerLink="/admin/profile" routerLinkActive="active" ariaCurrentWhenActive="page">Profil</a>
      }@else if(auth.isCashier()){
        <a routerLink="/cashier" routerLinkActive="active" ariaCurrentWhenActive="page">Blagajna</a>
        <a routerLink="/cashier" fragment="validation">Validacija ulaznice</a>
        <span class="badge">Blagajnik</span>
      }@else if(auth.isCurator()){
        <a routerLink="/curator" routerLinkActive="active" ariaCurrentWhenActive="page" [routerLinkActiveOptions]="{exact:true}">Pregled</a>
        <a routerLink="/curator/exhibits" routerLinkActive="active" ariaCurrentWhenActive="page">Eksponati</a>
        <a routerLink="/curator/authors" routerLinkActive="active" ariaCurrentWhenActive="page">Autori</a>
        <a routerLink="/curator/participations" routerLinkActive="active" ariaCurrentWhenActive="page">Postavke</a>
        <a routerLink="/curator/digital-content" routerLinkActive="active" ariaCurrentWhenActive="page">Digitalni sadržaj</a>
        <a routerLink="/curator/profile" routerLinkActive="active" ariaCurrentWhenActive="page">Profil</a>
      }@else if(auth.isGuide()){
        <a routerLink="/guide" routerLinkActive="active" ariaCurrentWhenActive="page" [routerLinkActiveOptions]="{exact:true}">Pregled</a>
        <a routerLink="/guide/events" routerLinkActive="active" ariaCurrentWhenActive="page">Događaji</a>
        <a routerLink="/guide/registrations" routerLinkActive="active" ariaCurrentWhenActive="page">Prijave</a>
        <a routerLink="/guide/profile" routerLinkActive="active" ariaCurrentWhenActive="page">Profil</a>
      }@else if(!auth.session() || auth.isVisitor()){
        <a routerLink="/" routerLinkActive="active" ariaCurrentWhenActive="page" [routerLinkActiveOptions]="{exact:true}">Istražite</a>
        <a routerLink="/exhibits" routerLinkActive="active" ariaCurrentWhenActive="page">Digitalni katalog</a>
        <a routerLink="/events" routerLinkActive="active" ariaCurrentWhenActive="page">Događaji</a>
        @if(auth.isVisitor()){<a routerLink="/tickets" routerLinkActive="active" ariaCurrentWhenActive="page">Ulaznice</a><a routerLink="/visits" routerLinkActive="active" ariaCurrentWhenActive="page">Posete</a><a routerLink="/recommendations" routerLinkActive="active" ariaCurrentWhenActive="page">Za vas</a><a routerLink="/profile" routerLinkActive="active" ariaCurrentWhenActive="page">Profil</a>}
      }
      @if(auth.isManager()){<a routerLink="/manager" routerLinkActive="active" ariaCurrentWhenActive="page">Manager izveštaji</a>}
      @if(canManageCollection()&&!auth.isAdmin()){<a routerLink="/collection" routerLinkActive="active" ariaCurrentWhenActive="page">Prostori i program</a>}
      @if(auth.session()){<button class="text-button" (click)="auth.logout()">Odjava</button>}
      @else{<a routerLink="/login" routerLinkActive="active" ariaCurrentWhenActive="page">Prijava</a><a class="button small" routerLink="/register">Kreirajte nalog</a>}
    </nav></header>
  <main id="main" tabindex="-1"><router-outlet /></main>
  <footer><span class="wordmark">MUZEJ</span><span>Otkrijte priče. Pronađite svoj put.</span><span>{{auth.isCashier()?'Blagajnik':auth.isCurator()?'Upravljanje katalogom':auth.isGuide()?'Prostor za vodiče':'Portal za posetioce'}}</span></footer>
`})
export class App {
  readonly auth=inject(Auth);menuOpen=signal(false);
  canManageCollection(){return hasCollectionRole(this.auth);}
  constructor(){
    inject(Router).events.pipe(takeUntilDestroyed()).subscribe(event=>{if(event instanceof NavigationEnd)this.menuOpen.set(false);});
    effect(()=>{if(this.auth.session())void this.auth.account().catch(()=>{});});
  }
}
