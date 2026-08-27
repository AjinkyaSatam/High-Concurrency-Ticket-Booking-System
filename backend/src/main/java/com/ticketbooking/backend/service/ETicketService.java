package com.ticketbooking.backend.service;

import java.util.Map;

public interface ETicketService {
    byte[] generateETicketPdf(String userEmail, Long bookingId);
    Map<String, Object> verifyTicketQr(String ticketCode);
}
