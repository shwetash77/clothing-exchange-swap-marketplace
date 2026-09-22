import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { tap } from 'rxjs';
import { API_URL } from './config';
import { AuthResponse, Role, SignupPayload } from './models';

const STORAGE_KEY = 'swap.session';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly session = signal<AuthResponse | null>(this.restore());

  readonly isLoggedIn = computed(() => this.session() !== null);
  readonly role = computed<Role | null>(() => this.session()?.role ?? null);
  readonly name = computed(() => this.session()?.name ?? '');
  readonly userId = computed(() => this.session()?.userId ?? null);
  /** Mirrors the backend rule: only SELLER / ADMIN may create listings. */
  readonly canSell = computed(() => this.role() === 'SELLER' || this.role() === 'ADMIN');

  token(): string | null {
    return this.session()?.token ?? null;
  }

  login(email: string, password: string) {
    return this.http
      .post<AuthResponse>(`${API_URL}/auth/login`, { email, password })
      .pipe(tap((res) => this.start(res)));
  }

  signup(payload: SignupPayload) {
    return this.http
      .post<AuthResponse>(`${API_URL}/auth/signup`, payload)
      .pipe(tap((res) => this.start(res)));
  }

  logout(sessionExpired = false) {
    this.session.set(null);
    localStorage.removeItem(STORAGE_KEY);
    this.router.navigate(['/login'], sessionExpired ? { queryParams: { expired: 1 } } : {});
  }

  isTokenExpired(): boolean {
    const token = this.token();
    return !token || this.expired(token);
  }

  private start(res: AuthResponse) {
    this.session.set(res);
    localStorage.setItem(STORAGE_KEY, JSON.stringify(res));
  }

  private restore(): AuthResponse | null {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      if (!raw) return null;
      const s = JSON.parse(raw) as AuthResponse;
      return this.expired(s.token) ? null : s;
    } catch {
      return null;
    }
  }

  private expired(token: string): boolean {
    try {
      const payload = JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')));
      return typeof payload.exp === 'number' && payload.exp * 1000 < Date.now();
    } catch {
      return true;
    }
  }
}
