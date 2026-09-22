// All API calls go to /api, which the Angular dev server proxies to the
// Spring Boot backend on :8080 (see proxy.conf.json). This avoids CORS
// entirely in development.
export const API_URL = '/api';

// Public (publishable) Razorpay TEST key id. Safe to keep in frontend code;
// never put the key *secret* here.
export const RAZORPAY_KEY_ID = 'rzp_test_Tf08Fig0xeOkER';

export const CATEGORIES = ['Tops', 'Bottoms', 'Dresses', 'Outerwear', 'Ethnic wear', 'Shoes', 'Accessories'];
export const SIZES = ['XS', 'S', 'M', 'L', 'XL', 'XXL', 'Free size'];
export const CONDITIONS = ['New with tags', 'Like new', 'Good', 'Fair'];
