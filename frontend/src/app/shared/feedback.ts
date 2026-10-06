import { TranslatePipe } from './../core/i18n';
import { Component, input } from '@angular/core';
@Component({selector:'app-feedback',imports:[TranslatePipe],template:`
  @if(busy()){<p class="loading" role="status">{{'uiText.loading'|t}}</p>}
  @if(error()){<div class="message error" role="alert">{{error()|t}}</div>}
  @if(notice()){<div class="message success" role="status">{{notice()|t}}</div>}
`})
export class Feedback { busy=input(false); error=input(''); notice=input(''); }
