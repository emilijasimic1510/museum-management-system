import { translate } from './i18n';
import { inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { CanActivateFn, Router } from '@angular/router';
import { Auth } from './auth';
import { errorMessage } from './api';

export interface AdminRole {id:number;name:string;description:string|null}
export type EmployeeType='CURATOR'|'GUIDE'|'CASHIER'|'MANAGER'|'ADMINISTRATOR';
export interface Employee {
  id:number;accountId:number;museumId:number;firstName:string;lastName:string;phone:string|null;hiredOn:string;
  supervisorId:number|null;type:EmployeeType;specialization:string|null;primaryLanguage:string|null;counterLabel:string|null;reportingLevel:string|null;administrationLevel:string|null;
}
export const subtypeFields:Record<EmployeeType,{key:string;label:string}>={
  CURATOR:{key:'specialization',label:translate("uiText.specialization")},GUIDE:{key:'primaryLanguage',label:translate("uiText.primary.language")},
  CASHIER:{key:'counterLabel',label:translate("uiText.cash.desk.label")},MANAGER:{key:'reportingLevel',label:translate("uiText.reporting.level")},ADMINISTRATOR:{key:'administrationLevel',label:translate("uiText.administration.level")}
};
export const adminGuard:CanActivateFn=async(_,state)=>{
  const auth=inject(Auth),router=inject(Router);
  if(!auth.token())return router.createUrlTree(['/login'],{queryParams:{returnUrl:state.url}});
  try{await auth.account();return auth.isAdmin()?true:router.createUrlTree(['/forbidden']);}
  catch{return router.createUrlTree(['/login']);}
};
export class AdminState {
  busy=signal(false);error=signal('');notice=signal('');
  async run(action:()=>Promise<void>){if(this.busy())return;this.busy.set(true);this.error.set('');this.notice.set('');
    try{await action();}catch(e){this.error.set(e instanceof HttpErrorResponse&&e.status===409
      ?translate("uiText.the.change.could.not.be.saved.check.for.an.existing.email.profile.or.role.assi")
      :errorMessage(e));}finally{this.busy.set(false);}}
}
export const adminStyles=`.admin-actions{display:flex;gap:1rem;flex-wrap:wrap;align-items:center;margin:1rem 0}.admin-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:1rem}.admin-fields{border:0;padding:0;min-width:0}.admin-kpis{display:grid;grid-template-columns:repeat(auto-fit,minmax(170px,1fr));gap:1rem}.admin-kpis strong{font-size:2rem}.admin-confirm{border:2px solid #856539;padding:1rem;margin:1rem 0}.admin-table{overflow:auto}table{width:100%;border-collapse:collapse;text-align:left}th,td{padding:.8rem;border-bottom:1px solid #ddd;overflow-wrap:anywhere}.admin-break{overflow-wrap:anywhere}@media(max-width:650px){.admin-grid{grid-template-columns:1fr}.admin-actions .button{width:100%}}`;
