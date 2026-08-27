import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface DashboardAnalytics {
  totalRevenue: number;
  totalBookings: number;
  confirmedBookings: number;
  totalTicketsSold: number;
  totalEvents: number;
  totalUsers: number;
  totalSeats: number;
  bookedSeats: number;
  heldSeats: number;
  availableSeats: number;
  occupancyRate: number;
  conversionRate: number;
  systemConcurrencyMode: string;
  redisStatus: string;
}

@Injectable({
  providedIn: 'root'
})
export class AnalyticsService {
  private apiUrl = 'http://localhost:8080/api/v1/analytics/dashboard';

  constructor(private http: HttpClient) {}

  getDashboardMetrics(): Observable<DashboardAnalytics> {
    return this.http.get<DashboardAnalytics>(this.apiUrl);
  }
}
