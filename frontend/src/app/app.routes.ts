import { Routes } from '@angular/router';
import { LoginComponent } from './components/login/login';
import { RegisterComponent } from './components/register/register';
import { EventsListComponent } from './components/events-list/events-list';
import { EventDetailComponent } from './components/event-detail/event-detail';
import { AdminEventsComponent } from './components/admin-events/admin-events';

export const routes: Routes = [
  { path: '', component: EventsListComponent },
  { path: 'events', component: EventsListComponent },
  { path: 'events/:id', component: EventDetailComponent },
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  { path: 'admin/events', component: AdminEventsComponent },
  { path: '**', redirectTo: '' }
];
