import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface QueueStatusResponse {
  eventId: number;
  queueToken: string;
  userId: number;
  position: number;
  totalWaiting: number;
  estimatedWaitSeconds: number;
  status: 'WAITING' | 'ADMITTED' | 'EXPIRED';
  admissionCode?: string;
  joinedAt?: string;
  admittedAt?: string;
}

@Injectable({
  providedIn: 'root'
})
export class QueueService {
  private baseUrl = 'http://localhost:8080/api/v1/queue';

  constructor(private http: HttpClient) {}

  joinQueue(eventId: number): Observable<QueueStatusResponse> {
    return this.http.post<QueueStatusResponse>(`${this.baseUrl}/join?eventId=${eventId}`, {});
  }

  getQueueStatus(eventId: number, queueToken: string): Observable<QueueStatusResponse> {
    return this.http.get<QueueStatusResponse>(`${this.baseUrl}/status?eventId=${eventId}&queueToken=${queueToken}`);
  }

  drainQueue(eventId: number, batchSize: number = 10): Observable<any> {
    return this.http.post<any>(`${this.baseUrl}/admin/drain?eventId=${eventId}&batchSize=${batchSize}`, {});
  }

  getQueueStats(eventId: number): Observable<any> {
    return this.http.get<any>(`${this.baseUrl}/admin/stats?eventId=${eventId}`);
  }
}
