import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, CommonModule],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class AppComponent {
  title = 'TicketVerse';
  
  systemServices = [
    { name: 'Spring Boot 3.3 Backend', port: 8080, status: 'READY', type: 'REST API & Logic' },
    { name: 'PostgreSQL 16 Database', port: 5432, status: 'CONFIGURED', type: 'Relational DB & Locks' },
    { name: 'Redis 7 In-Memory Cache', port: 6379, status: 'CONFIGURED', type: 'TTL Holds & Distributed Lock' },
    { name: 'Angular 18 Frontend', port: 4200, status: 'ACTIVE', type: 'Client App' }
  ];

  roadmapPhases = [
    { num: 1, name: 'Project Setup & Architecture', status: 'IN_PROGRESS', active: true },
    { num: 2, name: 'JWT Auth & User Roles', status: 'UPCOMING', active: false },
    { num: 3, name: 'Venue & Event Management', status: 'UPCOMING', active: false },
    { num: 4, name: 'Seat Map Grid & Generation', status: 'UPCOMING', active: false },
    { num: 5, name: 'Basic Booking Engine', status: 'UPCOMING', active: false },
    { num: 6, name: 'High Concurrency & Locks', status: 'UPCOMING', active: false },
    { num: 7, name: 'Temporary Seat Holds', status: 'UPCOMING', active: false },
    { num: 8, name: 'Idempotent Payment Gateway', status: 'UPCOMING', active: false }
  ];
}
