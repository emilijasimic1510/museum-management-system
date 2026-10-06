import {uiLocale} from './i18n';
import { translate } from './i18n';
import { signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { errorMessage } from './api';

export type EventStatus='PLANIRAN'|'POPUNJEN'|'ZAVRSEN'|'OTKAZAN';
export type EventType='TURA'|'PREDAVANJE'|'RADIONICA';
export type RegistrationStatus='PRIJAVLJEN'|'PRISUSTVOVAO'|'OTKAZAN';
export interface MuseumEvent {
  id:number; exhibitionId:number; guideId:number|null; name:string; type:EventType;
  start:string; end:string; capacity:number; language:string|null; status:EventStatus; registeredCount:number;
}
export type EventRequest=Omit<MuseumEvent,'id'|'registeredCount'>;
export interface EventRegistration { visitorId:number; eventId:number; registeredAt:string; status:RegistrationStatus }
export interface ManagedRegistration extends EventRegistration { firstName:string; lastName:string }
export interface OwnGuide { id:number; firstName:string; lastName:string; primaryLanguage:string|null }
export const eventStatuses:EventStatus[]=['PLANIRAN','POPUNJEN','ZAVRSEN','OTKAZAN'];
export const eventTypes:EventType[]=['TURA','PREDAVANJE','RADIONICA'];
export const registrationStatuses:RegistrationStatus[]=['PRIJAVLJEN','PRISUSTVOVAO','OTKAZAN'];
export function registrationOpen(e:MuseumEvent,now=Date.now()){return e.status==='PLANIRAN' && Date.parse(e.start)>now && e.registeredCount<e.capacity;}
export function eventDate(value:string){return new Intl.DateTimeFormat(uiLocale(),{dateStyle:'medium',timeStyle:'short'}).format(new Date(value));}
export function localDay(value:string){const d=new Date(value);return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`;}
export class EventState {
  busy=signal(false);error=signal('');notice=signal('');
  async run(action:()=>Promise<void>,conflict=translate("uiText.the.change.could.not.be.saved.the.event.is.unavailable.full.or.already.registe")) {
    if(this.busy())return;
    this.busy.set(true);this.error.set('');this.notice.set('');
    try{await action();}catch(e){this.error.set(e instanceof HttpErrorResponse&&e.status===409?conflict:errorMessage(e));}
    finally{this.busy.set(false);}
  }
}
