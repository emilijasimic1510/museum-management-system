import { Component, inject, effect, signal } from '@angular/core';
import { Router, NavigationEnd, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Auth } from './core/auth';
import { hasQrBoardRole } from './core/qr-board';
import { hasCollectionRole } from './core/collection-management';
import { I18n, TranslatePipe } from './core/i18n';
@Component({selector:'app-root',imports:[RouterLink,RouterLinkActive,RouterOutlet,TranslatePipe],template:`
  <a class="skip" href="#main">{{'nav.skip'|t}}</a>
  <header class="site-header"><a class="brand" [routerLink]="auth.isAdmin()?'/admin':auth.isCashier()?'/cashier':auth.isCurator()?'/curator':auth.isGuide()?'/guide':'/'" [attr.aria-label]="'uiText.museum.home'|t"><span class="brand-icon">M</span><span>{{'uiText.museum'|t}}<small>{{'nav.brandTagline'|t}}</small></span></a>
    <button class="menu-toggle button secondary" type="button" aria-controls="main-nav" [attr.aria-expanded]="menuOpen()" (click)="menuOpen.set(!menuOpen())">{{menuOpen()?('nav.closeMenu'|t):'☰ '+('nav.menu'|t)}}</button>
    <nav id="main-nav" [attr.aria-label]="'nav.aria'|t" [class.open]="menuOpen()" (keydown.escape)="menuOpen.set(false)">
      @if(auth.isAdmin()){
        <a routerLink="/admin" routerLinkActive="active" ariaCurrentWhenActive="page" [routerLinkActiveOptions]="{exact:true}">{{'nav.overview'|t}}</a><a routerLink="/admin/accounts" routerLinkActive="active">{{'nav.accounts'|t}}</a><a routerLink="/admin/roles" routerLinkActive="active">{{'nav.roles'|t}}</a><a routerLink="/admin/employees" routerLinkActive="active">{{'nav.employees'|t}}</a><a routerLink="/collection" routerLinkActive="active">{{'nav.collection'|t}}</a><a routerLink="/admin/profile" routerLinkActive="active">{{'nav.profile'|t}}</a>
      }@else if(auth.isCashier()){
        <a routerLink="/cashier" routerLinkActive="active">{{'nav.cashier'|t}}</a><a routerLink="/cashier" fragment="validation">{{'nav.validation'|t}}</a><span class="badge">{{'nav.cashierLabel'|t}}</span>
      }@else if(auth.isCurator()){
        <a routerLink="/curator" routerLinkActive="active" [routerLinkActiveOptions]="{exact:true}">{{'nav.overview'|t}}</a><a routerLink="/curator/exhibits" routerLinkActive="active">{{'nav.exhibits'|t}}</a><a routerLink="/curator/authors" routerLinkActive="active">{{'nav.authors'|t}}</a><a routerLink="/curator/participations" routerLinkActive="active">{{'nav.participations'|t}}</a><a routerLink="/curator/digital-content" routerLinkActive="active">{{'nav.digitalContent'|t}}</a><a routerLink="/curator/profile" routerLinkActive="active">{{'nav.profile'|t}}</a>
      }@else if(auth.isGuide()){
        <a routerLink="/guide" routerLinkActive="active" [routerLinkActiveOptions]="{exact:true}">{{'nav.overview'|t}}</a><a routerLink="/guide/events" routerLinkActive="active">{{'nav.guideEvents'|t}}</a><a routerLink="/guide/registrations" routerLinkActive="active">{{'nav.registrations'|t}}</a><a routerLink="/guide/profile" routerLinkActive="active">{{'nav.profile'|t}}</a>
      }@else if(!auth.session() || auth.isVisitor()){
        <a routerLink="/" routerLinkActive="active" [routerLinkActiveOptions]="{exact:true}">{{'nav.explore'|t}}</a><a routerLink="/exhibits" routerLinkActive="active">{{'nav.catalog'|t}}</a><a routerLink="/events" routerLinkActive="active">{{'nav.events'|t}}</a>@if(auth.isVisitor()){<a routerLink="/tickets" routerLinkActive="active">{{'nav.tickets'|t}}</a><a routerLink="/visits" routerLinkActive="active">{{'nav.visits'|t}}</a><a routerLink="/recommendations" routerLinkActive="active">{{'nav.forYou'|t}}</a><a routerLink="/profile" routerLinkActive="active">{{'nav.profile'|t}}</a>}
      }
      @if(auth.isManager()){<a routerLink="/manager" routerLinkActive="active">{{'nav.manager'|t}}</a>}
      @if(canPrintQr()){<a routerLink="/staff/qr-board" routerLinkActive="active">{{'qrBoard.title'|t}}</a>}
      @if(canManageCollection()&&!auth.isAdmin()){<a routerLink="/collection" routerLinkActive="active" ariaCurrentWhenActive="page">{{'nav.collection'|t}}</a>}
      <span class="language-switcher" aria-label="{{'nav.language'|t}}"><button class="text-button" type="button" [class.active]="i18n.language()==='sr'" (click)="i18n.setLanguage('sr')">SR</button><span aria-hidden="true"> | </span><button class="text-button" type="button" [class.active]="i18n.language()==='en'" (click)="i18n.setLanguage('en')">EN</button></span>@if(auth.session()){<button class="text-button" (click)="auth.logout()">{{'nav.logout'|t}}</button>}@else{<a routerLink="/login" routerLinkActive="active">{{'nav.login'|t}}</a><a class="button small" routerLink="/register">{{'nav.register'|t}}</a>}
    </nav></header>
  <main id="main" tabindex="-1"><router-outlet /></main>
  <footer><span class="wordmark">{{'uiText.museum'|t}}</span><span>{{'nav.footer'|t}}</span><span>{{auth.isCashier()?('nav.cashierLabel'|t):auth.isCurator()?('nav.catalogManagement'|t):auth.isGuide()?('nav.guideSpace'|t):('nav.visitorPortal'|t)}}</span></footer>`})
export class App {
  readonly auth=inject(Auth);readonly i18n=inject(I18n);menuOpen=signal(false);
  canManageCollection(){return hasCollectionRole(this.auth);}
  canPrintQr(){return hasQrBoardRole(this.auth);}
  constructor(){
    inject(Router).events.pipe(takeUntilDestroyed()).subscribe(event=>{if(event instanceof NavigationEnd)this.menuOpen.set(false);});
    effect(()=>{if(this.auth.session())void this.auth.account().catch(()=>{});});
  }
}
