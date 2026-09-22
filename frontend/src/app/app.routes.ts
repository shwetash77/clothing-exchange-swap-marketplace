import { Routes } from '@angular/router';
import { authGuard, sellerGuard } from './core/guards';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'items' },
  {
    path: 'login',
    data: { mode: 'login' },
    loadComponent: () => import('./pages/auth/auth-page').then((m) => m.AuthPage),
  },
  {
    path: 'signup',
    data: { mode: 'signup' },
    loadComponent: () => import('./pages/auth/auth-page').then((m) => m.AuthPage),
  },
  {
    path: 'items',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/listings/listings-page').then((m) => m.ListingsPage),
  },
  {
    // must come before items/:id so "new" is not read as an id
    path: 'items/new',
    canActivate: [authGuard, sellerGuard],
    loadComponent: () => import('./pages/create-item/create-item-page').then((m) => m.CreateItemPage),
  },
  {
    path: 'items/:id',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/item-detail/item-detail-page').then((m) => m.ItemDetailPage),
  },
  {
    path: 'swaps',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/my-swaps/my-swaps-page').then((m) => m.MySwapsPage),
  },
  { path: '**', redirectTo: 'items' },
];
