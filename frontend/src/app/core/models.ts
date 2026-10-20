export type Role = 'USER' | 'SELLER' | 'ADMIN';
export type ItemStatus = 'AVAILABLE' | 'LOCKED' | 'SWAPPED';
export type SwapStatus = 'PENDING' | 'ACCEPTED' | 'REJECTED' | 'CANCELLED' | 'COMPLETED';
export type PaymentStatus = 'CREATED' | 'HELD' | 'RELEASED' | 'REFUNDED' | 'FAILED';
export type ValueVerdict = 'FAIR' | 'OFFER_LOWER' | 'OFFER_HIGHER' | 'DEPOSIT_ONLY' | 'UNKNOWN';

export interface AuthResponse {
  token: string;
  role: Role;
  userId: number;
  name: string;
}

export interface SignupPayload {
  name: string;
  email: string;
  password: string;
  role: 'USER' | 'SELLER';
}

export interface UserSummary {
  id: number;
  name: string;
}

export interface Item {
  id: number;
  title: string;
  description: string | null;
  category: string | null;
  size: string | null;
  condition: string | null;
  /** Original retail price in INR, if the owner entered one. */
  originalPrice: number | null;
  /** Swap value calculator result in INR; null when there is no original price. */
  estimatedValue: number | null;
  images: string[];
  status: ItemStatus;
  owner: UserSummary;
  createdAt: string;
}

export interface ItemPayload {
  title: string;
  description?: string;
  category?: string;
  size?: string;
  condition?: string;
  originalPrice?: number;
  images?: string[];
}

export interface ValueComparison {
  requestedValue: number | null;
  offeredValue: number | null;
  /** offeredValue - requestedValue; negative means the offer is worth less. */
  difference: number | null;
  /** How much would even things out when the offer is the lower one. */
  suggestedTopUp: number | null;
  verdict: ValueVerdict;
}

export interface PaymentSummary {
  status: PaymentStatus;
  amount: number;
  orderId: string;
}

export interface SwapRequest {
  id: number;
  status: SwapStatus;
  item: Item;
  offeredItem: Item | null;
  requester: UserSummary;
  payment: PaymentSummary | null;
  valueComparison: ValueComparison;
  /** Note the owner left when rejecting the request, if any. */
  rejectionReason: string | null;
  createdAt: string;
}
