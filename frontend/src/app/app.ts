import { Component } from '@angular/core';
import { RouterOutlet, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from './services/auth.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, CommonModule],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class AppComponent {
  title = 'TicketVerse';

  constructor(public authService: AuthService) {}
  
  systemServices = [
    { name: 'Spring Boot 3.3 Backend', port: 8080, status: 'READY', type: 'REST API & Venue/Event Engine' },
    { name: 'PostgreSQL 16 Database', port: 5432, status: 'CONFIGURED', type: 'Relational DB & Venues/Events' },
    { name: 'Redis 7 In-Memory Cache', port: 6379, status: 'CONFIGURED', type: 'TTL Holds & Distributed Lock' },
    { name: 'Angular 18 Frontend', port: 4200, status: 'ACTIVE', type: 'Client App & Event Browser' }
  ];

  roadmapPhases = [
    { num: 1, name: 'Project Setup & Architecture', status: 'COMPLETED', active: false },
    { num: 2, name: 'JWT Auth & User Roles', status: 'COMPLETED', active: false },
    { num: 3, name: 'Venue & Event Management', status: 'COMPLETED', active: false },
    { num: 4, name: 'Seat Map Grid & Generation', status: 'COMPLETED', active: false },
    { num: 5, name: 'Basic Booking Engine', status: 'COMPLETED', active: true },
    { num: 6, name: 'High Concurrency & Locks', status: 'UPCOMING', active: false },
    { num: 7, name: 'Temporary Seat Holds', status: 'UPCOMING', active: false },
    { num: 8, name: 'Idempotent Payment Gateway', status: 'UPCOMING', active: false }
  ];

  logout(): void {
    this.authService.logout();
  }
}
