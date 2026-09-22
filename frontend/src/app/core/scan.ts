import { inject, Injectable, isDevMode } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { Api } from './api';
import { Auth } from './auth';

export interface DemoQr { participationId:number; name:string; token:string; exhibitionId:number; exhibitionName:string }
export function scanToken(input:string):string {
  const value=input.trim();
  if(!value) throw new Error('Unesite QR token ili URL.');
  if(value.startsWith('/') || /^https?:/i.test(value)) {
    const url=new URL(value,window.location.origin);
    if(url.origin!==window.location.origin || url.pathname!=='/scan' || !url.searchParams.get('token'))
      throw new Error('QR URL mora pripadati ovom muzeju i sadržati token eksponata.');
    return url.searchParams.get('token')!;
  }
  if(value.length>512 || /\s/.test(value)) throw new Error('QR token nije ispravan.');
  return value;
}
@Injectable({providedIn:'root'})
export class LocalDemo {
  private api=inject(Api);
  async enabled() {
    if(!isDevMode())return false;
    try{return (await this.api.get<{enabled:boolean}>('/local-demo/enabled')).enabled===true;}catch{return false;}
  }
}
export const demoBoardGuard:CanActivateFn=async(_,state)=>{
  const auth=inject(Auth),router=inject(Router),demo=inject(LocalDemo);
  if(!auth.token())return router.createUrlTree(['/login'],{queryParams:{returnUrl:state.url}});
  try {
    const account=await auth.account();
    if(auth.isCashier())return router.createUrlTree(['/cashier']);
    return account.roles.some(r=>['CASHIER','CURATOR','GUIDE','ADMINISTRATOR'].includes(r)) && await demo.enabled()
      ? true : router.createUrlTree(['/']);
  }catch{return router.createUrlTree(['/']);}
};
