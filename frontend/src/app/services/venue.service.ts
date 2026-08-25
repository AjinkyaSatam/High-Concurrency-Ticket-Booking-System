import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface VenueResponse {
  id: number;
  name: string;
  city: string;
  address: string;
  capacity: number;
  description?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface VenueRequest {
  name: string;
  city: string;
  address: string;
  capacity: number;
  description?: string;
}

@Injectable({
  providedIn: 'root'
})
export class VenueService {
  private apiUrl = 'http://localhost:8080/api/v1/venues';

  constructor(private http: HttpClient) {}

  getAllVenues(): Observable<VenueResponse[]> {
    return this.http.get<VenueResponse[]>(this.apiUrl);
  }

  getVenueById(id: number): Observable<VenueResponse> {
    return this.http.get<VenueResponse>(`${this.apiUrl}/${id}`);
  }

  createVenue(request: VenueRequest): Observable<VenueResponse> {
    return this.http.post<VenueResponse>(this.apiUrl, request);
  }

  updateVenue(id: number, request: VenueRequest): Observable<VenueResponse> {
    return this.http.put<VenueResponse>(`${this.apiUrl}/${id}`, request);
  }

  deleteVenue(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
