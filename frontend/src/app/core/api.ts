import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

@Injectable({providedIn:'root'})
export class Api {
  private http=inject(HttpClient);
  get<T>(path:string) { return firstValueFrom(this.http.get<T>('/api'+path)); }
  post<T>(path:string, body:unknown) { return firstValueFrom(this.http.post<T>('/api'+path,body)); }
  put<T>(path:string, body:unknown) { return firstValueFrom(this.http.put<T>('/api'+path,body)); }
}
export function errorMessage(error:unknown):string {
  if (!(error instanceof HttpErrorResponse)) return 'Zahtev nije uspeo. Pokušajte ponovo.';
  const details=error.error;
  const fields: string[]=Array.isArray(details?.errors) ? details.errors.map((e:{field:string;message:string})=>`${e.field}: ${e.message}`) : [];
  const fallback:Record<number,string>={0:'Backend nije dostupan. Proverite da li je pokrenut na portu 8081.',400:'Proverite unete podatke.',401:'Prijava nije važeća ili su email i lozinka netačni. Prijavite se ponovo.',403:'Vaš nalog nema pravo pristupa ovoj radnji.',404:'Traženi podatak nije pronađen.',409:'Radnja nije moguća zbog trenutnog stanja podataka.'};
  return [details?.detail || fallback[error.status] || `Greška servera (${error.status}). Pokušajte ponovo.`,...fields].join('\n');
}
export class PageState {
  readonly busy=signal(false); readonly error=signal(''); readonly notice=signal('');
  async run(action:()=>Promise<void>) {
    if(this.busy()) return;
    this.busy.set(true); this.error.set(''); this.notice.set('');
    try { await action(); } catch(error) { this.error.set(errorMessage(error)); } finally { this.busy.set(false); }
  }
}
