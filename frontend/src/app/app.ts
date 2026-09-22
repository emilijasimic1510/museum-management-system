import { Component, inject, effect, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { Auth } from './core/auth';
import { LocalDemo } from './core/scan';
@Component({selector:'app-root',imports:[RouterLink,RouterLinkActive,RouterOutlet],template:`
  <a class="skip" href="#main">Pređi na sadržaj</a>
  <header class="site-header"><a class="brand" routerLink="/" aria-label="Muzej, početna"><span class="brand-icon">M</span><span>MUZEJ<small>Prostor za otkrivanje</small></span></a>
    <nav aria-label="Glavna navigacija"><a routerLink="/" routerLinkActive="active" [routerLinkActiveOptions]="{exact:true}">Istražite</a>
      @if(showDemoBoard()){<a routerLink="/demo-qr" routerLinkActive="active">Demo QR tabla</a>}
      @if(auth.session()){<a routerLink="/tickets" routerLinkActive="active">Ulaznice</a><a routerLink="/visits" routerLinkActive="active">Posete</a><a routerLink="/recommendations" routerLinkActive="active">Za vas</a><a routerLink="/profile" routerLinkActive="active">Profil</a><a routerLink="/cashier" routerLinkActive="active">Blagajna</a><button class="text-button" (click)="auth.logout()">Odjava</button>}
      @else{<a routerLink="/login" routerLinkActive="active">Prijava</a><a class="button small" routerLink="/register">Kreirajte nalog</a>}
    </nav></header>
  <main id="main" tabindex="-1"><router-outlet /></main>
  <footer><span class="wordmark">MUZEJ</span><span>Otkrijte priče. Pronađite svoj put.</span><span>Portal za posetioce</span></footer>
`})
export class App {
  readonly auth=inject(Auth);private demo=inject(LocalDemo);showDemoBoard=signal(false);
  constructor(){effect(()=>{const session=this.auth.session();this.showDemoBoard.set(false);
    if(session)void this.auth.account().then(async account=>{
      const show=account.roles.some(r=>['CASHIER','CURATOR','GUIDE','ADMINISTRATOR'].includes(r)) && await this.demo.enabled();
      if(this.auth.session()===session)this.showDemoBoard.set(show);
    }).catch(()=>{});
  });}
}
