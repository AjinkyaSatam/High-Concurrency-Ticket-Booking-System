import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { EventService, EventResponse } from '../../services/event.service';

@Component({
  selector: 'app-events-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './events-list.html',
  styleUrl: './events-list.css'
})
export class EventsListComponent implements OnInit {
  events = signal<EventResponse[]>([]);
  loading = signal<boolean>(true);
  error = signal<string | null>(null);

  selectedCategory = '';
  searchQuery = '';
  selectedCity = '';

  categories = ['All', 'Concert', 'Conference', 'Orchestra', 'Sports', 'Theater'];

  constructor(private eventService: EventService) {}

  ngOnInit(): void {
    this.loadEvents();
  }

  loadEvents(): void {
    this.loading.set(true);
    this.error.set(null);

    const category = this.selectedCategory === 'All' ? '' : this.selectedCategory;

    this.eventService.searchEvents(category, this.selectedCity, this.searchQuery).subscribe({
      next: (data) => {
        this.events.set(data);
        this.loading.set(false);
      },
      error: (err) => {
        console.error('Failed to load events', err);
        this.error.set('Failed to connect to event service. Ensure backend is running.');
        this.loading.set(false);
      }
    });
  }

  onFilterChange(): void {
    this.loadEvents();
  }

  selectCategory(cat: string): void {
    this.selectedCategory = cat;
    this.loadEvents();
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'ON_SALE': return 'status-on-sale';
      case 'UPCOMING': return 'status-upcoming';
      case 'SOLD_OUT': return 'status-sold-out';
      case 'CANCELLED': return 'status-cancelled';
      default: return 'status-completed';
    }
  }

  formatStatus(status: string): string {
    switch (status) {
      case 'ON_SALE': return '● ON SALE';
      case 'UPCOMING': return 'UPCOMING';
      case 'SOLD_OUT': return 'SOLD OUT';
      case 'CANCELLED': return 'CANCELLED';
      default: return 'COMPLETED';
    }
  }
}
