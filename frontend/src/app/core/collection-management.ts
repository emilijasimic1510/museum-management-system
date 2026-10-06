import { translate } from './i18n';
import { inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { CanActivateFn, Router } from '@angular/router';
import { Auth } from './auth';
import { errorMessage } from './api';

export type CollectionKind='museums'|'halls'|'exhibitions'|'exhibition-terms';
export type Value=string|number|boolean|null;
export interface CollectionRecord {
  id:number;name?:string;museumId?:number;hallId?:number;exhibitionId?:number;
  start?:string;end?:string;status?:string;active?:boolean;onlineVisible?:boolean;
  [field:string]:unknown;
}
export interface ManagedPrice {exhibitionId:number;category:string;validFrom:string;validTo:string|null;price:number;note:string|null}
export interface Field {key:string;label:string;type:'text'|'textarea'|'number'|'datetime-local'|'checkbox'|'select';required?:boolean;min?:number;step?:string;options?:{value:Value;label:string}[]}
const options=(values:string[])=>values.map(value=>({value,label:'status.'+value}));
export const collectionLabels:Record<CollectionKind,string>={museums:translate("uiText.museums"),halls:translate("uiText.halls"),exhibitions:translate("uiText.exhibitions"),'exhibition-terms':translate("uiText.time.slots")};
export const fields:Record<CollectionKind,Field[]>={
  museums:[{key:'name',label:translate("uiText.name"),type:'text',required:true},{key:'foundationYear',label:translate("uiText.foundation.year"),type:'number',step:'1'},
    {key:'address',label:translate("uiText.address"),type:'text',required:true},{key:'description',label:translate("uiText.description"),type:'textarea'},{key:'active',label:translate("uiText.active.museum"),type:'checkbox'}],
  halls:[{key:'museumId',label:translate("catalog.museum"),type:'select',required:true},{key:'name',label:translate("uiText.hall.name"),type:'text',required:true},
    {key:'floor',label:translate("uiText.floor"),type:'number',step:'1'},{key:'area',label:translate("uiText.area.m"),type:'number',min:0.01,step:'0.01'},
    {key:'capacity',label:translate("event.capacity"),type:'number',min:1,step:'1'}],
  exhibitions:[{key:'hallId',label:translate("uiText.hall.2"),type:'select',required:true},{key:'name',label:translate("uiText.exhibition.name"),type:'text',required:true},
    {key:'description',label:translate("uiText.description"),type:'textarea'},{key:'start',label:translate("uiText.exhibition.start"),type:'datetime-local',required:true},
    {key:'end',label:translate("uiText.exhibition.end"),type:'datetime-local',required:true},{key:'status',label:translate("ui.status"),type:'select',required:true,options:options(['PLANIRANA','AKTIVNA','ZAVRSENA','OTKAZANA'])},
    {key:'onlineVisible',label:translate("uiText.publicly.visible"),type:'checkbox'}],
  'exhibition-terms':[{key:'exhibitionId',label:translate("ui.exhibition"),type:'select',required:true},{key:'start',label:translate("uiText.time.slot.start"),type:'datetime-local',required:true},
    {key:'end',label:translate("uiText.time.slot.end"),type:'datetime-local',required:true},{key:'capacity',label:translate("event.capacity"),type:'number',required:true,min:1,step:'1'},
    {key:'status',label:translate("ui.status"),type:'select',required:true,options:options(['OTVOREN','POPUNJEN','ZAVRSEN','OTKAZAN'])}]
};
export function hasCollectionRole(auth:Auth,adminOnly=false){return auth.currentAccount()?.roles.some(r=>r.replace(/^ROLE_/,'')==='ADMINISTRATOR'||(!adminOnly&&r.replace(/^ROLE_/,'')==='CURATOR'))===true;}
export const collectionGuard:CanActivateFn=async(route,state)=>{
  const auth=inject(Auth),router=inject(Router);
  if(!auth.token())return router.createUrlTree(['/login'],{queryParams:{returnUrl:state.url}});
  try{await auth.account();return hasCollectionRole(auth,!!route.data['adminOnly'])?true:router.createUrlTree(['/forbidden']);}
  catch{return router.createUrlTree(['/login']);}
};
export function localTimestamp(value:string){const d=new Date(value);return new Date(d.getTime()-d.getTimezoneOffset()*60000).toISOString().slice(0,-1);}
export function toInstant(value:Value,original?:unknown){
  if(typeof original==='string'&&value===localTimestamp(original))return original;
  return new Date(String(value)).toISOString();
}
export class CollectionState {
  busy=signal(false);error=signal('');notice=signal('');
  async run(action:()=>Promise<void>){if(this.busy())return;this.busy.set(true);this.error.set('');this.notice.set('');
    try{await action();}catch(e){this.error.set(e instanceof HttpErrorResponse&&e.status===409
      ?translate("uiText.the.change.could.not.be.saved.check.overlapping.periods.capacity.and.related.r")
      :errorMessage(e));}finally{this.busy.set(false);}}
}
