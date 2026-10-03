import { Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Api, PageState } from '../core/api';
import { ExhibitionRecommendation } from '../core/models';
import { Feedback } from '../shared/feedback';
@Component({imports:[DatePipe,RouterLink,Feedback],template:`
<section class="page-heading"><p class="eyebrow">NASTAVITE DA ISTRAŽUJETE</p><h1>Izabrano za vas</h1><p>Preporuke budućih izložbi iz muzejske kolekcije.</p></section><app-feedback [busy]="busy()" [error]="error()" /><button class="text-button refresh" [disabled]="busy()" (click)="load()">{{error()?'Pokušaj ponovo':'Osveži preporuke ↻'}}</button>@if(error()){<p role="status">Preporuke trenutno nisu dostupne. <a routerLink="/">Istražite javne izložbe</a>.</p>}
@if(!busy()&&!error()&&items().length&&items().every(isGeneral)){<p role="status">Personalizovane preporuke nisu dostupne; prikazan je opšti pregled izložbi.</p>}<div class="cards">@for(e of items();track e.exhibitionId){<article class="panel recommendation"><p class="eyebrow">USKORO U MUZEJU</p><h2>{{e.name}}</h2><p>{{e.start|date:'dd.MM.yyyy.'}} — {{e.end|date:'dd.MM.yyyy.'}}</p><p class="explanation">{{e.explanation}}</p><a class="button secondary" [routerLink]="['/exhibitions',e.exhibitionId]">Pogledajte izložbu ↗</a></article>}@empty{@if(!busy()&&!error()){<div class="empty"><span class="empty-mark">✧</span><h2>Nova otkrića stižu uskoro</h2><p>Trenutno nema budućih izložbi za preporuku. Možete istražiti objavljene izložbe.</p><a class="button" routerLink="/">Istražite izložbe</a></div>}}</div><p class="muted">Saglasnost za personalizaciju možete promeniti na <a routerLink="/profile">svom profilu</a>.</p>
`})
export class RecommendationsPage extends PageState {isGeneral=(item:ExhibitionRecommendation)=>item.explanation.includes("opšti pregled");private api=inject(Api);items=signal<ExhibitionRecommendation[]>([]);constructor(){super();this.load();}load(){void this.run(async()=>{this.items.set([]);this.items.set(await this.api.get<ExhibitionRecommendation[]>('/recommendations/exhibitions',10000));});}}
