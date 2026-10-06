import { Pipe, PipeTransform, inject } from '@angular/core';
import { I18n } from '../core/i18n';
@Pipe({name:'status',pure:false})
export class StatusPipe implements PipeTransform {private i18n=inject(I18n);transform(value:string):string{this.i18n.language();return this.i18n.status(value);}}
