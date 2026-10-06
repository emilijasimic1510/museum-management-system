import { translate } from './i18n';
import { inject, Injectable, isDevMode } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { Api } from './api';
import { Auth } from './auth';
import { publicBaseUrl } from './runtime-config';

export interface DemoQr { participationId:number; name:string; token:string; exhibitionId:number; exhibitionName:string }
export function scanToken(input:string):string {
  const value=input.trim();
  if(!value) throw new Error(translate("uiText.enter.a.qr.token.or.url"));
  if(value.startsWith('/') || /^https?:/i.test(value)) {
    const url=new URL(value,window.location.origin);
    if(url.origin!==window.location.origin || !/^\/scan(?:\/[^/]+)?$/.test(url.pathname) || !(url.searchParams.get('token') || url.pathname.split('/')[2]))
      throw new Error(translate("uiText.the.qr.url.must.belong.to.this.museum.and.contain.an.exhibit.token"));
    return url.searchParams.get('token') || decodeURIComponent(url.pathname.split('/')[2]);
  }
  if(value.length>512 || /\s/.test(value)) throw new Error(translate("uiText.the.qr.token.is.invalid"));
  return value;
}
export function scanUrl(token:string):string { return `${publicBaseUrl()}/scan/${encodeURIComponent(token)}`; }
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
