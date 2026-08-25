import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { VenueResponse } from './venue.service';

export type EventStatus = 'UPCOMING' | 'ON_SALE' | 'SOLD_OUT' | 'CANCELLED' | 'COMPLETED';

export interface EventResponse {
  id: number;
  title: string;
  description?: string;
  category: string;
  eventDate: string;
  status: EventStatus;
  bannerUrl?: string;
  venue: VenueResponse;
  totalSeats: number;
  availableSeats: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface EventRequest {
  title: string;
  description?: string;
  category: string;
  eventDate: string;
  status?: EventStatus;
  bannerUrl?: string;
  venueId: number;
  totalSeats: number;
}

@Injectable({
  providedIn: 'root'
})
export class EventService {
  private apiUrl = 'http://localhost:8080/api/v1/events';

  constructor(private http: HttpClient) {}

  searchEvents(category?: string, city?: string, search?: string): Observable<EventResponse[]> {
    let params = new HttpParams();
    if (category) params = params.set('category', category);
    if (city) params = params.set('city', city);
    if (search) params = params.set('search', search);

    return this.http.get<EventResponse[]>(this.apiUrl, { params });
  }

  getEventById(id: number): Observable<EventResponse> {
    return this.http.get<EventResponse>(`${this.apiUrl}/${id}`);
  }

  createEvent(request: EventRequest): Observable<EventResponse> {
    return this.http.post<EventResponse>(this.apiUrl, request);
  }

  updateEvent(id: number, request: EventRequest): Observable<EventResponse> {
    return this.http.put<EventResponse>(`${this.apiUrl}/${id}`, request);
  }

  deleteEvent(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
