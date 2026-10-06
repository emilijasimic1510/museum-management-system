import { Component, inject, input, output, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { Api } from '../core/api';
import { TranslatePipe } from '../core/i18n';
import { MediaImage } from './media-image';

@Component({selector:'app-image-editor',imports:[MediaImage,TranslatePipe],template:`
<section class="panel"><h2>{{(contextual()?'media.contextTitle':'media.publicTitle')|t}}</h2>
<p>{{(contextual()?'media.privateHint':'media.publicHint')|t}}</p>
@if(preview()){<app-media-image [path]="preview()" [protected]="true" [alt]="'media.preview'|t" [revision]="revision()" [contain]="true" />}
<label>{{'media.choose'|t}}<input type="file" accept="image/png,image/jpeg" [disabled]="busy()" (change)="choose($event)"></label>
<p class="muted">{{'media.limits'|t}}</p>
<button type="button" class="button" [disabled]="!file()||busy()" (click)="upload()">{{(busy()?'media.uploading':'media.upload')|t}}</button>
@if(message()){<p role="status">{{message()|t}}</p>}
</section>`})
export class ImageEditor {
  endpoint=input.required<string>();preview=input('');contextual=input(false);saved=output<number|null>();
  file=signal<File|null>(null);busy=signal(false);message=signal('');revision=signal(0);private api=inject(Api);
  choose(event:Event){const file=(event.target as HTMLInputElement).files?.[0]??null;this.message.set('');this.file.set(null);
    if(file&&!['image/png','image/jpeg'].includes(file.type)){this.message.set('media.invalid');return;}
    if(file&&file.size>5*1024*1024){this.message.set('media.large');return;}this.file.set(file);}
  async upload(){const file=this.file(),endpoint=this.endpoint();if(!file||this.busy())return;this.busy.set(true);this.message.set('');const body=new FormData();body.append('file',file);
    try{const result=await this.api.post<{contentId:number|null}>(endpoint,body);if(this.endpoint()!==endpoint)return;this.file.set(null);this.revision.update(v=>v+1);this.message.set('media.saved');this.saved.emit(result.contentId);}
    catch(e){if(this.endpoint()===endpoint)this.message.set(e instanceof HttpErrorResponse&&e.status===413?'media.large':'media.error');}finally{this.busy.set(false);}
  }
}
