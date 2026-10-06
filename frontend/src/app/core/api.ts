import { translate } from './i18n';
import { HttpClient, HttpContext, HttpContextToken, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom, timeout } from 'rxjs';
import { apiUrl } from './runtime-config';

export const PUBLIC_CATALOG_REQUEST=new HttpContextToken(()=>false);

@Injectable({providedIn:'root'})
export class Api {
  private http=inject(HttpClient);
  get<T>(path:string, wait?:number) { const request=this.http.get<T>(apiUrl(path)); return firstValueFrom(wait===undefined?request:request.pipe(timeout(wait))); }
  blob(path:string) { return firstValueFrom(this.http.get(apiUrl(path),{responseType:'blob'}).pipe(timeout(45000))); }
  publicGet<T>(path:string) { return firstValueFrom(this.http.get<T>(apiUrl(path),{context:new HttpContext().set(PUBLIC_CATALOG_REQUEST,true)})); }
  post<T>(path:string, body:unknown, wait?:number) { const request=this.http.post<T>(apiUrl(path),body); return firstValueFrom(wait===undefined?request:request.pipe(timeout(wait))); }
  put<T>(path:string, body:unknown) { return firstValueFrom(this.http.put<T>(apiUrl(path),body)); }
  delete(path:string) { return firstValueFrom(this.http.delete<void>(apiUrl(path))); }
}
export function errorMessage(error:unknown):string {
  if (!(error instanceof HttpErrorResponse)) return translate("uiText.the.request.failed.please.try.again");
  const fallback:Record<number,string>={0:translate("uiText.the.server.is.unavailable.check.your.connection.and.try.again"),400:translate("uiText.check.the.entered.values"),401:translate("uiText.your.session.is.invalid.or.the.email.and.password.are.incorrect.please.sign.in"),403:translate("uiText.your.account.does.not.have.permission.for.this.action"),404:translate("uiText.the.requested.information.was.not.found"),409:translate("uiText.the.ticket.is.invalid.used.expired.or.does.not.belong.to.the.selected.time.slo")};
  // Do not echo backend details: they may be untranslated or contain submitted secrets.
  return error.status===409?translate('error.conflict'):(fallback[error.status] || translate('error.server')+' ('+error.status+')');
}
export class PageState {
  readonly busy=signal(false); readonly error=signal(''); readonly notice=signal('');
  async run(action:()=>Promise<void>) {
    if(this.busy()) return;
    this.busy.set(true); this.error.set(''); this.notice.set('');
    try { await action(); } catch(error) { this.error.set(errorMessage(error)); } finally { this.busy.set(false); }
  }
}
