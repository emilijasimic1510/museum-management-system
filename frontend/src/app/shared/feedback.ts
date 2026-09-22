import { Component, input } from '@angular/core';
@Component({selector:'app-feedback',template:`
  @if(busy()){<p class="loading" role="status">Učitavanje…</p>}
  @if(error()){<div class="message error" role="alert">{{error()}}</div>}
  @if(notice()){<div class="message success" role="status">{{notice()}}</div>}
`})
export class Feedback { busy=input(false); error=input(''); notice=input(''); }
