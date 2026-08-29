import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ToastService } from '../../services/toast.service';

@Component({
  selector: 'app-toast-container',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="toast-container">
      <div *ngFor="let toast of toastService.toasts()" class="toast-item" [ngClass]="toast.type">
        <div class="toast-content">
          <strong *ngIf="toast.title" class="toast-title">{{ toast.title }}</strong>
          <span class="toast-message">{{ toast.message }}</span>
        </div>
        <button class="toast-close" (click)="toastService.dismiss(toast.id)">×</button>
      </div>
    </div>
  `,
  styles: [`
    .toast-container {
      position: fixed;
      top: 20px;
      right: 20px;
      z-index: 9999;
      display: flex;
      flex-direction: column;
      gap: 10px;
      max-width: 400px;
      width: 100%;
    }
    .toast-item {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 12px 16px;
      border-radius: 8px;
      color: #ffffff;
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
      animation: slideIn 0.3s ease-out;
      backdrop-filter: blur(10px);
    }
    .toast-item.success { background-color: #10b981; }
    .toast-item.error { background-color: #ef4444; }
    .toast-item.warning { background-color: #f59e0b; }
    .toast-item.info { background-color: #3b82f6; }
    .toast-content {
      display: flex;
      flex-direction: column;
      gap: 2px;
    }
    .toast-title { font-weight: 600; font-size: 0.9rem; }
    .toast-message { font-size: 0.85rem; opacity: 0.95; }
    .toast-close {
      background: none;
      border: none;
      color: #ffffff;
      font-size: 1.4rem;
      cursor: pointer;
      line-height: 1;
      padding: 0 4px;
      opacity: 0.8;
    }
    .toast-close:hover { opacity: 1; }
    @keyframes slideIn {
      from { transform: translateX(100%); opacity: 0; }
      to { transform: translateX(0); opacity: 1; }
    }
  `]
})
export class ToastContainerComponent {
  toastService = inject(ToastService);
}
