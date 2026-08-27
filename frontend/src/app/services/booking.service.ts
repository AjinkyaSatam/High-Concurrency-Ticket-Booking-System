import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export type BookingStatus = 'CONFIRMED' | 'CANCELLED' | 'PENDING';

export interface TicketResponse {
  id: number;
  ticketCode: string;
  seatId: number;
  sectionName: string;
  rowName: string;
  seatNumber: number;
  seatCode: string;
  price: number;
}

export interface BookingResponse {
  id: number;
  bookingReference: string;
  userId: number;
  userEmail: string;
  userName: string;
  eventId: number;
  eventTitle: string;
  venueName: string;
  eventDate: string;
  totalAmount: number;
  status: BookingStatus;
  bookingTime: string;
  tickets: TicketResponse[];
}

export interface BookingRequest {
  eventId: number;
  seatIds: number[];
}

@Injectable({
  providedIn: 'root'
})
export class BookingService {
  private apiUrl = 'http://localhost:8080/api/v1/bookings';

  constructor(private http: HttpClient) {}

  createBooking(request: BookingRequest): Observable<BookingResponse> {
    return this.http.post<BookingResponse>(this.apiUrl, request);
  }

  getMyBookings(): Observable<BookingResponse[]> {
    return this.http.get<BookingResponse[]>(`${this.apiUrl}/my-bookings`);
  }

  getBookingById(id: number): Observable<BookingResponse> {
    return this.http.get<BookingResponse>(`${this.apiUrl}/${id}`);
  }

  cancelBooking(id: number): Observable<BookingResponse> {
    return this.http.post<BookingResponse>(`${this.apiUrl}/${id}/cancel`, {});
  }

  downloadETicketPdf(bookingId: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/${bookingId}/ticket-pdf`, { responseType: 'blob' });
  }

  verifyTicketQr(ticketCode: string): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/verify/${ticketCode}`);
  }
}
