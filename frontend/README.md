# SwapMarket UI (Angular)

Angular 20 frontend for the Clothing Exchange & Swap Marketplace Spring Boot backend.

**Features:** JWT login/signup · role-aware navigation (USER / SELLER / ADMIN) · browse & filter listings ·
item detail + swap request · create listing (SELLER/ADMIN only) · sent/received swap requests ·
escrow deposit via Razorpay Checkout with a payment-status timeline (created → held → released/refunded).

**Angular concepts used:** standalone components, signals, lazy-loaded routes, functional route guards
(auth + role), functional HTTP interceptor (JWT + session expiry), reactive forms with validation,
typed API service, new control flow (`@if` / `@for`).

## Run

1. Apply the backend changes to the Spring Boot repo (`git apply backend-changes.patch`) and start it:
   `docker compose up --build` (API on http://localhost:8080).
2. Put your Razorpay **test key id** in `src/app/core/config.ts` (`RAZORPAY_KEY_ID`).
3. Start the UI:

```bash
npm install
npm start        # http://localhost:4200  (API calls are proxied /api -> :8080)
```

Requires Node 20.19+ / 22.12+.

## Structure

```
src/app
├── core/      auth.service, auth.interceptor, guards, api.service, razorpay.service, models
├── shared/    status-badge, payment-timeline
└── pages/     auth, listings, item-detail, create-item, my-swaps
```

## Payments in local development

Razorpay tells the backend a payment was authorised through a **webhook**, which can't reach `localhost`.
Expose the backend with a tunnel (e.g. `ngrok http 8080`), add `https://<tunnel>/payments/webhook` in the
Razorpay dashboard (event `payment.authorized`, same secret as `RAZORPAY_WEBHOOK_SECRET`). After paying,
the UI polls a few times until the status changes to "Funds held".
