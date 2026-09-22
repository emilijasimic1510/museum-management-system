import { Pipe, PipeTransform } from '@angular/core';
@Pipe({name:'status'})
export class StatusPipe implements PipeTransform {
  transform(value:string):string { return ({OTVOREN:'Otvoren',POPUNJEN:'Popunjen',OTKAZAN:'Otkazan',ZAVRSEN:'Završen',PLANIRANA:'Planirana',AKTIVNA:'Aktivna',ZAVRSENA:'Završena',OTKAZANA:'Otkazana',KUPLJENA:'Kupljena',ISKORISCENA:'Iskorišćena',ISTEKLA:'Istekla',KOMBINACIJA:'Kombinovana',INTERESOVANJA:'Interesovanja',BLIZINA:'Blizina',POPULARNOST:'Popularnost'} as Record<string,string>)[value]||value; }
}
