import { Component, computed, input } from '@angular/core';
import { ValueComparison } from '../core/models';

const inr = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 });
const money = (n: number | null) => (n === null ? '—' : inr.format(n));

/**
 * Side-by-side value of the requested and the offered item, with a plain-language
 * verdict. The numbers come from the backend swap value calculator, so this
 * component only formats them.
 */
@Component({
  selector: 'app-value-comparison',
  template: `
    <div class="value-box" [class]="'value-box ' + comparison().verdict">
      <div class="vb-head">{{ headline() }}</div>
      <p class="vb-detail">{{ detail() }}</p>
      @if (showFigures()) {
        <div class="vb-figures">
          <span>Requested item <strong>{{ requested() }}</strong></span>
          <span>Offered item <strong>{{ offered() }}</strong></span>
        </div>
      }
    </div>
  `,
})
export class ValueComparisonBox {
  comparison = input.required<ValueComparison>();

  requested = computed(() => money(this.comparison().requestedValue));
  offered = computed(() => money(this.comparison().offeredValue));
  showFigures = computed(() => this.comparison().verdict !== 'DEPOSIT_ONLY' || this.comparison().requestedValue !== null);

  headline = computed(() => {
    const c = this.comparison();
    switch (c.verdict) {
      case 'FAIR':
        return 'Looks like a fair swap';
      case 'OFFER_LOWER':
        return `The offered item is worth about ${money(c.suggestedTopUp)} less`;
      case 'OFFER_HIGHER':
        return `The offered item is worth about ${money(c.difference)} more`;
      case 'DEPOSIT_ONLY':
        return 'No item offered';
      default:
        return 'Value comparison unavailable';
    }
  });

  detail = computed(() => {
    switch (this.comparison().verdict) {
      case 'FAIR':
        return 'The two items are within 15% of each other in estimated value.';
      case 'OFFER_LOWER':
        return 'You may want to add another item or agree a top-up before accepting.';
      case 'OFFER_HIGHER':
        return 'The offer is more valuable than the item that was requested.';
      case 'DEPOSIT_ONLY':
        return 'Only a deposit is being offered, so there is no item to compare against.';
      default:
        return 'At least one of the items has no original price, so its value cannot be estimated.';
    }
  });
}
