import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AnalyticsService, DashboardAnalytics } from '../../services/analytics.service';
import { QueueService } from '../../services/queue.service';

@Component({
  selector: 'app-analytics-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './analytics-dashboard.html',
  styleUrl: './analytics-dashboard.css'
})
export class AnalyticsDashboardComponent implements OnInit {
  analytics = signal<DashboardAnalytics | null>(null);
  loading = signal<boolean>(true);
  error = signal<string | null>(null);

  drainEventId = signal<number>(1);
  drainBatchSize = signal<number>(10);
  drainMessage = signal<string | null>(null);

  constructor(
    private analyticsService: AnalyticsService,
    private queueService: QueueService
  ) {}

  ngOnInit(): void {
    this.loadMetrics();
  }

  loadMetrics(): void {
    this.loading.set(true);
    this.analyticsService.getDashboardMetrics().subscribe({
      next: (data) => {
        this.analytics.set(data);
        this.loading.set(false);
      },
      error: (err) => {
        console.error('Failed to load dashboard metrics', err);
        this.error.set('Could not load executive analytics data.');
        this.loading.set(false);
      }
    });
  }

  triggerQueueDrain(eventId: number, batchSize: number): void {
    this.queueService.drainQueue(eventId, batchSize).subscribe({
      next: (res) => {
        this.drainMessage.set(`Successfully admitted ${res.admittedCount} waiting fans for Event #${eventId}!`);
        this.loadMetrics();
        setTimeout(() => this.drainMessage.set(null), 4000);
      },
      error: (err) => {
        console.error('Failed to drain queue', err);
        alert('Failed to trigger queue admission drain.');
      }
    });
  }
}
