export type Role = 'USER' | 'SELLER' | 'ADMIN';
export type ItemStatus = 'AVAILABLE' | 'LOCKED' | 'SWAPPED';
export type SwapStatus = 'PENDING' | 'ACCEPTED' | 'REJECTED' | 'CANCELLED' | 'COMPLETED';
export type PaymentStatus = 'CREATED' | 'HELD' | 'RELEASED' | 'REFUNDED' | 'FAILED';

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
  images?: string[];
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
  createdAt: string;
}
