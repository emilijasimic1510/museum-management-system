import {uiLocale,enumLabel} from '../core/i18n';
import { TranslatePipe, translate } from './../core/i18n';
import { Component, computed, inject, signal } from '@angular/core';
import { LocalDatePipe as DatePipe, LocalNumberPipe as DecimalPipe } from '../core/i18n';
import { FormsModule, NgForm } from '@angular/forms';
import { Api, PageState } from '../core/api';
import { CountRow, ManagerDashboard, MoneyRow } from '../core/manager';
import { Feedback } from '../shared/feedback';

interface ReportTable {title:string;note:string;columns:string[];rows:string[][]}
@Component({imports:[TranslatePipe,FormsModule,DatePipe,DecimalPipe,Feedback],template:`
<section class="page-heading"><p class="eyebrow">MANAGER</p><h1>{{'uiText.business.overview'|t}}</h1><p>{{'uiText.attendance.sales.and.visitor.experience.from.recorded.data'|t}}</p></section>
<form #filter="ngForm" (ngSubmit)="load(filter)" class="panel manager-filter">
  <label>{{'uiText.from.date.2'|t}} <span class="required-mark" aria-hidden="true">*</span><input type="date" name="from" [(ngModel)]="from" required [disabled]="busy()"></label>
  <label>{{'uiText.to.date.2'|t}} <span class="required-mark" aria-hidden="true">*</span><input type="date" name="to" [(ngModel)]="to" required [min]="from" [disabled]="busy()"></label>
  <button class="button" [disabled]="filter.invalid||busy()">{{'uiText.show.report'|t}}</button>
</form>
<app-feedback [busy]="busy()" [error]="error()" />
@if(error()){<button type="button" class="button secondary" [disabled]="busy()" (click)="load()">{{'ui.retry'|t}}</button>}
@if(report();as r){
  <p class="muted">{{'uiText.report'|t}} {{r.from|date}} — {{r.to|date}} · {{r.zone}} {{'uiText.loaded'|t}} {{r.generatedAt|date:'dd.MM.yyyy. HH:mm'}}</p>
  <div class="manager-kpis">
    <article class="panel"><h2>{{'nav.visits'|t}}</h2><strong>{{r.attendance.total|number}}</strong></article>
    <article class="panel"><h2>{{'uiText.revenue'|t}}</h2><strong>{{r.revenue.total|number:'1.2-2'}}</strong><p>{{'uiText.successful.transactions'|t}}</p></article>
    <article class="panel"><h2>{{'uiText.paid.tickets'|t}}</h2><strong>{{r.revenue.paidTicketValue|number:'1.2-2'}}</strong><p>{{'uiText.sum.of.recorded.prices.once.per.ticket'|t}}</p></article>
    <article class="panel"><h2>{{'uiText.recorded.tickets'|t}}</h2><strong>{{ticketCount()|number}}</strong><p>{{'uiText.all.statuses.by.purchase.date'|t}}</p></article>
  </div>
  @if(empty()){<p class="empty">{{'uiText.no.activity.recorded.in.the.selected.period'|t}}</p>}
  @for(table of tables();track table.title){
    <section class="panel manager-section"><h2>{{table.title|t}}</h2><p class="muted">{{table.note|t}}</p>
    @if(table.rows.length){<div class="manager-table" tabindex="0" role="region" [attr.aria-label]="table.title"><table><caption class="sr-only">{{table.title|t}}</caption><thead><tr>@for(c of table.columns;track $index){<th scope="col">{{c|t}}</th>}</tr></thead><tbody>@for(row of table.rows;track $index){<tr>@for(cell of row;track $index){<td>{{cell}}</td>}</tr>}</tbody></table></div>}
    @else{<p class="empty">{{'uiText.no.data.to.display'|t}}</p>}
    </section>
  }
  <section class="panel manager-section"><h2>{{'uiText.rating.comments'|t}}</h2>
    @if(r.commentsTruncated){<p>{{'uiText.showing.the.latest.200.comments.narrow.the.period.to.view.older.comments'|t}}</p>}
    @for(c of r.comments;track c.id){<article class="manager-comment"><h3>{{c.exhibition}} · {{c.rating}}/5</h3><p>{{c.ratedAt|date:'dd.MM.yyyy. HH:mm'}}</p><p class="description">{{c.comment}}</p></article>}
    @empty{<p class="empty">{{'uiText.no.comments.in.the.selected.period'|t}}</p>}
  </section>
  <section class="panel manager-section"><h2>{{'uiText.how.to.read.this.report'|t}}</h2>@for(note of r.limitations;track $index){<p>{{note}}</p>}</section>
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
  from=this.to.slice(0,8)+'01';report=signal<ManagerDashboard|null>(null);tables=computed(()=>this.report()?this.mapTables(this.report()!):[]);
  constructor(){super();this.load();}
  ticketCount(){return this.report()?.tickets.reduce((sum,r)=>sum+r.count,0)||0;}
  empty(){const r=this.report();return !!r&&!r.attendance.total&&!this.ticketCount()&&!r.revenue.total&&!r.popularity.length&&!r.events.length&&!r.ratings.length&&!r.transitions.length;}
  load(form?:NgForm){if(form?.invalid||this.busy())return;
    if(!/^\d{4}-\d{2}-\d{2}$/.test(this.from)||!/^\d{4}-\d{2}-\d{2}$/.test(this.to)||this.from>this.to){this.error.set(translate("uiText.choose.a.valid.range.the.start.date.must.not.be.after.the.end.date"));return;}
    const from=this.from,to=this.to;
    void this.run(async()=>{this.report.set(null);const r=await this.api.get<ManagerDashboard>(`/manager/dashboard?from=${from}&to=${to}`,15000);this.report.set(r);});
  }
  private mapTables(r:ManagerDashboard):ReportTable[]{
    const number=(n:number|null,d=0)=>n===null?translate("uiText.not.recorded.3"):new Intl.NumberFormat(uiLocale(),{minimumFractionDigits:d,maximumFractionDigits:d}).format(n);
    const counts=(title:string,rows:CountRow[],note:string):ReportTable=>({title,note,columns:[translate("uiText.group"),translate("uiText.count")],rows:rows.map(v=>[v.label,number(v.count)])});
    const money=(title:string,rows:MoneyRow[]):ReportTable=>({title,note:translate("uiText.successful.transactions.in.the.period.amounts.use.the.system.s.existing.curren"),columns:[translate("uiText.group"),translate("uiText.revenue")],rows:rows.map(v=>[v.label,number(v.amount,2)])});
    return [counts(translate("uiText.attendance.by.museum"),r.attendance.museums,translate("uiText.visits.by.entry.time")),counts(translate("uiText.attendance.by.exhibition"),r.attendance.exhibitions,translate("uiText.each.visit.belongs.to.one.time.slot")),counts(translate("uiText.attendance.by.time.slot"),r.attendance.terms,translate("uiText.time.slots.are.shown.in.the.europe.belgrade.time.zone")),counts(translate("uiText.attendance.by.day"),r.attendance.days,translate("uiText.days.without.activity.have.no.row")),
      money(translate("uiText.revenue.by.exhibition"),r.revenue.exhibitions),money(translate("uiText.revenue.by.category"),r.revenue.categories),money(translate("uiText.revenue.by.payment.method"),r.revenue.methods.map(v=>({...v,label:enumLabel(v.label,'payment')}))),money(translate("uiText.revenue.by.day"),r.revenue.days),
      counts(translate("uiText.sales.ticket.statuses"),r.tickets.map(v=>({...v,label:enumLabel(v.label)})),translate("uiText.current.status.of.tickets.bought.in.the.period.purchased.is.not.a.total.of.all")),
      {title:translate("uiText.installation.popularity"),note:translate("uiText.sorted.by.scan.count.unique.visits.are.per.installation.duration.is.the.averag"),columns:[(translate("uiText.installation")),translate("uiText.exhibit.2"),translate("ui.exhibition"),translate("uiText.scans"),translate("nav.visits"),translate("uiText.average.s"),translate("uiText.recorded.durations")],rows:r.popularity.map(p=>['#'+p.participationId,p.exhibit+' (#'+p.exhibitId+')',p.exhibition,number(p.scans),number(p.visits),number(p.averageSeconds,1),number(p.measuredScans)])},
      {title:translate("nav.guideEvents"),note:translate("uiText.events.starting.in.the.period.occupancy.registered.attended.capacity.statuses."),columns:[translate("uiText.event"),translate("event.start"),translate("ui.status"),translate("event.capacity"),translate("uiText.total.registrations"),translate("uiText.registered.6"),translate("uiText.attended"),translate("uiText.cancelled"),translate("uiText.occupancy")],rows:r.events.map(e=>[e.name+' (#'+e.id+')',new Intl.DateTimeFormat(uiLocale(),{dateStyle:'short',timeStyle:'short',timeZone:r.zone}).format(new Date(e.start)),enumLabel(e.status),number(e.capacity),number(e.registrations),number(e.registered),number(e.attended),number(e.cancelled),number(e.occupancyPercent,1)])},
      {title:translate("uiText.ratings.by.exhibition"),note:translate("uiText.ratings.submitted.in.the.selected.period.visitor.identities.are.excluded"),columns:[translate("ui.exhibition"),translate("uiText.rating.count"),translate("uiText.average.rating")],rows:r.ratings.map(v=>[v.exhibition,number(v.count),number(v.average,2)])},
      counts(translate("uiText.recorded.transitions.between.installations"),r.transitions,translate("uiText.consecutive.scans.in.the.same.visit.the.second.scan.determines.the.period.this")),
      {title:translate("uiText.digital.content.inventory"),note:translate("uiText.current.record.version.count.the.date.filter.does.not.apply.this.is.not.a.view"),columns:[(translate("uiText.installation")),translate("uiText.exhibit.2"),translate("ui.exhibition"),translate("nav.language"),translate("uiText.type.2"),translate("uiText.records")],rows:r.contentInventory.map(c=>['#'+c.participationId,c.exhibit,c.exhibition,c.language||translate("uiText.not.specified"),enumLabel(c.type,'content'),number(c.records)])}
    ];
  }
}
