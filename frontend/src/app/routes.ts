import { Routes } from '@angular/router';
import { authGuard, cashierGuard } from './core/auth';
import { demoBoardGuard } from './core/scan';
export const routes:Routes=[
  {path:'scan',canActivate:[authGuard],loadComponent:()=>import('./pages/scan').then(m=>m.ScanPage)},
  {path:'demo-qr',canActivate:[demoBoardGuard],loadComponent:()=>import('./pages/demo-qr-board').then(m=>m.DemoQrBoard)},
  {path:'',loadComponent:()=>import('./pages/catalog').then(m=>m.CatalogPage)},
  {path:'exhibitions/:id',loadComponent:()=>import('./pages/exhibition').then(m=>m.ExhibitionPage)},
  {path:'login',loadComponent:()=>import('./pages/auth-page').then(m=>m.AuthPage)},
  {path:'register',loadComponent:()=>import('./pages/auth-page').then(m=>m.AuthPage)},
  {path:'profile',canActivate:[authGuard],loadComponent:()=>import('./pages/profile').then(m=>m.ProfilePage)},
  {path:'tickets',canActivate:[authGuard],loadComponent:()=>import('./pages/tickets').then(m=>m.TicketsPage)},
  {path:'visits',canActivate:[authGuard],loadComponent:()=>import('./pages/visits').then(m=>m.VisitsPage)},
  {path:'visits/:id',canActivate:[authGuard],loadComponent:()=>import('./pages/visit-detail').then(m=>m.VisitDetailPage)},
  {path:'recommendations',canActivate:[authGuard],loadComponent:()=>import('./pages/recommendations').then(m=>m.RecommendationsPage)},
  {path:'cashier',canActivate:[cashierGuard],loadComponent:()=>import('./pages/cashier').then(m=>m.CashierPage)},
  {path:'**',redirectTo:''}
];
