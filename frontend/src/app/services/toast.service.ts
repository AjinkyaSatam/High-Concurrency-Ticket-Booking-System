import { Injectable, signal } from '@angular/core';

export interface ToastMessage {
  id: string;
  type: 'success' | 'error' | 'info' | 'warning';
  title?: string;
  message: string;
}

@Injectable({
  providedIn: 'root'
})
export class ToastService {
  toasts = signal<ToastMessage[]>([]);

  show(message: string, type: 'success' | 'error' | 'info' | 'warning' = 'info', title?: string): void {
    const id = Math.random().toString(36).substring(2, 9);
    const toast: ToastMessage = { id, type, title, message };
    this.toasts.update(current => [...current, toast]);

    setTimeout(() => {
      this.dismiss(id);
    }, 5000);
  }

  success(message: string, title: string = 'Success'): void {
    this.show(message, 'success', title);
  }

  error(message: string, title: string = 'Error'): void {
    this.show(message, 'error', title);
  }

  warning(message: string, title: string = 'Warning'): void {
    this.show(message, 'warning', title);
  }

  info(message: string, title: string = 'Information'): void {
    this.show(message, 'info', title);
  }

  dismiss(id: string): void {
    this.toasts.update(current => current.filter(t => t.id !== id));
  }
}
