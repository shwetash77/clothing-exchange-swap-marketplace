import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

export const authGuard: CanActivateFn = () =>
  inject(AuthService).isLoggedIn() ? true : inject(Router).createUrlTree(['/login']);

/** Route-level RBAC: mirrors @PreAuthorize("hasAnyRole('SELLER','ADMIN')") on the backend. */
export const sellerGuard: CanActivateFn = () =>
  inject(AuthService).canSell() ? true : inject(Router).createUrlTree(['/items']);
