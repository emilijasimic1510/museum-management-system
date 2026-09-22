import { Routes } from '@angular/router';
import { authGuard } from './core/auth';
export const routes:Routes=[
  {path:'',loadComponent:()=>import('./pages/catalog').then(m=>m.CatalogPage)},
  {path:'exhibitions/:id',loadComponent:()=>import('./pages/exhibition').then(m=>m.ExhibitionPage)},
  {path:'login',loadComponent:()=>import('./pages/auth-page').then(m=>m.AuthPage)},
  {path:'register',loadComponent:()=>import('./pages/auth-page').then(m=>m.AuthPage)},
  {path:'profile',canActivate:[authGuard],loadComponent:()=>import('./pages/profile').then(m=>m.ProfilePage)},
  {path:'tickets',canActivate:[authGuard],loadComponent:()=>import('./pages/tickets').then(m=>m.TicketsPage)},
  {path:'visits',canActivate:[authGuard],loadComponent:()=>import('./pages/visits').then(m=>m.VisitsPage)},
  {path:'visits/:id',canActivate:[authGuard],loadComponent:()=>import('./pages/visit-detail').then(m=>m.VisitDetailPage)},
  {path:'recommendations',canActivate:[authGuard],loadComponent:()=>import('./pages/recommendations').then(m=>m.RecommendationsPage)},
  {path:'**',redirectTo:''}
];
