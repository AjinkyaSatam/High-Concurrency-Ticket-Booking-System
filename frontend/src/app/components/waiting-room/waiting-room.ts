import { Component, OnInit, OnDestroy, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { QueueService, QueueStatusResponse } from '../../services/queue.service';
import { EventService, EventResponse } from '../../services/event.service';

@Component({
  selector: 'app-waiting-room',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './waiting-room.html',
  styleUrl: './waiting-room.css'
})
export class WaitingRoomComponent implements OnInit, OnDestroy {
  eventId = signal<number | null>(null);
  event = signal<EventResponse | null>(null);
  queueStatus = signal<QueueStatusResponse | null>(null);
  loading = signal<boolean>(true);
  error = signal<string | null>(null);
  
  private pollInterval: any;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private queueService: QueueService,
    private eventService: EventService
  ) {}

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('eventId');
    if (idParam) {
      const id = parseInt(idParam, 10);
      this.eventId.set(id);
      this.loadEvent(id);
      this.joinAndPollQueue(id);
    }
  }

  ngOnDestroy(): void {
    if (this.pollInterval) {
      clearInterval(this.pollInterval);
    }
  }

  loadEvent(eventId: number): void {
    this.eventService.getEventById(eventId).subscribe({
      next: (data) => this.event.set(data),
      error: (err) => console.error('Failed to fetch event', err)
    });
  }

  joinAndPollQueue(eventId: number): void {
    this.loading.set(true);
    this.queueService.joinQueue(eventId).subscribe({
      next: (status) => {
        this.queueStatus.set(status);
        this.loading.set(false);

        if (status.status === 'ADMITTED') {
          this.navigateToSeatMap();
          return;
        }

        // Start polling queue status
        this.startPolling(eventId, status.queueToken);
      },
      error: (err) => {
        console.error('Failed to join queue', err);
        this.error.set('Could not join Virtual Waiting Room. Please try again.');
        this.loading.set(false);
      }
    });
  }

  private startPolling(eventId: number, queueToken: string): void {
    this.pollInterval = setInterval(() => {
      this.queueService.getQueueStatus(eventId, queueToken).subscribe({
        next: (status) => {
          this.queueStatus.set(status);
          if (status.status === 'ADMITTED') {
            clearInterval(this.pollInterval);
            this.navigateToSeatMap();
          }
        },
        error: (err) => console.error('Error polling queue status', err)
      });
    }, 3000);
  }

  navigateToSeatMap(): void {
    if (this.eventId()) {
      setTimeout(() => {
        this.router.navigate(['/events', this.eventId()]);
      }, 1000);
    }
  }
}
