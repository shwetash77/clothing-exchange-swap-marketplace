import { Component, computed, input } from '@angular/core';
import { PaymentStatus } from '../core/models';

/** Visualises the escrow lifecycle: created -> held -> released | refunded. */
@Component({
  selector: 'app-payment-timeline',
  template: `
    <ol class="timeline">
      @for (step of steps(); track step.label) {
        <li [class.done]="step.done" [class.current]="step.current" [class.bad]="step.bad">
          <span class="dot"></span>{{ step.label }}
        </li>
      }
    </ol>
  `,
})
export class PaymentTimeline {
  status = input.required<PaymentStatus>();

  steps = computed(() => {
    const s = this.status();
    const rank = { CREATED: 0, HELD: 1, RELEASED: 2, REFUNDED: 2, FAILED: 0 }[s];
    const last = s === 'REFUNDED' ? 'Deposit refunded' : 'Deposit released';
    return [
      { label: 'Deposit requested', done: rank >= 0, current: s === 'CREATED' || s === 'FAILED', bad: s === 'FAILED' },
      { label: 'Funds held in escrow', done: rank >= 1, current: s === 'HELD', bad: false },
      { label: last, done: rank >= 2, current: rank === 2, bad: s === 'REFUNDED' },
    ];
  });
}
