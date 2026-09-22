import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { errorMessage } from '../../core/errors';

/** One component serves both /login and /signup (route data decides the mode). */
@Component({
  selector: 'app-auth-page',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './auth-page.html',
})
export class AuthPage {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  readonly isSignup = this.route.snapshot.data['mode'] === 'signup';
  readonly expired = this.route.snapshot.queryParamMap.has('expired');
  readonly submitting = signal(false);
  readonly error = signal('');
  readonly title = computed(() => (this.isSignup ? 'Create your account' : 'Welcome back'));

  readonly form = this.fb.nonNullable.group({
    name: ['', this.isSignup ? [Validators.required] : []],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(this.isSignup ? 8 : 1)]],
    role: ['USER' as 'USER' | 'SELLER'],
  });

  submit() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { name, email, password, role } = this.form.getRawValue();
    this.submitting.set(true);
    this.error.set('');

    const request$ = this.isSignup
      ? this.auth.signup({ name, email, password, role })
      : this.auth.login(email, password);

    request$.subscribe({
      next: () => this.router.navigate(['/items']),
      error: (err) => {
        this.submitting.set(false);
        this.error.set(err.status === 401 || err.status === 403 ? 'Invalid email or password.' : errorMessage(err));
      },
    });
  }
}
