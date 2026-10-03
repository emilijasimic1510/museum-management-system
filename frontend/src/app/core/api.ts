import { HttpClient, HttpContext, HttpContextToken, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom, timeout } from 'rxjs';

export const PUBLIC_CATALOG_REQUEST=new HttpContextToken(()=>false);

@Injectable({providedIn:'root'})
export class Api {
  private http=inject(HttpClient);
  get<T>(path:string, wait?:number) { const request=this.http.get<T>('/api'+path); return firstValueFrom(wait===undefined?request:request.pipe(timeout(wait))); }
  blob(path:string) { return firstValueFrom(this.http.get('/api'+path,{responseType:'blob'}).pipe(timeout(45000))); }
  publicGet<T>(path:string) { return firstValueFrom(this.http.get<T>('/api'+path,{context:new HttpContext().set(PUBLIC_CATALOG_REQUEST,true)})); }
  post<T>(path:string, body:unknown, wait?:number) { const request=this.http.post<T>('/api'+path,body); return firstValueFrom(wait===undefined?request:request.pipe(timeout(wait))); }
  put<T>(path:string, body:unknown) { return firstValueFrom(this.http.put<T>('/api'+path,body)); }
  delete(path:string) { return firstValueFrom(this.http.delete<void>('/api'+path)); }
}
export function errorMessage(error:unknown):string {
  if (!(error instanceof HttpErrorResponse)) return 'Zahtev nije uspeo. Pokušajte ponovo.';
  const fallback:Record<number,string>={0:'Backend nije dostupan. Proverite da li je pokrenut na portu 8081.',400:'Proverite unete podatke.',401:'Prijava nije važeća ili su email i lozinka netačni. Prijavite se ponovo.',403:'Vaš nalog nema pravo pristupa ovoj radnji.',404:'Traženi podatak nije pronađen.',409:'Ulaznica nije validna, već je iskorišćena, istekla je ili ne pripada izabranom terminu.'};
  // Do not echo backend details: they may be untranslated or contain submitted secrets.
  return fallback[error.status] || `Greška servera (${error.status}). Pokušajte ponovo.`;
}
export class PageState {
  readonly busy=signal(false); readonly error=signal(''); readonly notice=signal('');
  async run(action:()=>Promise<void>) {
    if(this.busy()) return;
    this.busy.set(true); this.error.set(''); this.notice.set('');
    try { await action(); } catch(error) { this.error.set(errorMessage(error)); } finally { this.busy.set(false); }
  }
}
