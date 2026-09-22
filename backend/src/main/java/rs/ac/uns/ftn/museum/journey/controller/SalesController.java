package rs.ac.uns.ftn.museum.journey.controller;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rs.ac.uns.ftn.museum.journey.service.SalesService;
import static rs.ac.uns.ftn.museum.journey.dto.JourneyDtos.*;

@RestController
public class SalesController {
    private final SalesService sales;
    public SalesController(SalesService sales) { this.sales=sales; }
    @GetMapping("/api/prices") public List<PriceResponse> prices(@RequestParam Long exhibitionId) { return sales.prices(exhibitionId); }
    @PostMapping("/api/tickets") public ResponseEntity<TicketResponse> buy(@Valid @RequestBody PurchaseRequest request) {
        var result=sales.purchase(request,false);return ResponseEntity.created(URI.create("/api/tickets/"+result.id())).body(result);
    }
    @PostMapping("/api/cashier/tickets") public ResponseEntity<TicketResponse> sell(@Valid @RequestBody PurchaseRequest request) {
        var result=sales.purchase(request,true);return ResponseEntity.created(URI.create("/api/tickets/"+result.id())).body(result);
    }
    @GetMapping("/api/tickets/me") public List<TicketResponse> mine() { return sales.mine(); }
    @GetMapping("/api/tickets/{id}") public TicketResponse get(@PathVariable Long id) { return sales.get(id); }
    @GetMapping("/api/tickets/{id}/payments") public List<PaymentResponse> payments(@PathVariable Long id) { return sales.payments(id); }
    @PostMapping("/api/tickets/{id}/payments") public TicketResponse retry(@PathVariable Long id,@Valid @RequestBody RetryPaymentRequest request) { return sales.retry(id,request); }
    @PostMapping("/api/tickets/{id}/cancel") public TicketResponse cancel(@PathVariable Long id) { return sales.cancel(id); }
    @PostMapping("/api/cashier/validate") public ResponseEntity<VisitResponse> validate(@Valid @RequestBody ValidateTicketRequest request) {
        var result=sales.validate(request);return ResponseEntity.created(URI.create("/api/visits/"+result.id())).body(result);
    }
}
