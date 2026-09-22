package rs.ac.uns.ftn.museum.journey.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.math.BigDecimal;
import rs.ac.uns.ftn.museum.catalog.domain.*;
import rs.ac.uns.ftn.museum.people.domain.Visitor;

@Entity
@Table(name="transakcija")
public class Payment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="ulaznica_id", nullable=false, unique=false)
    private Ticket ticket;
    @Column(name = "iznos", nullable = false, precision=19, scale=2)
    private BigDecimal amount;
    @Enumerated(EnumType.STRING) @Column(name="nacin_placanja", nullable=false, length=32)
    private PaymentMethod method;
    @Enumerated(EnumType.STRING) @Column(name="status", nullable=false, length=32)
    private PaymentStatus status;
    @Column(name = "eksterna_referenca", nullable = true, columnDefinition="text")
    private String externalReference;
    @Column(name = "datum_transakcije", nullable = false)
    private Instant createdAt;
    public Long getId() { return id; }
    public void setId(Long value) { id = value; }
    public Ticket getTicket() { return ticket; }
    public void setTicket(Ticket value) { ticket = value; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal value) { amount = value; }
    public PaymentMethod getMethod() { return method; }
    public void setMethod(PaymentMethod value) { method = value; }
    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus value) { status = value; }
    public String getExternalReference() { return externalReference; }
    public void setExternalReference(String value) { externalReference = value; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant value) { createdAt = value; }
}
