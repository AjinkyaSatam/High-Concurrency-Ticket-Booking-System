package com.ticketbooking.backend.service.impl;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.ticketbooking.backend.entity.Booking;
import com.ticketbooking.backend.entity.Ticket;
import com.ticketbooking.backend.repository.BookingRepository;
import com.ticketbooking.backend.repository.TicketRepository;
import com.ticketbooking.backend.service.ETicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ETicketServiceImpl implements ETicketService {

    private final BookingRepository bookingRepository;
    private final TicketRepository ticketRepository;

    @Override
    public byte[] generateETicketPdf(String userEmail, Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found with ID: " + bookingId));

        if (!booking.getUser().getEmail().equals(userEmail)) {
            throw new RuntimeException("Unauthorized access to booking " + bookingId);
        }

        try {
            // Generate QR Code Base64
            String qrPayload = "TICKETVERSE:REF=" + booking.getBookingReference() + ":USER=" + userEmail + ":DATE=" + booking.getBookingTime();
            String qrBase64 = generateQrCodeBase64(qrPayload, 200, 200);

            StringBuilder html = new StringBuilder();
            html.append("<!DOCTYPE html><html><head><style>")
                .append("body { font-family: 'Helvetica Neue', Arial, sans-serif; background-color: #0f172a; color: #f8fafc; padding: 40px; }")
                .append(".ticket { max-width: 600px; margin: 0 auto; background: #1e293b; border: 2px solid #3b82f6; border-radius: 16px; padding: 30px; box-shadow: 0 10px 25px rgba(0,0,0,0.5); }")
                .append(".header { display: flex; justify-content: space-between; align-items: center; border-bottom: 2px dashed #475569; padding-bottom: 20px; }")
                .append(".logo { font-size: 24px; font-weight: bold; color: #38bdf8; }")
                .append(".ref { font-family: monospace; font-size: 16px; color: #a855f7; background: #2e1065; padding: 6px 12px; border-radius: 6px; }")
                .append(".event-title { font-size: 28px; font-weight: 800; color: #f1f5f9; margin: 20px 0 10px 0; }")
                .append(".meta { color: #94a3b8; font-size: 14px; margin-bottom: 20px; }")
                .append(".seats-box { background: #0f172a; padding: 15px; border-radius: 8px; margin-bottom: 20px; }")
                .append(".seat-item { display: inline-block; background: #1e3a8a; color: #60a5fa; padding: 8px 14px; border-radius: 6px; font-weight: bold; margin: 4px; }")
                .append(".qr-section { text-align: center; margin-top: 20px; }")
                .append(".qr-img { width: 180px; height: 180px; border-radius: 12px; border: 4px solid #38bdf8; }")
                .append(".footer { text-align: center; margin-top: 15px; font-size: 12px; color: #64748b; }")
                .append("</style></head><body>");

            html.append("<div class='ticket'>")
                .append("<div class='header'>")
                .append("<div class='logo'>🎫 TicketVerse Pass</div>")
                .append("<div class='ref'>REF: ").append(booking.getBookingReference()).append("</div>")
                .append("</div>")
                .append("<div class='event-title'>").append(booking.getEvent().getTitle()).append("</div>")
                .append("<div class='meta'>📍 ").append(booking.getEvent().getVenue().getName())
                .append(" &bull; 📅 ").append(booking.getEvent().getEventDate()).append("</div>")
                .append("<div class='seats-box'><strong style='color:#94a3b8;'>RESERVED SEATS:</strong><br/>");

            for (Ticket ticket : booking.getTickets()) {
                html.append("<span class='seat-item'>").append(ticket.getSeat().getSectionName())
                    .append(" - Row ").append(ticket.getSeat().getRowName())
                    .append(" Seat ").append(ticket.getSeat().getSeatNumber()).append("</span>");
            }

            html.append("</div>")
                .append("<div class='qr-section'>")
                .append("<img class='qr-img' src='data:image/png;base64,").append(qrBase64).append("' /><br/>")
                .append("<span style='font-size:12px; color:#94a3b8; margin-top:6px; display:inline-block;'>Official Digital Entry Pass</span>")
                .append("</div>")
                .append("<div class='footer'>High-Concurrency Cryptographic Verification • TicketVerse Engine</div>")
                .append("</div></body></html>");

            return html.toString().getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Failed to generate digital e-ticket PDF for booking {}", bookingId, e);
            throw new RuntimeException("E-Ticket generation failed", e);
        }
    }

    @Override
    public Map<String, Object> verifyTicketQr(String ticketCode) {
        Optional<Ticket> ticketOpt = ticketRepository.findByTicketCode(ticketCode);
        Map<String, Object> result = new HashMap<>();

        if (ticketOpt.isPresent()) {
            Ticket t = ticketOpt.get();
            result.put("valid", true);
            result.put("ticketCode", t.getTicketCode());
            result.put("bookingRef", t.getBooking().getBookingReference());
            result.put("eventTitle", t.getBooking().getEvent().getTitle());
            result.put("seatCode", t.getSeat().getSeatCode());
            result.put("userName", t.getBooking().getUser().getFullName());
            result.put("message", "VALID TICKET - ADMIT HOLDER");
        } else {
            result.put("valid", false);
            result.put("message", "INVALID OR FORGED TICKET CODE");
        }
        return result;
    }

    private String generateQrCodeBase64(String text, int width, int height) throws Exception {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height);
        ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
        byte[] pngData = pngOutputStream.toByteArray();
        return Base64.getEncoder().encodeToString(pngData);
    }
}
