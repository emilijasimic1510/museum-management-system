import { translate } from './i18n';
import { signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { errorMessage } from './api';

export interface ManagedParticipation {
  id:number; exhibitionId:number; exhibitId:number; installedAt:string|null; removedAt:string|null;
  position:string|null; tourOrder:number|null; contextDescription:string|null; qrToken:string; qrPath:string|null;
}
export interface ManagedAuthorship { authorId:number; exhibitId:number; contributionType:string|null; note:string|null }
export interface DigitalContent {
  id:number; participationId:number; type:'TEKST'|'SLIKA'|'AUDIO'; path:string; language:string|null;
  version:number; automaticallyGenerated:boolean; createdAt:string;
}
export class CuratorState {
  busy=signal(false); error=signal(''); notice=signal('');
  async run(action:()=>Promise<void>) {
    if(this.busy())return;
    this.busy.set(true);this.error.set('');this.notice.set('');
    try {await action();}
    catch(e){this.error.set(e instanceof HttpErrorResponse && e.status===409
      ? translate("uiText.the.change.could.not.be.saved.the.record.already.exists.or.has.related.data.re")
      :errorMessage(e));}
    finally{this.busy.set(false);}
  }
}
export function validReference(value:string):boolean {
  if(!value.trim() || /[\s\\]/.test(value) || value.startsWith('//'))return false;
  try {
    const decoded=decodeURIComponent(value);
    if(decoded.split(/[/?#]/).includes('..') || decoded.includes('\\'))return false;
    const url=new URL(value,'https://museum.invalid/');
    return ['http:','https:'].includes(url.protocol) && !url.username && !url.password;
  }catch{return false;}
}
export function localDate(value:string|null):string {
  if(!value)return '';
  const d=new Date(value);
  return new Date(d.getTime()-d.getTimezoneOffset()*60000).toISOString().slice(0,16);
}
