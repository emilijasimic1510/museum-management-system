import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { Auth } from './auth';
export function hasQrBoardRole(auth:Auth):boolean {
  return auth.currentAccount()?.roles.some(r=>['CURATOR','ADMINISTRATOR'].includes(r.replace(/^ROLE_/,'')))===true;
}
export const qrBoardGuard:CanActivateFn=async(_,state)=>{
  const auth=inject(Auth),router=inject(Router);
  if(!auth.token())return router.createUrlTree(['/login'],{queryParams:{returnUrl:state.url}});
  try {await auth.account();return hasQrBoardRole(auth)?true:router.createUrlTree(['/forbidden']);}
  catch {return router.createUrlTree(['/login'],{queryParams:{returnUrl:state.url}});}
};
