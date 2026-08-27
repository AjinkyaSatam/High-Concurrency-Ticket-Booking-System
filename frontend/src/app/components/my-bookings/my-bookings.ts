import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { BookingService, BookingResponse } from '../../services/booking.service';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-my-bookings',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './my-bookings.html',
  styleUrl: './my-bookings.css'
})
export class MyBookingsComponent implements OnInit {
  bookings = signal<BookingResponse[]>([]);
  loading = signal<boolean>(true);
  error = signal<string | null>(null);
  cancellingId = signal<number | null>(null);

  constructor(
    private bookingService: BookingService,
    public authService: AuthService
  ) {}

  ngOnInit(): void {
    this.loadBookings();
  }

  loadBookings(): void {
    this.loading.set(true);
    this.bookingService.getMyBookings().subscribe({
      next: (data) => {
        this.bookings.set(data);
        this.loading.set(false);
      },
      error: (err) => {
        console.error('Error fetching bookings', err);
        this.error.set('Failed to load your bookings. Please make sure you are logged in.');
        this.loading.set(false);
      }
    });
  }

  cancelBooking(bookingId: number): void {
    if (!confirm('Are you sure you want to cancel this booking? Released seats will be made available to other users.')) {
      return;
    }

    this.cancellingId.set(bookingId);
    this.bookingService.cancelBooking(bookingId).subscribe({
      next: (updatedBooking) => {
        const list = this.bookings().map((b) => (b.id === bookingId ? updatedBooking : b));
        this.bookings.set(list);
        this.cancellingId.set(null);
      },
      error: (err) => {
        console.error('Failed to cancel booking', err);
        alert(err.error?.message || 'Failed to cancel booking.');
        this.cancellingId.set(null);
      }
    });
  }

  downloadETicket(bookingId: number): void {
    this.bookingService.downloadETicketPdf(bookingId).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `eticket-booking-${bookingId}.html`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);
      },
      error: (err) => {
        console.error('Failed to download e-ticket', err);
        alert('Could not download E-Ticket pass.');
      }
    });
  }
}
