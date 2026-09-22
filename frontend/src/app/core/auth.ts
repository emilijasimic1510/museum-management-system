import { Injectable, inject, signal } from '@angular/core';
import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { Api, PUBLIC_CATALOG_REQUEST } from './api';
import { Account, Registration, Token } from './models';

const KEY='museum.session';
interface Session { token:string; expiresAt:number }
export function safeReturnUrl(value:string|null):string {
  return value && /^\/(?!\/)/.test(value) && !value.includes('\\') && !value.startsWith('/login') && !value.startsWith('/register') ? value : '/profile';
}
@Injectable({providedIn:'root'})
export class Auth {
  private api=inject(Api); private router=inject(Router);
  readonly currentAccount=signal<Account|null>(null);
  private accountRequest:Promise<Account>|null=null;
  isCashier(){return this.currentAccount()?.roles.some(r=>r.replace(/^ROLE_/,'')==='CASHIER')===true;}
  isVisitor(){return !this.isCashier() && this.currentAccount()?.roles.some(r=>r.replace(/^ROLE_/,'')==='VISITOR')===true;}
  readonly session=signal<Session|null>(this.restore());
  private restore():Session|null {
    try { const s=JSON.parse(sessionStorage.getItem(KEY)||'null'); return s && typeof s.token==='string' && s.expiresAt>Date.now()?s:null; } catch { return null; }
  }
  token():string|null { const s=this.session(); if(s && s.expiresAt<=Date.now()) { this.clear(); return null; } return s?.token||null; }
  async login(email:string,password:string) {
    const result=await this.api.post<Token>('/auth/login',{email,password});
    this.clear();
    const s={token:result.accessToken,expiresAt:Date.now()+result.expiresIn*1000};
    sessionStorage.setItem(KEY,JSON.stringify(s)); this.session.set(s);
  }
  async account():Promise<Account> {
    if(this.currentAccount())return this.currentAccount()!;
    if(this.accountRequest)return this.accountRequest;
    const session=this.session();
    const request=this.api.get<Account>('/accounts/me').then(account=>{
      if(this.session()!==session)throw new Error('Sesija je promenjena.');
      this.currentAccount.set(account);return account;
    }).finally(()=>{if(this.accountRequest===request)this.accountRequest=null;});
    this.accountRequest=request;return request;
  }
  async hasRole(role:string):Promise<boolean> {
    try { return (await this.account()).roles.some(value=>String(value)===role || String(value)==='ROLE_'+role); }
    catch { return false; }
  }
  register(data:Registration) { return this.api.post<Account>('/auth/register',data); }
  clear() { sessionStorage.removeItem(KEY); this.session.set(null); this.currentAccount.set(null);this.accountRequest=null; }
  logout() { this.clear(); void this.router.navigateByUrl('/'); }
}
export const authGuard:CanActivateFn=(_,state)=>inject(Auth).token()?true:inject(Router).createUrlTree(['/login'],{queryParams:{returnUrl:state.url}});
export const cashierGuard:CanActivateFn=async(_,state)=>{
  const auth=inject(Auth), router=inject(Router);
  if(!auth.token()) return router.createUrlTree(['/login'],{queryParams:{returnUrl:state.url}});
  return await auth.hasRole('CASHIER') ? true : router.createUrlTree(['/']);
};
export const visitorGuard:CanActivateFn=async(route,state)=>{
  const auth=inject(Auth),router=inject(Router);
  if(!auth.token())return route.data?.['publicCatalog'] ? true : router.createUrlTree(['/login'],{queryParams:{returnUrl:state.url}});
  try{await auth.account();return auth.isCashier()?router.createUrlTree(['/cashier']):auth.isVisitor()?true:router.createUrlTree(['/login']);}
  catch{return router.createUrlTree(['/login']);}
};
export const authInterceptor:HttpInterceptorFn=(request,next)=> {
  const auth=inject(Auth); const router=inject(Router);
  const apiRequest=request.url.startsWith('/api/');
  const anonymous=request.url.startsWith('/api/auth/') || request.context.get(PUBLIC_CATALOG_REQUEST);
  const token=apiRequest&&!anonymous?auth.token():null;
  return next(token?request.clone({setHeaders:{Authorization:`Bearer ${token}`}}):request).pipe(catchError((error:unknown)=> {
    if(error instanceof HttpErrorResponse && error.status===401 && token) {
      auth.clear(); void router.navigate(['/login'],{queryParams:{expired:1,returnUrl:router.url}});
    }
    return throwError(()=>error);
  }));
};
