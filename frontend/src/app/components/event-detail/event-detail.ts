import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { EventService, EventResponse } from '../../services/event.service';
import { SeatService, SeatResponse } from '../../services/seat.service';
import { BookingService, BookingResponse } from '../../services/booking.service';
import { AuthService } from '../../services/auth.service';

export interface RowGroup {
  rowName: string;
  seats: SeatResponse[];
}

export interface SectionGroup {
  sectionName: string;
  price: number;
  rows: RowGroup[];
}

@Component({
  selector: 'app-event-detail',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './event-detail.html',
  styleUrl: './event-detail.css'
})
export class EventDetailComponent implements OnInit {
  event = signal<EventResponse | null>(null);
  seats = signal<SeatResponse[]>([]);
  selectedSeats = signal<SeatResponse[]>([]);

  loading = signal<boolean>(true);
  loadingSeats = signal<boolean>(false);
  bookingInProgress = signal<boolean>(false);
  
  error = signal<string | null>(null);
  bookingError = signal<string | null>(null);
  bookingSuccess = signal<BookingResponse | null>(null);

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private eventService: EventService,
    private seatService: SeatService,
    private bookingService: BookingService,
    public authService: AuthService
  ) {}

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      const eventId = Number(idParam);
      this.loadEvent(eventId);
    } else {
      this.error.set('Invalid event ID');
      this.loading.set(false);
    }
  }

  loadEvent(id: number): void {
    this.loading.set(true);
    this.eventService.getEventById(id).subscribe({
      next: (data) => {
        this.event.set(data);
        this.loading.set(false);
        this.loadSeats(id);
      },
      error: (err) => {
        console.error('Error fetching event details', err);
        this.error.set('Failed to load event details. Please verify the event ID.');
        this.loading.set(false);
      }
    });
  }

  loadSeats(eventId: number): void {
    this.loadingSeats.set(true);
    this.seatService.getSeatsByEventId(eventId).subscribe({
      next: (data) => {
        this.seats.set(data);
        this.loadingSeats.set(false);
      },
      error: (err) => {
        console.error('Error loading seats', err);
        this.loadingSeats.set(false);
      }
    });
  }

  // Helper computed signal to structure seats into sections & rows for clean grid display
  sections = computed<SectionGroup[]>(() => {
    const allSeats = this.seats();
    if (!allSeats || allSeats.length === 0) return [];

    const sectionMap = new Map<string, Map<string, SeatResponse[]>>();

    for (const seat of allSeats) {
      if (!sectionMap.has(seat.sectionName)) {
        sectionMap.set(seat.sectionName, new Map());
      }
      const rowMap = sectionMap.get(seat.sectionName)!;
      if (!rowMap.has(seat.rowName)) {
        rowMap.set(seat.rowName, []);
      }
      rowMap.get(seat.rowName)!.push(seat);
    }

    const sectionGroups: SectionGroup[] = [];

    sectionMap.forEach((rowMap, sectionName) => {
      const rows: RowGroup[] = [];
      let sectionPrice = 0;

      rowMap.forEach((seatList, rowName) => {
        seatList.sort((a, b) => a.seatNumber - b.seatNumber);
        if (seatList.length > 0) sectionPrice = seatList[0].price;
        rows.push({ rowName, seats: seatList });
      });

      rows.sort((a, b) => a.rowName.localeCompare(b.rowName));
      sectionGroups.push({ sectionName, price: sectionPrice, rows });
    });

    // Custom sort sections: VIP -> PREMIUM -> REGULAR
    const order = ['VIP', 'PREMIUM', 'REGULAR'];
    sectionGroups.sort((a, b) => {
      const idxA = order.indexOf(a.sectionName.toUpperCase());
      const idxB = order.indexOf(b.sectionName.toUpperCase());
      if (idxA !== -1 && idxB !== -1) return idxA - idxB;
      return a.sectionName.localeCompare(b.sectionName);
    });

    return sectionGroups;
  });

  toggleSeat(seat: SeatResponse): void {
    if (seat.status !== 'AVAILABLE') return;

    const current = this.selectedSeats();
    const index = current.findIndex((s) => s.id === seat.id);

    if (index >= 0) {
      this.selectedSeats.set(current.filter((s) => s.id !== seat.id));
    } else {
      if (current.length >= 6) {
        alert('You can select a maximum of 6 seats per booking.');
        return;
      }
      this.selectedSeats.set([...current, seat]);
    }
  }

  isSelected(seatId: number): boolean {
    return this.selectedSeats().some((s) => s.id === seatId);
  }

  getTotalPrice(): number {
    return this.selectedSeats().reduce((sum, s) => sum + Number(s.price), 0);
  }

  bookSelectedSeats(): void {
    if (!this.authService.isAuthenticated()) {
      this.router.navigate(['/login']);
      return;
    }

    const sel = this.selectedSeats();
    const ev = this.event();
    if (sel.length === 0 || !ev) return;

    this.bookingInProgress.set(true);
    this.bookingError.set(null);

    const seatIds = sel.map((s) => s.id);

    this.bookingService.createBooking({ eventId: ev.id, seatIds }).subscribe({
      next: (booking) => {
        this.bookingSuccess.set(booking);
        this.bookingInProgress.set(false);
        this.selectedSeats.set([]);
        this.loadEvent(ev.id); // Reload event & seats
      },
      error: (err) => {
        console.error('Booking failed', err);
        const msg = err.error?.message || err.error || 'Failed to complete booking. Some seats may no longer be available.';
        this.bookingError.set(msg);
        this.bookingInProgress.set(false);
        this.loadSeats(ev.id);
      }
    });
  }

  generateSeats(): void {
    const ev = this.event();
    if (!ev) return;

    this.loadingSeats.set(true);
    this.seatService.generateSeats(ev.id, {
      vipRows: 2,
      premiumRows: 3,
      regularRows: 5,
      seatsPerRow: 10,
      vipPrice: 150,
      premiumPrice: 100,
      regularPrice: 50
    }).subscribe({
      next: (data) => {
        this.seats.set(data);
        this.loadingSeats.set(false);
        this.loadEvent(ev.id);
      },
      error: (err) => {
        console.error('Seat generation error', err);
        this.loadingSeats.set(false);
      }
    });
  }

  closeModal(): void {
    this.bookingSuccess.set(null);
  }
}
