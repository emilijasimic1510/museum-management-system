import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { Auth } from './auth';

export interface CountRow {key:string;label:string;count:number}
export interface MoneyRow {key:string;label:string;amount:number}
export interface ManagerDashboard {
  from:string;to:string;zone:string;generatedAt:string;
  attendance:{total:number;museums:CountRow[];exhibitions:CountRow[];terms:CountRow[];days:CountRow[]};
  revenue:{total:number;paidTicketValue:number;exhibitions:MoneyRow[];categories:MoneyRow[];methods:MoneyRow[];days:MoneyRow[]};
  tickets:CountRow[];
  popularity:{participationId:number;exhibitId:number;exhibit:string;exhibition:string;scans:number;visits:number;averageSeconds:number|null;measuredScans:number}[];
  events:{id:number;name:string;start:string;status:string;capacity:number;registrations:number;registered:number;attended:number;cancelled:number;occupancyPercent:number|null}[];
  ratings:{exhibitionId:number;exhibition:string;count:number;average:number}[];
  comments:{id:number;exhibition:string;rating:number;comment:string;ratedAt:string}[];commentsTruncated:boolean;
  transitions:CountRow[];
  contentInventory:{participationId:number;exhibit:string;exhibition:string;language:string|null;type:string;records:number}[];
  limitations:string[];
}
export const managerGuard:CanActivateFn=async(_,state)=>{
  const auth=inject(Auth),router=inject(Router);
  if(!auth.token())return router.createUrlTree(['/login'],{queryParams:{returnUrl:state.url}});
  try{await auth.account();return auth.isManager()?true:router.createUrlTree(['/forbidden']);}
  catch{return router.createUrlTree(['/login']);}
};
