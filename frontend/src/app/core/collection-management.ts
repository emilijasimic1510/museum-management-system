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
const options=(values:string[])=>values.map(value=>({value,label:value}));
export const collectionLabels:Record<CollectionKind,string>={museums:'Muzeji',halls:'Sale',exhibitions:'Izložbe','exhibition-terms':'Termini'};
export const fields:Record<CollectionKind,Field[]>={
  museums:[{key:'name',label:'Naziv',type:'text',required:true},{key:'foundationYear',label:'Godina osnivanja',type:'number',step:'1'},
    {key:'address',label:'Adresa',type:'text',required:true},{key:'description',label:'Opis',type:'textarea'},{key:'active',label:'Aktivan muzej',type:'checkbox'}],
  halls:[{key:'museumId',label:'Muzej',type:'select',required:true},{key:'name',label:'Naziv sale',type:'text',required:true},
    {key:'floor',label:'Sprat',type:'number',step:'1'},{key:'area',label:'Površina (m²)',type:'number',min:0.01,step:'0.01'},
    {key:'capacity',label:'Kapacitet',type:'number',min:1,step:'1'}],
  exhibitions:[{key:'hallId',label:'Sala',type:'select',required:true},{key:'name',label:'Naziv izložbe',type:'text',required:true},
    {key:'description',label:'Opis',type:'textarea'},{key:'start',label:'Početak izložbe',type:'datetime-local',required:true},
    {key:'end',label:'Kraj izložbe',type:'datetime-local',required:true},{key:'status',label:'Status',type:'select',required:true,options:options(['PLANIRANA','AKTIVNA','ZAVRSENA','OTKAZANA'])},
    {key:'onlineVisible',label:'Javno vidljiva',type:'checkbox'}],
  'exhibition-terms':[{key:'exhibitionId',label:'Izložba',type:'select',required:true},{key:'start',label:'Početak termina',type:'datetime-local',required:true},
    {key:'end',label:'Kraj termina',type:'datetime-local',required:true},{key:'capacity',label:'Kapacitet',type:'number',required:true,min:1,step:'1'},
    {key:'status',label:'Status',type:'select',required:true,options:options(['OTVOREN','POPUNJEN','ZAVRSEN','OTKAZAN'])}]
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
      ?'Izmena nije moguća: proverite preklapanje perioda, kapacitet i povezane podatke. Zapis sa istorijom sačuvajte, deaktivirajte muzej ili promenite status izložbe/termina.'
      :errorMessage(e));}finally{this.busy.set(false);}}
}
