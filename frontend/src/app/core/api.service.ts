import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { API_URL } from './config';
import { Item, ItemPayload, PaymentSummary, SwapRequest, ValueComparison } from './models';

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
  /** Swap value calculator: what is this worth, given its original price and condition? */
  estimateValue(originalPrice: number, condition: string) {
    let params = new HttpParams().set('originalPrice', originalPrice);
    if (condition) params = params.set('condition', condition);
    return this.http.get<{ estimatedValue: number | null }>(`${API_URL}/items/estimate`, { params });
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
  /** Owner declines a pending request, optionally with a note for the requester. */
  rejectSwap(id: number, reason: string) {
    return this.http.patch<SwapRequest>(`${API_URL}/swap-requests/${id}/reject`, { reason });
  }
  /** Value comparison before a request exists (null offeredItemId = deposit only). */
  valuePreview(itemId: number, offeredItemId: number | null) {
    let params = new HttpParams().set('itemId', itemId);
    if (offeredItemId !== null) params = params.set('offeredItemId', offeredItemId);
    return this.http.get<ValueComparison>(`${API_URL}/swap-requests/value-preview`, { params });
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
