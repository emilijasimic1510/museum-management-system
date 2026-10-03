import { ActivatedRouteSnapshot, BaseRouteReuseStrategy, RouteReuseStrategy } from '@angular/router';

/** Editors own drafts and pending requests for one record. Never carry them to another ID. */
export class RecordRouteReuseStrategy extends BaseRouteReuseStrategy {
  override shouldReuseRoute(future:ActivatedRouteSnapshot,current:ActivatedRouteSnapshot):boolean {
    if(future.data['reloadOnIdChange'] && future.paramMap.get('id')!==current.paramMap.get('id'))return false;
    return super.shouldReuseRoute(future,current);
  }
}
export const recordRouteReuseProvider={provide:RouteReuseStrategy,useClass:RecordRouteReuseStrategy};
