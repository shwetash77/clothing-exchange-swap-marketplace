import { HttpErrorResponse } from '@angular/common/http';

/** Turns any HTTP failure into a message that is safe to show a user. */
export function errorMessage(err: unknown): string {
  if (err instanceof HttpErrorResponse) {
    if (err.status === 0) return 'Cannot reach the server. Is the backend running on port 8080?';
    if (err.error?.message) return err.error.message; // ApiError { timestamp, status, message }
    if (err.status === 403) return "You don't have permission to do that.";
    if (err.status === 401) return 'Please log in again.';
  }
  return 'Something went wrong. Please try again.';
}
