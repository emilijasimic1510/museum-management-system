import { Component, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { FormsModule, NgForm } from '@angular/forms';
import { Api, PageState } from '../core/api';
import { CountRow, ManagerDashboard, MoneyRow } from '../core/manager';
import { Feedback } from '../shared/feedback';

interface ReportTable {title:string;note:string;columns:string[];rows:string[][]}
@Component({imports:[FormsModule,DatePipe,DecimalPipe,Feedback],template:`
<section class="page-heading"><p class="eyebrow">MANAGER</p><h1>Pregled poslovanja</h1><p>Posećenost, prodaja i iskustvo posetilaca iz evidentiranih podataka.</p></section>
<form #filter="ngForm" (ngSubmit)="load(filter)" class="panel manager-filter">
  <label>Datum od <span class="required-mark" aria-hidden="true">*</span><input type="date" name="from" [(ngModel)]="from" required [disabled]="busy()"></label>
  <label>Datum do <span class="required-mark" aria-hidden="true">*</span><input type="date" name="to" [(ngModel)]="to" required [min]="from" [disabled]="busy()"></label>
  <button class="button" [disabled]="filter.invalid||busy()">Prikaži izveštaj</button>
</form>
<app-feedback [busy]="busy()" [error]="error()" />
@if(error()){<button type="button" class="button secondary" [disabled]="busy()" (click)="load()">Pokušaj ponovo</button>}
@if(report();as r){
  <p class="muted">Izveštaj: {{r.from}} — {{r.to}} · {{r.zone}} · Učitano {{r.generatedAt|date:'dd.MM.yyyy. HH:mm'}}</p>
  <div class="manager-kpis">
    <article class="panel"><h2>Posete</h2><strong>{{r.attendance.total|number}}</strong></article>
    <article class="panel"><h2>Prihod</h2><strong>{{r.revenue.total|number:'1.2-2'}}</strong><p>Uspešne transakcije</p></article>
    <article class="panel"><h2>Plaćene karte</h2><strong>{{r.revenue.paidTicketValue|number:'1.2-2'}}</strong><p>Zbir sačuvanih cena, jednom po karti</p></article>
    <article class="panel"><h2>Evidentirane karte</h2><strong>{{ticketCount()|number}}</strong><p>Svi statusi, po datumu kupovine</p></article>
  </div>
  @if(empty()){<p class="empty">Nema evidentirane aktivnosti u izabranom periodu.</p>}
  @for(table of tables();track table.title){
    <section class="panel manager-section"><h2>{{table.title}}</h2><p class="muted">{{table.note}}</p>
    @if(table.rows.length){<div class="manager-table" tabindex="0" role="region" [attr.aria-label]="table.title"><table><caption class="sr-only">{{table.title}}</caption><thead><tr>@for(c of table.columns;track $index){<th scope="col">{{c}}</th>}</tr></thead><tbody>@for(row of table.rows;track $index){<tr>@for(cell of row;track $index){<td>{{cell}}</td>}</tr>}</tbody></table></div>}
    @else{<p class="empty">Nema podataka za prikaz.</p>}
    </section>
  }
  <section class="panel manager-section"><h2>Komentari uz ocene</h2>
    @if(r.commentsTruncated){<p>Prikazano je poslednjih 200 komentara. Suzite period za pregled starijih komentara.</p>}
    @for(c of r.comments;track c.id){<article class="manager-comment"><h3>{{c.exhibition}} · {{c.rating}}/5</h3><p>{{c.ratedAt|date:'dd.MM.yyyy. HH:mm'}}</p><p class="description">{{c.comment}}</p></article>}
    @empty{<p class="empty">Nema komentara u izabranom periodu.</p>}
  </section>
  <section class="panel manager-section"><h2>Kako čitati izveštaj</h2>@for(note of r.limitations;track $index){<p>{{note}}</p>}</section>
}
`,styles:[`
  .manager-filter{display:flex;flex-wrap:wrap;gap:1rem;align-items:end;margin-bottom:1rem}.manager-filter label{flex:1;min-width:180px}
  .manager-kpis{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:1rem}.manager-kpis strong{font-size:1.8rem;overflow-wrap:anywhere}
  .manager-section{margin-top:1.5rem}.manager-table{overflow-x:auto}table{width:100%;border-collapse:collapse;text-align:left}th,td{padding:.8rem;border-bottom:1px solid #ddd;vertical-align:top;overflow-wrap:anywhere}th{font-weight:700}.manager-comment{border-top:1px solid #ddd;padding:1rem 0}
  @media(max-width:800px){.manager-kpis{grid-template-columns:repeat(2,minmax(0,1fr))}}@media(max-width:480px){.manager-kpis{grid-template-columns:1fr}.manager-filter>*{width:100%}}
`]})
export class ManagerPage extends PageState {
  private api=inject(Api);
  to=new Intl.DateTimeFormat('sv-SE',{timeZone:'Europe/Belgrade'}).format(new Date());
  from=this.to.slice(0,8)+'01';report=signal<ManagerDashboard|null>(null);tables=signal<ReportTable[]>([]);
  constructor(){super();this.load();}
  ticketCount(){return this.report()?.tickets.reduce((sum,r)=>sum+r.count,0)||0;}
  empty(){const r=this.report();return !!r&&!r.attendance.total&&!this.ticketCount()&&!r.revenue.total&&!r.popularity.length&&!r.events.length&&!r.ratings.length&&!r.transitions.length;}
  load(form?:NgForm){if(form?.invalid||this.busy())return;
    if(!/^\d{4}-\d{2}-\d{2}$/.test(this.from)||!/^\d{4}-\d{2}-\d{2}$/.test(this.to)||this.from>this.to){this.error.set('Izaberite ispravan opseg: datum od ne sme biti posle datuma do.');return;}
    const from=this.from,to=this.to;
    void this.run(async()=>{this.report.set(null);this.tables.set([]);const r=await this.api.get<ManagerDashboard>(`/manager/dashboard?from=${from}&to=${to}`,15000);this.report.set(r);this.tables.set(this.mapTables(r));});
  }
  private mapTables(r:ManagerDashboard):ReportTable[]{
    const number=(n:number|null,d=0)=>n===null?'Nije evidentirano':new Intl.NumberFormat('sr-Latn-RS',{minimumFractionDigits:d,maximumFractionDigits:d}).format(n);
    const counts=(title:string,rows:CountRow[],note:string):ReportTable=>({title,note,columns:['Grupa','Broj'],rows:rows.map(v=>[v.label,number(v.count)])});
    const money=(title:string,rows:MoneyRow[]):ReportTable=>({title,note:'Uspešne transakcije u periodu. Iznosi u novčanoj jedinici postojećeg sistema.',columns:['Grupa','Prihod'],rows:rows.map(v=>[v.label,number(v.amount,2)])});
    return [counts('Posećenost po muzeju',r.attendance.museums,'Broj poseta prema vremenu ulaska.'),counts('Posećenost po izložbi',r.attendance.exhibitions,'Svaka poseta pripada jednom terminu.'),counts('Posećenost po terminu',r.attendance.terms,'Vreme termina prikazano je u zoni Europe/Belgrade.'),counts('Posećenost po danu',r.attendance.days,'Dani bez aktivnosti nemaju red.'),
      money('Prihodi po izložbi',r.revenue.exhibitions),money('Prihodi po kategoriji',r.revenue.categories),money('Prihodi po načinu plaćanja',r.revenue.methods),money('Prihodi po danu',r.revenue.days),
      counts('Prodaja — statusi ulaznica',r.tickets,'Sadašnji status karata kupljenih u periodu; KUPLJENA nije zbir svih prodaja.'),
      {title:'Popularnost postavki',note:'Sortirano po broju skeniranja. Jedinstvene posete su po postavci; trajanje je prosek unetih vrednosti.',columns:['Postavka','Eksponat','Izložba','Skeniranja','Posete','Prosek (s)','Unetih trajanja'],rows:r.popularity.map(p=>['#'+p.participationId,p.exhibit+' (#'+p.exhibitId+')',p.exhibition,number(p.scans),number(p.visits),number(p.averageSeconds,1),number(p.measuredScans)])},
      {title:'Događaji',note:'Događaji koji počinju u periodu. Popunjenost = (prijavljeni + prisustvovali) / kapacitet. Statusi su trenutni.',columns:['Događaj','Početak','Status','Kapacitet','Ukupno prijava','Prijavljeni','Prisustvovali','Otkazali','Popunjenost %'],rows:r.events.map(e=>[e.name+' (#'+e.id+')',new Intl.DateTimeFormat('sr-Latn-RS',{dateStyle:'short',timeStyle:'short',timeZone:r.zone}).format(new Date(e.start)),e.status,number(e.capacity),number(e.registrations),number(e.registered),number(e.attended),number(e.cancelled),number(e.occupancyPercent,1)])},
      {title:'Ocene po izložbi',note:'Ocene ostavljene u izabranom periodu. Bez identiteta posetilaca.',columns:['Izložba','Broj ocena','Prosečna ocena'],rows:r.ratings.map(v=>[v.exhibition,number(v.count),number(v.average,2)])},
      counts('Zabeleženi prelazi između postavki',r.transitions,'Uzastopni skenovi iste posete; vreme drugog skena određuje period. Nije dokaz fizičke putanje ili mesta prekida obilaska.'),
      {title:'Digitalni sadržaj — inventar',note:'Trenutni broj zapisa/verzija; filter datuma se ne primenjuje. Ne predstavlja broj pregleda ili reprodukcija.',columns:['Postavka','Eksponat','Izložba','Jezik','Tip','Zapisi'],rows:r.contentInventory.map(c=>['#'+c.participationId,c.exhibit,c.exhibition,c.language||'Nije naveden',c.type,number(c.records)])}
    ];
  }
}
