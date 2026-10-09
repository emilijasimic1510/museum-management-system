import { ChangeDetectorRef, Component, ElementRef, HostListener, Injectable, OnDestroy, ViewChild, inject, output, signal } from '@angular/core';
import { TranslatePipe } from '../core/i18n';
@Injectable({providedIn:'root'})
export class QrDecoder {
  async start(stream:MediaStream,video:HTMLVideoElement,found:(text:string)=>void) {
    const {BrowserQRCodeReader}=await import('@zxing/browser');
    return new BrowserQRCodeReader().decodeFromStream(stream,video,result=>{if(result)found(result.getText());});
  }
}
@Component({selector:'app-qr-camera',imports:[TranslatePipe],template:`
<button type="button" class="button" (click)="opened()?stop():start()">{{(opened()?'uiText.close.camera':'visitor.scanExhibit')|t}}</button>
@if(opened()){<video #video autoplay muted playsinline [attr.aria-label]="'camera.preview'|t"></video><p role="status">{{(starting()?'camera.starting':'uiText.point.camera.at.exhibit.qr')|t}}</p>}
@if(error()){<p class="message error" role="alert">{{error()|t}}</p>}
`,styles:[`video{display:block;width:100%;max-width:560px;max-height:60vh;object-fit:contain;margin-top:1rem;background:#17231e;border-radius:12px}`]})
export class QrCamera implements OnDestroy {
  decoded=output<string>();opened=signal(false);starting=signal(false);error=signal('');
  @ViewChild('video') video?:ElementRef<HTMLVideoElement>;
  private decoder=inject(QrDecoder);private change=inject(ChangeDetectorRef);
  private stream?:MediaStream;private controls?:{stop():void};private generation=0;
  async start(){
    this.error.set('');
    if(!window.isSecureContext){this.error.set('camera.https');return;}
    if(!navigator.mediaDevices?.getUserMedia){this.error.set('camera.unavailable');return;}
    this.stop();const generation=this.generation;this.opened.set(true);this.starting.set(true);this.change.detectChanges();
    try {
      const stream=await navigator.mediaDevices.getUserMedia({video:{facingMode:{ideal:'environment'}},audio:false});
      if(generation!==this.generation){stream.getTracks().forEach(t=>t.stop());return;}
      this.stream=stream;
      const controls=await this.decoder.start(stream,this.video!.nativeElement,text=>{
        if(generation!==this.generation)return;
        this.stop();this.decoded.emit(text);
      });
      if(generation!==this.generation){controls.stop();return;}
      this.controls=controls;this.starting.set(false);
    } catch(error){
      if(generation!==this.generation)return;
      this.stop();const name=(error as {name?:string})?.name;
      this.error.set(name==='NotAllowedError'?'camera.denied':name==='NotFoundError'?'camera.missing':'camera.unavailable');
    }
  }
  stop(){this.generation++;this.controls?.stop();this.controls=undefined;this.stream?.getTracks().forEach(t=>t.stop());this.stream=undefined;this.opened.set(false);this.starting.set(false);}
  @HostListener('document:visibilitychange') hidden(){if(document.hidden)this.stop();}
  ngOnDestroy(){this.stop();}
}
