import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { VenueService, VenueResponse, VenueRequest } from '../../services/venue.service';
import { EventService, EventResponse, EventRequest, EventStatus } from '../../services/event.service';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-admin-events',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-events.html',
  styleUrl: './admin-events.css'
})
export class AdminEventsComponent implements OnInit {
  activeTab = signal<'venues' | 'events'>('events');

  venues = signal<VenueResponse[]>([]);
  events = signal<EventResponse[]>([]);

  loading = signal<boolean>(true);
  message = signal<string | null>(null);
  error = signal<string | null>(null);

  // New Venue Form
  venueForm: VenueRequest = {
    name: '',
    city: '',
    address: '',
    capacity: 1000,
    description: ''
  };

  // New Event Form
  eventForm: EventRequest = {
    title: '',
    description: '',
    category: 'Concert',
    eventDate: new Date(Date.now() + 86400000 * 7).toISOString().slice(0, 16),
    status: 'ON_SALE',
    bannerUrl: '',
    venueId: 0,
    totalSeats: 1000
  };

  showVenueModal = false;
  showEventModal = false;

  constructor(
    private venueService: VenueService,
    private eventService: EventService,
    public authService: AuthService
  ) {}

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.loading.set(true);
    this.error.set(null);

    this.venueService.getAllVenues().subscribe({
      next: (venues) => {
        this.venues.set(venues);
        if (venues.length > 0 && !this.eventForm.venueId) {
          this.eventForm.venueId = venues[0].id;
        }
        this.loadEvents();
      },
      error: (err) => {
        this.error.set('Failed to load venues. Verify backend authentication & CORS.');
        this.loading.set(false);
      }
    });
  }

  loadEvents(): void {
    this.eventService.searchEvents().subscribe({
      next: (events) => {
        this.events.set(events);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set('Failed to load events.');
        this.loading.set(false);
      }
    });
  }

  switchTab(tab: 'venues' | 'events'): void {
    this.activeTab.set(tab);
  }

  openVenueModal(): void {
    this.showVenueModal = true;
  }

  closeVenueModal(): void {
    this.showVenueModal = false;
  }

  openEventModal(): void {
    this.showEventModal = true;
  }

  closeEventModal(): void {
    this.showEventModal = false;
  }

  submitVenue(): void {
    this.error.set(null);
    this.message.set(null);

    this.venueService.createVenue(this.venueForm).subscribe({
      next: (res) => {
        this.message.set(`Venue "${res.name}" created successfully!`);
        this.closeVenueModal();
        this.loadData();
      },
      error: (err) => {
        console.error('Error creating venue', err);
        this.error.set(err.error?.message || 'Failed to create venue. Admin privileges required.');
      }
    });
  }

  submitEvent(): void {
    this.error.set(null);
    this.message.set(null);

    // Format ISO date
    const formattedDate = new Date(this.eventForm.eventDate).toISOString();
    const payload: EventRequest = {
      ...this.eventForm,
      eventDate: formattedDate
    };

    this.eventService.createEvent(payload).subscribe({
      next: (res) => {
        this.message.set(`Event "${res.title}" created successfully!`);
        this.closeEventModal();
        this.loadEvents();
      },
      error: (err) => {
        console.error('Error creating event', err);
        this.error.set(err.error?.message || 'Failed to create event. Admin privileges required.');
      }
    });
  }

  deleteVenue(id: number): void {
    if (confirm('Are you sure you want to delete this venue?')) {
      this.venueService.deleteVenue(id).subscribe({
        next: () => {
          this.message.set('Venue deleted.');
          this.loadData();
        },
        error: (err) => this.error.set('Failed to delete venue.')
      });
    }
  }

  deleteEvent(id: number): void {
    if (confirm('Are you sure you want to delete this event?')) {
      this.eventService.deleteEvent(id).subscribe({
        next: () => {
          this.message.set('Event deleted.');
          this.loadEvents();
        },
        error: (err) => this.error.set('Failed to delete event.')
      });
    }
  }
}
