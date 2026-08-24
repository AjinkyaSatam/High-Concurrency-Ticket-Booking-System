import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

export interface RegisterRequest {
  email: string;
  password: String;
  fullName: string;
}

export interface LoginRequest {
  email: string;
  password: String;
}

export interface AuthResponse {
  token: string;
  id: number;
  email: string;
  fullName: string;
  roles: string[];
}

export interface UserResponse {
  id: number;
  email: string;
  fullName: string;
  roles: string[];
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = 'http://localhost:8080/api/v1/auth';
  
  currentUser = signal<UserResponse | null>(null);
  token = signal<string | null>(localStorage.getItem('token'));

  constructor(private http: HttpClient) {
    if (this.token()) {
      this.fetchCurrentUser().subscribe({
        error: () => this.logout()
      });
    }
  }

  register(request: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/register`, request).pipe(
      tap(response => this.handleAuthSuccess(response))
    );
  }

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/login`, request).pipe(
      tap(response => this.handleAuthSuccess(response))
    );
  }

  fetchCurrentUser(): Observable<UserResponse> {
    return this.http.get<UserResponse>(`${this.apiUrl}/me`).pipe(
      tap(user => this.currentUser.set(user))
    );
  }

  logout(): void {
    localStorage.removeItem('token');
    this.token.set(null);
    this.currentUser.set(null);
  }

  getToken(): string | null {
    return localStorage.getItem('token');
  }

  isAuthenticated(): boolean {
    return !!this.getToken();
  }

  private handleAuthSuccess(response: AuthResponse): void {
    localStorage.setItem('token', response.token);
    this.token.set(response.token);
    this.currentUser.set({
      id: response.id,
      email: response.email,
      fullName: response.fullName,
      roles: response.roles
    });
  }
}
