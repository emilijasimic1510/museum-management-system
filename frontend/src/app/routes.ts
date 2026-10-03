import { Routes } from '@angular/router';
import { visitorGuard, cashierGuard, curatorGuard, guideGuard } from './core/auth';
import { demoBoardGuard } from './core/scan';
import { collectionGuard } from './core/collection-management';
import { managerGuard } from './core/manager';
import { adminGuard } from './core/admin';
export const routes:Routes=[
  {path:'admin',canActivate:[adminGuard],canActivateChild:[adminGuard],children:[
    {path:'',pathMatch:'full',loadComponent:()=>import('./pages/admin-overview').then(m=>m.AdminOverview)},
    {path:'profile',data:{profile:true},loadComponent:()=>import('./pages/admin-overview').then(m=>m.AdminOverview)},
    {path:'roles',data:{kind:'roles'},loadComponent:()=>import('./pages/admin-lists').then(m=>m.AdminLists)},
    {path:'accounts',data:{kind:'accounts'},loadComponent:()=>import('./pages/admin-lists').then(m=>m.AdminLists)},
    {path:'accounts/new',data:{mode:'new'},loadComponent:()=>import('./pages/admin-account').then(m=>m.AdminAccount)},
    {path:'accounts/:id/edit',data:{mode:'edit',reloadOnIdChange:true},loadComponent:()=>import('./pages/admin-account').then(m=>m.AdminAccount)},
    {path:'accounts/:id',data:{mode:'view',reloadOnIdChange:true},loadComponent:()=>import('./pages/admin-account').then(m=>m.AdminAccount)},
    {path:'employees',data:{kind:'employees'},loadComponent:()=>import('./pages/admin-lists').then(m=>m.AdminLists)},
    {path:'employees/new',data:{mode:'new'},loadComponent:()=>import('./pages/admin-employee').then(m=>m.AdminEmployee)},
    {path:'employees/:id/edit',data:{mode:'edit',reloadOnIdChange:true},loadComponent:()=>import('./pages/admin-employee').then(m=>m.AdminEmployee)}
  ]},
  {path:'manager',canActivate:[managerGuard],loadComponent:()=>import('./pages/manager').then(m=>m.ManagerPage)},
  {path:'collection',canActivate:[collectionGuard],canActivateChild:[collectionGuard],children:[
    {path:'',pathMatch:'full',redirectTo:'exhibitions'},
    {path:'exhibitions/:id/prices',data:{reloadOnIdChange:true},loadComponent:()=>import('./pages/collection-prices').then(m=>m.CollectionPrices)},
    ...['museums','halls','exhibitions','exhibition-terms'].flatMap(kind=>[
      {path:kind,data:{kind,adminOnly:['museums','halls'].includes(kind)},loadComponent:()=>import('./pages/collection-list').then(m=>m.CollectionList)},
      {path:kind+'/new',data:{kind,mode:'new',adminOnly:['museums','halls'].includes(kind)},loadComponent:()=>import('./pages/collection-editor').then(m=>m.CollectionEditor)},
      {path:kind+'/:id/edit',data:{kind,mode:'edit',adminOnly:['museums','halls'].includes(kind),reloadOnIdChange:true},loadComponent:()=>import('./pages/collection-editor').then(m=>m.CollectionEditor)},
      {path:kind+'/:id',data:{kind,mode:'view',adminOnly:['museums','halls'].includes(kind),reloadOnIdChange:true},loadComponent:()=>import('./pages/collection-editor').then(m=>m.CollectionEditor)}
    ])
  ]},
  {path:'guide',canActivate:[guideGuard],canActivateChild:[guideGuard],children:[
    {path:'',pathMatch:'full',loadComponent:()=>import('./pages/guide-overview').then(m=>m.GuideOverview)},
    {path:'profile',loadComponent:()=>import('./pages/guide-overview').then(m=>m.GuideProfile)},
    {path:'registrations',loadComponent:()=>import('./pages/guide-registrations').then(m=>m.GuideRegistrations)},
    {path:'events',data:{guide:true},loadComponent:()=>import('./pages/events').then(m=>m.EventsPage)},
    {path:'events/new',loadComponent:()=>import('./pages/guide-event-editor').then(m=>m.GuideEventEditor)},
    {path:'events/:id/edit',data:{reloadOnIdChange:true},loadComponent:()=>import('./pages/guide-event-editor').then(m=>m.GuideEventEditor)},
    {path:'events/:id/registrations',data:{reloadOnIdChange:true},loadComponent:()=>import('./pages/guide-registrations').then(m=>m.GuideRegistrations)}
  ]},
  {path:'events',canActivate:[visitorGuard],data:{publicCatalog:true},loadComponent:()=>import('./pages/events').then(m=>m.EventsPage)},
  {path:'events/:id',canActivate:[visitorGuard],data:{publicCatalog:true,reloadOnIdChange:true},loadComponent:()=>import('./pages/event-detail').then(m=>m.EventDetail)},
  {path:'forbidden',loadComponent:()=>import('./pages/curator-overview').then(m=>m.ForbiddenPage)},
  {path:'curator',canActivate:[curatorGuard],canActivateChild:[curatorGuard],children:[
    {path:'',pathMatch:'full',loadComponent:()=>import('./pages/curator-overview').then(m=>m.CuratorOverview)},
    {path:'profile',loadComponent:()=>import('./pages/curator-overview').then(m=>m.CuratorProfile)},
    {path:'digital-content',loadComponent:()=>import('./pages/curator-content').then(m=>m.CuratorContent)},
    {path:'digital-content/new',data:{contentMode:'new'},loadComponent:()=>import('./pages/curator-content').then(m=>m.CuratorContent)},
    {path:'digital-content/:id/edit',data:{contentMode:'edit',reloadOnIdChange:true},loadComponent:()=>import('./pages/curator-content').then(m=>m.CuratorContent)},
    ...['exhibits','authors','participations'].flatMap(kind=>[
      {path:kind,data:{kind},loadComponent:()=>import('./pages/curator-list').then(m=>m.CuratorList)},
      {path:kind+'/new',data:{kind},loadComponent:()=>import('./pages/curator-editor').then(m=>m.CuratorEditor)},
      {path:kind+'/:id/edit',data:{kind,reloadOnIdChange:true},loadComponent:()=>import('./pages/curator-editor').then(m=>m.CuratorEditor)}
    ])
  ]},
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
  {path:'visits/:id',canActivate:[visitorGuard],data:{reloadOnIdChange:true},loadComponent:()=>import('./pages/visit-detail').then(m=>m.VisitDetailPage)},
  {path:'recommendations',canActivate:[visitorGuard],loadComponent:()=>import('./pages/recommendations').then(m=>m.RecommendationsPage)},
  {path:'cashier',canActivate:[cashierGuard],loadComponent:()=>import('./pages/cashier').then(m=>m.CashierPage)},
  {path:'**',redirectTo:''}
];

