import { Component, input } from '@angular/core';

const LABELS: Record<string, string> = {
  AVAILABLE: 'Available', LOCKED: 'Reserved', SWAPPED: 'Swapped',
  PENDING: 'Pending', ACCEPTED: 'Accepted', REJECTED: 'Rejected', CANCELLED: 'Cancelled', COMPLETED: 'Completed',
  CREATED: 'Awaiting payment', HELD: 'Funds held', RELEASED: 'Released', REFUNDED: 'Refunded', FAILED: 'Failed',
};

@Component({
  selector: 'app-status-badge',
  template: `<span class="badge" [class]="'badge ' + status()">{{ label() }}</span>`,
})
export class StatusBadge {
  status = input.required<string>();
  label = () => LABELS[this.status()] ?? this.status();
}
