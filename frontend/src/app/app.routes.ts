import { Routes } from '@angular/router';
import { LoginComponent } from './components/login/login';
import { RegisterComponent } from './components/register/register';
import { EventsListComponent } from './components/events-list/events-list';
import { EventDetailComponent } from './components/event-detail/event-detail';
import { AdminEventsComponent } from './components/admin-events/admin-events';
import { MyBookingsComponent } from './components/my-bookings/my-bookings';

import { WaitingRoomComponent } from './components/waiting-room/waiting-room';
import { AnalyticsDashboardComponent } from './components/analytics-dashboard/analytics-dashboard';

export const routes: Routes = [
  { path: '', component: EventsListComponent },
  { path: 'events', component: EventsListComponent },
  { path: 'events/:id', component: EventDetailComponent },
  { path: 'waiting-room/:eventId', component: WaitingRoomComponent },
  { path: 'my-bookings', component: MyBookingsComponent },
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  { path: 'admin/events', component: AdminEventsComponent },
  { path: 'admin/analytics', component: AnalyticsDashboardComponent },
  { path: '**', redirectTo: '' }
];
