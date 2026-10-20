import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Subject, catchError, of, switchMap } from 'rxjs';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { errorMessage } from '../../core/errors';
import { Item, ValueComparison } from '../../core/models';
import { ToastService } from '../../core/toast.service';
import { StatusBadge } from '../../shared/status-badge';
import { ValueComparisonBox } from '../../shared/value-comparison';

@Component({
  selector: 'app-item-detail-page',
  imports: [FormsModule, RouterLink, DatePipe, CurrencyPipe, StatusBadge, ValueComparisonBox],
  templateUrl: './item-detail-page.html',
})
export class ItemDetailPage implements OnInit {
  private readonly api = inject(ApiService);
  private readonly auth = inject(AuthService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);

  readonly item = signal<Item | null>(null);
  readonly myItems = signal<Item[]>([]);
  readonly offeredId = signal<number | null>(null);
  readonly preview = signal<ValueComparison | null>(null);
  readonly activeImage = signal(0);
  readonly error = signal('');
  readonly submitting = signal(false);

  readonly isOwner = computed(() => this.item()?.owner.id === this.auth.userId());
  readonly canRequest = computed(() => this.item()?.status === 'AVAILABLE' && !this.isOwner());

  // Each change of the offered item asks the backend for a fresh value comparison.
  // switchMap drops a stale response if the person changes their mind quickly.
  private readonly previewRequests = new Subject<number | null>();

  constructor() {
    this.previewRequests
      .pipe(
        switchMap((offeredId) => {
          const item = this.item();
          if (!item) return of(null);
          return this.api.valuePreview(item.id, offeredId).pipe(catchError(() => of(null)));
        }),
        takeUntilDestroyed(),
      )
      .subscribe((comparison) => this.preview.set(comparison));
  }

  ngOnInit() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.api.getItem(id).subscribe({
      next: (item) => {
        this.item.set(item);
        if (item.owner.id !== this.auth.userId()) {
          this.loadMyItems();
          this.previewRequests.next(null);
        }
      },
      error: (err) => this.error.set(errorMessage(err)),
    });
  }

  onOfferChange(offeredId: number | null) {
    this.offeredId.set(offeredId);
    this.previewRequests.next(offeredId);
  }

  private loadMyItems() {
    this.api.myItems().subscribe({
      next: (items) => this.myItems.set(items.filter((i) => i.status === 'AVAILABLE')),
      error: () => this.myItems.set([]), // offering an item is optional, so fail quietly
    });
  }

  requestSwap() {
    const item = this.item();
    if (!item) return;
    this.submitting.set(true);
    this.api.requestSwap(item.id, this.offeredId()).subscribe({
      next: () => {
        this.toast.success('Swap request sent. The owner will be notified.');
        this.router.navigate(['/swaps']);
      },
      error: (err) => {
        this.submitting.set(false);
        this.toast.error(errorMessage(err));
      },
    });
  }
}
