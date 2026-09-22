import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Observable, forkJoin, take, timer } from 'rxjs';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { errorMessage } from '../../core/errors';
import { PaymentSummary, SwapRequest } from '../../core/models';
import { RazorpayService } from '../../core/razorpay.service';
import { ToastService } from '../../core/toast.service';
import { PaymentTimeline } from '../../shared/payment-timeline';
import { StatusBadge } from '../../shared/status-badge';

type Tab = 'sent' | 'received';

@Component({
  selector: 'app-my-swaps-page',
  imports: [FormsModule, RouterLink, DatePipe, CurrencyPipe, StatusBadge, PaymentTimeline],
  templateUrl: './my-swaps-page.html',
})
export class MySwapsPage implements OnInit {
  private readonly api = inject(ApiService);
  private readonly auth = inject(AuthService);
  private readonly razorpay = inject(RazorpayService);
  private readonly toast = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);

  readonly tab = signal<Tab>('sent');
  readonly sent = signal<SwapRequest[]>([]);
  readonly received = signal<SwapRequest[]>([]);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly busyId = signal<number | null>(null);
  readonly deposit = signal(200); // deposit amount in INR

  ngOnInit() {
    this.load();
  }

  load(silent = false) {
    if (!silent) this.loading.set(true);
    forkJoin({ sent: this.api.sentSwaps(), received: this.api.receivedSwaps() }).subscribe({
      next: ({ sent, received }) => {
        this.sent.set(sent);
        this.received.set(received);
        this.loading.set(false);
        this.error.set('');
      },
      error: (err) => {
        this.error.set(errorMessage(err));
        this.loading.set(false);
      },
    });
  }

  // ---- owner actions ----
  accept(s: SwapRequest) {
    // A 409 here means someone else got the item first (optimistic locking on the backend).
    this.run(s, this.api.acceptSwap(s.id), 'Swap accepted. The item is now reserved for this swap.');
  }
  cancel(s: SwapRequest) {
    const msg = this.isMine(s) ? 'Swap request cancelled.' : 'Request declined.';
    this.run(s, this.api.cancelSwap(s.id), msg);
  }

  // ---- escrow actions ----
  confirmComplete(s: SwapRequest) {
    this.run(s, this.api.releaseDeposit(s.id), 'Swap complete. The deposit has been released.');
  }
  reportProblem(s: SwapRequest) {
    this.run(s, this.api.refundDeposit(s.id), 'Deposit refunded and the item is back on the marketplace.');
  }

  startPayment(s: SwapRequest) {
    // Resume an unfinished checkout instead of creating a second order for the same swap.
    if (s.payment?.status === 'CREATED') {
      this.checkout(s, s.payment);
      return;
    }
    if (!this.deposit() || this.deposit() <= 0) {
      this.toast.error('Enter a deposit amount greater than zero.');
      return;
    }
    this.busyId.set(s.id);
    this.api.createDeposit(s.id, this.deposit()).subscribe({
      next: (payment) => {
        this.busyId.set(null);
        this.checkout(s, payment);
      },
      error: (err) => {
        this.busyId.set(null);
        this.toast.error(errorMessage(err));
      },
    });
  }

  isMine(s: SwapRequest) {
    return s.requester.id === this.auth.userId();
  }

  private async checkout(s: SwapRequest, payment: PaymentSummary) {
    try {
      await this.razorpay.openCheckout({
        orderId: payment.orderId,
        amountRupees: payment.amount,
        description: `Deposit for swap of "${s.item.title}"`,
        payerName: this.auth.name(),
      });
      this.toast.success('Payment authorised. Waiting for confirmation from Razorpay…');
      this.pollUntilHeld();
    } catch (e) {
      this.toast.error(e instanceof Error ? e.message : 'Payment failed.');
      this.load(true);
    }
  }

  /**
   * Razorpay tells the backend about the authorised payment via webhook, so the
   * UI re-fetches a few times until the status flips from CREATED to HELD.
   */
  private pollUntilHeld() {
    timer(1500, 2500)
      .pipe(take(8), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.load(true));
  }

  private run(s: SwapRequest, call$: Observable<unknown>, successMessage: string) {
    this.busyId.set(s.id);
    call$.subscribe({
      next: () => {
        this.busyId.set(null);
        this.toast.success(successMessage);
        this.load(true);
      },
      error: (err) => {
        this.busyId.set(null);
        this.toast.error(errorMessage(err));
        this.load(true); // state may have changed under us, so refresh
      },
    });
  }
}
