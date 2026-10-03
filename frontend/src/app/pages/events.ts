import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Api } from '../core/api';
import { EventState, MuseumEvent, eventLabels, eventStatuses, eventDate, localDay, registrationOpen } from '../core/events';
import { Feedback } from '../shared/feedback';

@Component({imports:[FormsModule,RouterLink,Feedback],template:`
<section class="page-heading"><p class="eyebrow">{{guide?'PROSTOR ZA VODIČE':'DOŽIVITE MUZEJ'}}</p><h1>Događaji</h1><p>{{guide?'Upravljajte programom muzeja i prijavama posetilaca.':'Pronađite turu, predavanje ili radionicu i rezervišite svoje mesto.'}}</p>
@if(guide){<a class="button" routerLink="/guide/events/new">Novi događaj</a>}</section>
<app-feedback [busy]="busy()" [error]="error()" [notice]="notice()" />
<section class="panel event-filters" aria-label="Filteri događaja">
<label>Pretraga<input type="search" [ngModel]="query()" (ngModelChange)="query.set($event)" placeholder="Naziv događaja"></label>
<label>Status<select [ngModel]="status()" (ngModelChange)="status.set($event)"><option value="">Svi statusi</option>@for(s of statuses;track s){<option [value]="s">{{labels[s]}}</option>}</select></label>
<label>Od datuma<input type="date" [ngModel]="from()" (ngModelChange)="from.set($event)"></label>
<label>Do datuma<input type="date" [ngModel]="to()" (ngModelChange)="to.set($event)"></label>
<button class="button secondary" (click)="load()" [disabled]="busy()">Osveži</button></section>
@if(!busy()&&!error()&&!filtered().length){<p class="empty">{{events().length?'Nema događaja za izabrane filtere.':'Trenutno nema događaja.'}}</p>}
<div class="cards">@for(e of filtered();track e.id){<article class="panel event-card"><div class="event-tags"><span class="eyebrow">{{labels[e.type]}}</span><span class="badge">{{labels[e.status]}}</span></div><h2>{{e.name}}</h2>
<p>{{date(e.start)}} – {{date(e.end)}}</p><p>Prijavljenih: {{e.registeredCount}} / {{e.capacity}} · Slobodnih mesta: {{remaining(e)}}</p>
@if(e.language){<p>Jezik: {{e.language}}</p>}
@if(guide){<div class="event-actions"><a class="button secondary" [routerLink]="['/guide/events',e.id,'edit']">Izmeni</a><a class="button secondary" [routerLink]="['/guide/events',e.id,'registrations']">Prijave</a><button class="button danger" [disabled]="busy()" (click)="pending.set(e.id)">Obriši</button></div>
@if(pending()===e.id){<div class="message" role="alert"><p>Obrisati „{{e.name}}“? Događaj sa istorijom prijava nije moguće obrisati.</p><div class="event-actions"><button class="button danger" [disabled]="busy()" (click)="remove(e.id)">Potvrdi brisanje</button><button class="button secondary" [disabled]="busy()" (click)="pending.set(null)">Odustani</button></div></div>}}
@else{<p>{{open(e)?'Prijavljivanje je dostupno.':'Prijavljivanje trenutno nije dostupno.'}}</p><a class="button secondary" [routerLink]="['/events',e.id]">Detalji i prijava</a>}
</article>}</div>`})
export class EventsPage extends EventState {
  private api=inject(Api);guide=inject(ActivatedRoute).snapshot.data['guide']===true;
  events=signal<MuseumEvent[]>([]);query=signal('');status=signal('');from=signal('');to=signal('');pending=signal<number|null>(null);
  labels=eventLabels;statuses=eventStatuses;date=eventDate;open=registrationOpen;
  filtered=computed(()=>this.events().filter(e=>e.name.toLocaleLowerCase('sr').includes(this.query().trim().toLocaleLowerCase('sr'))&&(!this.status()||e.status===this.status())&&(!this.from()||localDay(e.start)>=this.from())&&(!this.to()||localDay(e.start)<=this.to())).sort((a,b)=>Date.parse(a.start)-Date.parse(b.start)));
  constructor(){super();this.load();}
  remaining(e:MuseumEvent){return Math.max(0,e.capacity-e.registeredCount);}
  load(){void this.run(async()=>{this.events.set(await (this.guide?this.api.get<MuseumEvent[]>('/events'):this.api.publicGet<MuseumEvent[]>('/events')));});}
  remove(id:number){if(!this.guide||this.pending()!==id)return;void this.run(async()=>{await this.api.delete('/events/'+id);this.events.update(items=>items.filter(e=>e.id!==id));this.pending.set(null);this.notice.set('Događaj je obrisan.');},'Događaj ima povezane prijave i ne može se obrisati. Ako se neće održati, promenite status u Otkazan.');}
}
