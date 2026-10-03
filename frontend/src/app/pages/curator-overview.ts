import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Api } from '../core/api';
import { Auth } from '../core/auth';
import { Author, CatalogExhibit } from '../core/catalog';
import { CuratorState, ManagedParticipation } from '../core/curator';
import { Exhibition } from '../core/models';
import { Feedback } from '../shared/feedback';

@Component({imports:[RouterLink,Feedback],template:`
<section class="page-heading"><p class="eyebrow">KUSTOSKI PROSTOR</p><h1>Pregled zbirke</h1><p>Upravljajte predmetima, autorima i postavkama izložbi.</p></section>
<app-feedback [busy]="busy()" [error]="error()" [notice]="notice()" />
@if(error()){<button class="button" (click)="load()" [disabled]="busy()">Pokušaj ponovo</button>}
@if(counts();as c){<div class="cards">
<article class="panel"><h2>{{c.exhibits}}</h2><p>Eksponata</p><a routerLink="/curator/exhibits">Upravljanje eksponatima</a></article>
<article class="panel"><h2>{{c.authors}}</h2><p>Autora</p><a routerLink="/curator/authors">Upravljanje autorima</a></article>
<article class="panel"><h2>{{c.participations}}</h2><p>Učešća na izložbama</p><small>Aktivnih učešća: {{c.active}} — postavljena i još neuklonjena, na aktivnim izložbama koje su u toku.</small><p><a routerLink="/curator/participations">Postavke</a></p></article>
</div>}
<section class="panel"><h2>Brze radnje</h2><div class="curator-actions"><a class="button" routerLink="/curator/exhibits/new">Novi eksponat</a><a class="button secondary" routerLink="/curator/authors/new">Novi autor</a><a class="button secondary" routerLink="/curator/participations/new">Dodaj eksponat na izložbu</a><a class="button secondary" routerLink="/curator/digital-content/new">Dodaj digitalni sadržaj</a></div><p class="muted">Sadržaj se pregleda po učešću; ukupan broj se ne izračunava dodatnim pozivima za svako učešće.</p></section>
`})
export class CuratorOverview extends CuratorState {
  private api=inject(Api);counts=signal<{exhibits:number;authors:number;participations:number;active:number}|null>(null);
  constructor(){super();this.load();}
  load(){void this.run(async()=>{
    const [exhibits,authors,parts,exhibitions]=await Promise.all([this.api.get<CatalogExhibit[]>('/exhibits'),this.api.get<Author[]>('/authors'),this.api.get<ManagedParticipation[]>('/exhibit-participations'),this.api.get<Exhibition[]>('/exhibitions')]);
    const now=Date.now(),active=new Set(exhibitions.filter(e=>e.status==='AKTIVNA'&&Date.parse(e.start)<=now&&Date.parse(e.end)>now).map(e=>e.id));
    this.counts.set({exhibits:exhibits.length,authors:authors.length,participations:parts.length,active:parts.filter(p=>active.has(p.exhibitionId)&&!!p.installedAt&&Date.parse(p.installedAt)<=now&&(!p.removedAt||Date.parse(p.removedAt)>now)).length});
  });}
}
@Component({imports:[Feedback],template:`
<section class="page-heading"><h1>Profil zaposlenog</h1></section><app-feedback [busy]="busy()" [error]="error()" />
@if(auth.currentAccount();as a){<section class="panel"><h2>{{a.email}}</h2><p>{{auth.isCurator()?'Upravljanje katalogom':''}}</p><p>Uloge: {{a.roles.join(', ')}}</p><p>Detaljni lični podaci zaposlenog nisu dostupni kroz postojeći API za sopstveni nalog.</p></section>}
@if(error()){<button class="button" (click)="load()">Pokušaj ponovo</button>}
`})
export class CuratorProfile extends CuratorState {
  auth=inject(Auth);constructor(){super();this.load();}load(){void this.run(async()=>{await this.auth.account();});}
}
@Component({imports:[RouterLink],template:`<section class="page-heading"><p class="eyebrow">403</p><h1>Pristup nije dozvoljen</h1><p>Vaša uloga nema pravo pristupa ovoj stranici.</p><a class="button" routerLink="/">Na početnu</a></section>`})
export class ForbiddenPage {}
