import { Routes } from '@angular/router';
import { visitorGuard, cashierGuard } from './core/auth';
import { demoBoardGuard } from './core/scan';
export const routes:Routes=[
  {path:'exhibits',canActivate:[visitorGuard],data:{publicCatalog:true},loadComponent:()=>import('./pages/exhibits').then(m=>m.ExhibitsPage)},
  {path:'exhibits/:id',canActivate:[visitorGuard],data:{publicCatalog:true},loadComponent:()=>import('./pages/exhibit-detail').then(m=>m.ExhibitDetailPage)},
  {path:'authors',canActivate:[visitorGuard],data:{publicCatalog:true},loadComponent:()=>import('./pages/authors').then(m=>m.AuthorsPage)},
  {path:'authors/:id',canActivate:[visitorGuard],data:{publicCatalog:true},loadComponent:()=>import('./pages/author-detail').then(m=>m.AuthorDetailPage)},
  {path:'scan',canActivate:[visitorGuard],loadComponent:()=>import('./pages/scan').then(m=>m.ScanPage)},
  {path:'demo-qr',canActivate:[demoBoardGuard],loadComponent:()=>import('./pages/demo-qr-board').then(m=>m.DemoQrBoard)},
  {path:'',canActivate:[visitorGuard],data:{publicCatalog:true},loadComponent:()=>import('./pages/catalog').then(m=>m.CatalogPage)},
  {path:'exhibitions/:id',canActivate:[visitorGuard],data:{publicCatalog:true},loadComponent:()=>import('./pages/exhibition').then(m=>m.ExhibitionPage)},
  {path:'login',loadComponent:()=>import('./pages/auth-page').then(m=>m.AuthPage)},
  {path:'register',loadComponent:()=>import('./pages/auth-page').then(m=>m.AuthPage)},
  {path:'profile',canActivate:[visitorGuard],loadComponent:()=>import('./pages/profile').then(m=>m.ProfilePage)},
  {path:'tickets',canActivate:[visitorGuard],loadComponent:()=>import('./pages/tickets').then(m=>m.TicketsPage)},
  {path:'visits',canActivate:[visitorGuard],loadComponent:()=>import('./pages/visits').then(m=>m.VisitsPage)},
  {path:'visits/:id',canActivate:[visitorGuard],loadComponent:()=>import('./pages/visit-detail').then(m=>m.VisitDetailPage)},
  {path:'recommendations',canActivate:[visitorGuard],loadComponent:()=>import('./pages/recommendations').then(m=>m.RecommendationsPage)},
  {path:'cashier',canActivate:[cashierGuard],loadComponent:()=>import('./pages/cashier').then(m=>m.CashierPage)},
  {path:'**',redirectTo:''}
];

