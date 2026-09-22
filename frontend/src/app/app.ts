import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { Auth } from './core/auth';
@Component({selector:'app-root',imports:[RouterLink,RouterLinkActive,RouterOutlet],template:`
  <a class="skip" href="#main">Pređi na sadržaj</a>
  <header class="site-header"><a class="brand" routerLink="/" aria-label="Muzej, početna"><span class="brand-icon">M</span><span>MUZEJ<small>Prostor za otkrivanje</small></span></a>
    <nav aria-label="Glavna navigacija"><a routerLink="/" routerLinkActive="active" [routerLinkActiveOptions]="{exact:true}">Istražite</a>
      @if(auth.session()){<a routerLink="/tickets" routerLinkActive="active">Ulaznice</a><a routerLink="/visits" routerLinkActive="active">Posete</a><a routerLink="/recommendations" routerLinkActive="active">Za vas</a><a routerLink="/profile" routerLinkActive="active">Profil</a><button class="text-button" (click)="auth.logout()">Odjava</button>}
      @else{<a routerLink="/login" routerLinkActive="active">Prijava</a><a class="button small" routerLink="/register">Kreirajte nalog</a>}
    </nav></header>
  <main id="main" tabindex="-1"><router-outlet /></main>
  <footer><span class="wordmark">MUZEJ</span><span>Otkrijte priče. Pronađite svoj put.</span><span>Portal za posetioce</span></footer>
`})
export class App { readonly auth=inject(Auth); }
