import { Injectable } from '@angular/core';
import { RAZORPAY_KEY_ID } from './config';

interface CheckoutOptions {
  key: string;
  order_id: string;
  amount: number; // paise
  currency: 'INR';
  name: string;
  description: string;
  prefill?: { name?: string };
  theme?: { color: string };
  handler: (res: { razorpay_payment_id: string }) => void;
  modal: { ondismiss: () => void };
}
interface RazorpayInstance {
  open(): void;
  on(event: 'payment.failed', cb: (res: { error: { description: string } }) => void): void;
}
declare global {
  interface Window {
    Razorpay?: new (options: CheckoutOptions) => RazorpayInstance;
  }
}

@Injectable({ providedIn: 'root' })
export class RazorpayService {
  private scriptReady?: Promise<void>;

  /**
   * Opens Razorpay Checkout for an order the backend already created with
   * payment_capture = 0, so a successful payment is only *authorised* (held).
   * The backend learns about it via the payment.authorized webhook.
   */
  async openCheckout(order: { orderId: string; amountRupees: number; description: string; payerName: string }) {
    await this.loadScript();
    return new Promise<string>((resolve, reject) => {
      const checkout = new window.Razorpay!({
        key: RAZORPAY_KEY_ID,
        order_id: order.orderId,
        amount: Math.round(order.amountRupees * 100),
        currency: 'INR',
        name: 'SwapMarket',
        description: order.description,
        prefill: { name: order.payerName },
        theme: { color: '#b4532a' },
        handler: (res) => resolve(res.razorpay_payment_id),
        modal: { ondismiss: () => reject(new Error('Payment window closed before completing.')) },
      });
      checkout.on('payment.failed', (res) => reject(new Error(res.error.description)));
      checkout.open();
    });
  }

  private loadScript(): Promise<void> {
    if (window.Razorpay) return Promise.resolve();
    this.scriptReady ??= new Promise<void>((resolve, reject) => {
      const s = document.createElement('script');
      s.src = 'https://checkout.razorpay.com/v1/checkout.js';
      s.onload = () => resolve();
      s.onerror = () => {
        this.scriptReady = undefined;
        reject(new Error('Could not load Razorpay Checkout. Check your internet connection.'));
      };
      document.head.appendChild(s);
    });
    return this.scriptReady;
  }
}
