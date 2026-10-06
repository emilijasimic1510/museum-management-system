import { translate } from './../core/i18n';
import { Component, effect, input, output, signal } from '@angular/core';
import { toDataURL } from 'qrcode';
import { scanUrl } from '../core/scan';

@Component({selector:'app-participation-qr',template:`
@if(image()){<img [src]="image()" [alt]="label()" width="240" height="240" style="max-width:100%;height:auto" (load)="ready.emit(true)" (error)="ready.emit(false)">}
@if(failed()){<p class="muted">{{failureText()}}</p>}
`})
export class ParticipationQr {
  token=input.required<string>();image=signal('');failed=signal(false);
  label=input(translate("uiText.exhibit.installation.qr.code"));failureText=input(translate("uiText.qr.image.unavailable.use.the.token.in.the.field"));ready=output<boolean>();
  constructor(){effect(onCleanup=>{
    let active=true;onCleanup(()=>active=false);this.image.set('');this.failed.set(false);this.ready.emit(false);
    void toDataURL(scanUrl(this.token()),{width:240,margin:4,errorCorrectionLevel:'M'})
      .then(value=>{if(active)this.image.set(value);}).catch(()=>{if(active)this.failed.set(true);});
  });}
}
