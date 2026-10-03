import { Component, effect, input, signal } from '@angular/core';
import { toDataURL } from 'qrcode';

@Component({selector:'app-participation-qr',template:`
@if(image()){<img [src]="image()" alt="QR oznaka učešća eksponata" width="240" height="240" style="max-width:100%;height:auto">}
@if(failed()){<p class="muted">QR slika nije dostupna. Koristite token iz polja.</p>}
`})
export class ParticipationQr {
  token=input.required<string>();image=signal('');failed=signal(false);
  constructor(){effect(onCleanup=>{
    let active=true;onCleanup(()=>active=false);this.image.set('');this.failed.set(false);
    void toDataURL(this.token(),{width:240,margin:4,errorCorrectionLevel:'M'})
      .then(value=>{if(active)this.image.set(value);}).catch(()=>{if(active)this.failed.set(true);});
  });}
}
