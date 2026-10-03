import { signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { errorMessage } from './api';

// Public ExhibitDtos only. Staff, inventory and QR fields intentionally have no UI model.
export interface CatalogAuthor { id:number; name:string; contributionType:string|null }
export interface CatalogExhibit {
  id:number; name:string; description:string|null; creationPeriod:string|null;
  type:string|null; material:string|null; origin:string|null; authors:CatalogAuthor[];
  exhibitions:{id:number;name:string}[];
}
export interface CatalogParticipation {
  exhibition:{id:number;name:string}; exhibit:{id:number;name:string}; contextDescription:string|null;
}
export interface Author { id:number; firstName:string; lastName:string|null; birthYear:number|null; deathYear:number|null; biography:string|null }

/** Latest request wins when Angular reuses a detail component for another route id. */
export class CatalogResource<T> {
  readonly data=signal<T|null>(null); readonly busy=signal(false); readonly error=signal('');
  readonly notFound=signal(false); private revision=0;
  async load(action:()=>Promise<T>, missing='Traženi podatak nije pronađen.') {
    const revision=++this.revision;
    this.busy.set(true);this.error.set('');this.notFound.set(false);this.data.set(null);
    try { const data=await action();if(revision===this.revision)this.data.set(data); }
    catch(error) { if(revision!==this.revision)return;
      this.notFound.set(error instanceof HttpErrorResponse && error.status===404);
      this.error.set(this.notFound()?missing:errorMessage(error));
    } finally {if(revision===this.revision)this.busy.set(false);}
  }
}
export function validCatalogId(id:string):boolean { return /^[1-9]\d*$/.test(id); }
