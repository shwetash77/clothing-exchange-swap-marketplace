import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { API_URL } from './config';
import { Item, ItemPayload, PaymentSummary, SwapRequest } from './models';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);

  // ---- items ----
  listItems(category?: string) {
    let params = new HttpParams();
    if (category) params = params.set('category', category);
    return this.http.get<Item[]>(`${API_URL}/items`, { params });
  }
  getItem(id: number) {
    return this.http.get<Item>(`${API_URL}/items/${id}`);
  }
  myItems() {
    return this.http.get<Item[]>(`${API_URL}/items/mine`);
  }
  createItem(body: ItemPayload) {
    return this.http.post<Item>(`${API_URL}/items`, body);
  }

  // ---- swap requests ----
  sentSwaps() {
    return this.http.get<SwapRequest[]>(`${API_URL}/swap-requests/mine`);
  }
  receivedSwaps() {
    return this.http.get<SwapRequest[]>(`${API_URL}/swap-requests/incoming`);
  }
  requestSwap(itemId: number, offeredItemId: number | null) {
    return this.http.post<SwapRequest>(`${API_URL}/swap-requests`, { itemId, offeredItemId });
  }
  acceptSwap(id: number) {
    return this.http.patch<SwapRequest>(`${API_URL}/swap-requests/${id}/accept`, {});
  }
  cancelSwap(id: number) {
    return this.http.patch<void>(`${API_URL}/swap-requests/${id}/cancel`, {});
  }

  // ---- escrow payments ----
  createDeposit(swapRequestId: number, amount: number) {
    return this.http.post<PaymentSummary>(`${API_URL}/payments/deposit`, { swapRequestId, amount });
  }
  releaseDeposit(swapRequestId: number) {
    return this.http.post<void>(`${API_URL}/payments/${swapRequestId}/release`, {});
  }
  refundDeposit(swapRequestId: number) {
    return this.http.post<void>(`${API_URL}/payments/${swapRequestId}/refund`, {});
  }
}
