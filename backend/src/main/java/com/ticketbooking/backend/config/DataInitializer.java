package com.ticketbooking.backend.config;

import com.ticketbooking.backend.entity.*;
import com.ticketbooking.backend.repository.EventRepository;
import com.ticketbooking.backend.repository.RoleRepository;
import com.ticketbooking.backend.repository.UserRepository;
import com.ticketbooking.backend.repository.VenueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Set;

@Component
@Profile("!test")
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final VenueRepository venueRepository;
    private final EventRepository eventRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        log.info("Checking & seeding initial data...");

        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_USER").build()));

        Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_ADMIN").build()));

        if (!userRepository.existsByEmail("admin@ticketverse.com")) {
            User admin = User.builder()
                    .email("admin@ticketverse.com")
                    .fullName("TicketVerse Administrator")
                    .password(passwordEncoder.encode("Admin@123456"))
                    .roles(Set.of(userRole, adminRole))
                    .build();
            userRepository.save(admin);
            log.info("Created default admin user: admin@ticketverse.com / Admin@123456");
        }

        if (venueRepository.count() == 0) {
            Venue msg = venueRepository.save(Venue.builder()
                    .name("Madison Square Garden")
                    .city("New York")
                    .address("4 Pennsylvania Plaza, New York, NY 10001")
                    .capacity(20000)
                    .description("World-famous multi-purpose indoor arena in New York City.")
                    .build());

            Venue wembley = venueRepository.save(Venue.builder()
                    .name("Wembley Stadium")
                    .city("London")
                    .address("London HA9 0WS, United Kingdom")
                    .capacity(90000)
                    .description("Iconic major football stadium and concert venue in London.")
                    .build());

            Venue redRocks = venueRepository.save(Venue.builder()
                    .name("Red Rocks Amphitheatre")
                    .city("Denver")
                    .address("18300 W Alameda Pkwy, Morrison, CO 80465")
                    .capacity(9525)
                    .description("Open-air amphitheatre built into a rock structure in Morrison, Colorado.")
                    .build());

            if (eventRepository.count() == 0) {
                eventRepository.save(Event.builder()
                        .title("Coldplay - Music of the Spheres World Tour")
                        .description("Experience Coldplay's electrifying stadium tour with stunning visuals and hit singles.")
                        .category("Concert")
                        .eventDate(OffsetDateTime.now().plusDays(15))
                        .status(EventStatus.ON_SALE)
                        .bannerUrl("https://images.unsplash.com/photo-1470225620780-dba8ba36b745?auto=format&fit=crop&w=1200&q=80")
                        .venue(wembley)
                        .totalSeats(50000)
                        .availableSeats(50000)
                        .build());

                eventRepository.save(Event.builder()
                        .title("Global Tech Innovators Summit 2026")
                        .description("Annual premier conference bringing together AI, Cloud, and High-Concurrency systems engineers.")
                        .category("Conference")
                        .eventDate(OffsetDateTime.now().plusDays(30))
                        .status(EventStatus.ON_SALE)
                        .bannerUrl("https://images.unsplash.com/photo-1540575467063-178a50c2df87?auto=format&fit=crop&w=1200&q=80")
                        .venue(msg)
                        .totalSeats(15000)
                        .availableSeats(15000)
                        .build());

                eventRepository.save(Event.builder()
                        .title("Symphony Under the Stars")
                        .description("An enchanting night of classical masterpieces performed live at Red Rocks Amphitheatre.")
                        .category("Orchestra")
                        .eventDate(OffsetDateTime.now().plusDays(45))
                        .status(EventStatus.UPCOMING)
                        .bannerUrl("https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?auto=format&fit=crop&w=1200&q=80")
                        .venue(redRocks)
                        .totalSeats(8000)
                        .availableSeats(8000)
                        .build());

                log.info("Seeded initial venues and events successfully.");
            }
        }
    }
}
