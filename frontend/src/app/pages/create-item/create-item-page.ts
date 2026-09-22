import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { CATEGORIES, CONDITIONS, SIZES } from '../../core/config';
import { errorMessage } from '../../core/errors';
import { ToastService } from '../../core/toast.service';

@Component({
  selector: 'app-create-item-page',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './create-item-page.html',
})
export class CreateItemPage {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(ApiService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);

  readonly categories = CATEGORIES;
  readonly sizes = SIZES;
  readonly conditions = CONDITIONS;
  readonly submitting = signal(false);
  readonly error = signal('');

  readonly form = this.fb.nonNullable.group({
    title: ['', [Validators.required, Validators.maxLength(120)]],
    description: ['', Validators.maxLength(2000)],
    category: ['', Validators.required],
    size: ['', Validators.required],
    condition: ['', Validators.required],
    images: [''], // one image URL per line
  });

  submit() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    const images = v.images.split('\n').map((s) => s.trim()).filter(Boolean);

    this.submitting.set(true);
    this.error.set('');
    this.api.createItem({ ...v, images }).subscribe({
      next: () => {
        this.toast.success('Your item is now listed.');
        this.router.navigate(['/items']);
      },
      error: (err) => {
        this.submitting.set(false);
        this.error.set(errorMessage(err));
      },
    });
  }
}
