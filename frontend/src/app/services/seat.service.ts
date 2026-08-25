import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export type SeatStatus = 'AVAILABLE' | 'HELD' | 'BOOKED' | 'BLOCKED';

export interface SeatResponse {
  id: number;
  eventId: number;
  sectionName: string;
  rowName: string;
  seatNumber: number;
  seatCode: string;
  price: number;
  status: SeatStatus;
}

export interface SeatGenerationRequest {
  vipRows?: number;
  premiumRows?: number;
  regularRows?: number;
  seatsPerRow?: number;
  vipPrice?: number;
  premiumPrice?: number;
  regularPrice?: number;
}

@Injectable({
  providedIn: 'root'
})
export class SeatService {
  private baseUrl = 'http://localhost:8080/api/v1/events';

  constructor(private http: HttpClient) {}

  getSeatsByEventId(eventId: number): Observable<SeatResponse[]> {
    return this.http.get<SeatResponse[]>(`${this.baseUrl}/${eventId}/seats`);
  }

  generateSeats(eventId: number, request: SeatGenerationRequest): Observable<SeatResponse[]> {
    return this.http.post<SeatResponse[]>(`${this.baseUrl}/${eventId}/seats/generate`, request);
  }
}
