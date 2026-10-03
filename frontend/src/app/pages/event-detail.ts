import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Api } from '../core/api';
import { Auth } from '../core/auth';
import { EventState, MuseumEvent, EventRegistration, eventLabels, eventDate, registrationOpen } from '../core/events';
import { Exhibition } from '../core/models';
import { Feedback } from '../shared/feedback';

@Component({imports:[RouterLink,Feedback],template:`
<section class="page-heading"><p class="eyebrow">MUZEJSKI PROGRAM</p><h1>{{event()?.name||'Događaj'}}</h1><a routerLink="/events">← Svi događaji</a></section>
<app-feedback [busy]="busy()" [error]="error()" [notice]="notice()" />
<button class="button secondary" (click)="load()" [disabled]="busy()">Osveži podatke</button>
@if(event();as e){<div class="detail-layout"><section class="panel"><div class="event-tags"><span class="eyebrow">{{labels[e.type]}}</span><span class="badge">{{labels[e.status]}}</span></div><h2>O događaju</h2><dl class="event-facts"><dt>Početak</dt><dd>{{date(e.start)}}</dd><dt>Završetak</dt><dd>{{date(e.end)}}</dd><dt>Izložba</dt><dd>{{exhibitionName()||'Izložba #'+e.exhibitionId}}</dd><dt>Jezik</dt><dd>{{e.language||'Nije naveden'}}</dd><dt>Kapacitet</dt><dd>{{e.capacity}}</dd><dt>Slobodnih mesta</dt><dd>{{remaining(e)}}</dd></dl></section>
<section class="panel"><h2>Vaša prijava</h2>
@if(!auth.session()){<p>{{open(e)?'Prijavite se kao posetilac da biste rezervisali mesto.':'Prijavljivanje trenutno nije dostupno.'}}</p><a class="button" routerLink="/login" [queryParams]="{returnUrl:'/events/'+e.id}">Prijavi se na nalog</a>}
@else if(auth.isVisitor()){
@if(!registrationLoaded()){<p>Status vaše prijave nije učitan. Osvežite podatke pre nove radnje.</p>}
@else{
@if(registration();as r){<p>Status: <strong>{{labels[r.status]}}</strong></p>}
@if(!registration()||registration()?.status==='OTKAZAN'){<p>{{open(e)?'Prijavljivanje je dostupno.':'Prijavljivanje nije dostupno: događaj je počeo, otkazan, završen ili popunjen.'}}</p><button class="button" [disabled]="busy()||!open(e)" (click)="register()">{{busy()?'Sačekajte…':registration()?'Prijavi se ponovo':'Prijavi se na događaj'}}</button>}
@else if(registration()?.status==='PRIJAVLJEN'){<button class="button secondary" [disabled]="busy()" (click)="confirmCancel.set(true)">Otkaži moju prijavu</button>}
@else{<p>Prisustvo je evidentirano. Ovu prijavu ne možete otkazati.</p>}
@if(confirmCancel()){<div class="message" role="alert"><p>Otkazati vašu prijavu za „{{e.name}}“?</p><div class="event-actions"><button class="button" [disabled]="busy()" (click)="cancel()">Potvrdi otkazivanje</button><button class="button secondary" [disabled]="busy()" (click)="confirmCancel.set(false)">Odustani</button></div></div>}
}}
</section></div>}`})
export class EventDetail extends EventState {
  private api=inject(Api);private route=inject(ActivatedRoute);auth=inject(Auth);
  id=Number(this.route.snapshot.paramMap.get('id'));event=signal<MuseumEvent|null>(null);exhibitionName=signal('');registration=signal<EventRegistration|null>(null);registrationLoaded=signal(false);confirmCancel=signal(false);
  labels=eventLabels;date=eventDate;open=registrationOpen;
  constructor(){super();this.load();}
  remaining(e:MuseumEvent){return Math.max(0,e.capacity-e.registeredCount);}
  private async fetch(){
    this.registrationLoaded.set(false);
    const [event,exhibitions,registrations]=await Promise.all([this.api.publicGet<MuseumEvent>('/events/'+this.id),this.api.publicGet<Exhibition[]>('/exhibitions'),this.auth.isVisitor()?this.api.get<EventRegistration[]>('/visitors/me/registrations'):Promise.resolve([])]);
    this.event.set(event);this.exhibitionName.set(exhibitions.find(x=>x.id===event.exhibitionId)?.name||'');this.registration.set(registrations.find(r=>r.eventId===this.id)||null);this.registrationLoaded.set(true);
  }
  load(){void this.run(()=>this.fetch());}
  register(){const e=this.event();if(this.busy()||!this.auth.isVisitor()||!this.registrationLoaded()||!e||!registrationOpen(e)||(this.registration()&&this.registration()?.status!=='OTKAZAN'))return;void this.run(async()=>{const r=await this.api.post<EventRegistration>('/events/'+this.id+'/registrations',{});this.registration.set(r);this.notice.set('Uspešno ste prijavljeni na događaj.');await this.fetch();});}
  cancel(){if(this.busy()||!this.confirmCancel()||!this.auth.isVisitor()||!this.registrationLoaded()||this.registration()?.status!=='PRIJAVLJEN')return;void this.run(async()=>{await this.api.delete('/events/'+this.id+'/registrations/me');this.confirmCancel.set(false);this.notice.set('Vaša prijava je otkazana.');await this.fetch();},'Prijavu nije moguće otkazati jer je prisustvo već evidentirano. Osvežite podatke.');}
}
