package rs.ac.uns.ftn.museum.journey.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.math.BigDecimal;
import rs.ac.uns.ftn.museum.catalog.domain.*;
import rs.ac.uns.ftn.museum.people.domain.Visitor;

@Entity
@Table(name="ulaznica")
public class Ticket {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch=FetchType.LAZY, optional=true) @JoinColumn(name="posetilac_id", nullable=true, unique=false)
    private Visitor visitor;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="termin_id", nullable=false, unique=false)
    private ExhibitionTerm term;
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumns({
        @JoinColumn(name="cenovnik_izlozba_id", referencedColumnName="izlozba_id", nullable=false),
        @JoinColumn(name="cenovnik_vazi_od", referencedColumnName="datum_vazenja_od", nullable=false),
        @JoinColumn(name="kategorija_cene", referencedColumnName="kategorija", nullable=false)})
    private ExhibitionPrice price;
    @Column(name = "datum_kupovine", nullable = false)
    private Instant purchasedAt;
    @Column(name = "cena_placena", nullable = false, precision=19, scale=2)
    private BigDecimal paidPrice;
    @Enumerated(EnumType.STRING) @Column(name="status", nullable=false, length=32)
    private TicketStatus status;
    @Column(name = "qr_token", nullable = false, columnDefinition="text")
    private String qrToken;
    @Column(name = "vazi_do", nullable = false)
    private Instant validUntil;
    public Long getId() { return id; }
    public void setId(Long value) { id = value; }
    public Visitor getVisitor() { return visitor; }
    public void setVisitor(Visitor value) { visitor = value; }
    public ExhibitionTerm getTerm() { return term; }
    public void setTerm(ExhibitionTerm value) { term = value; }
    public ExhibitionPrice getPrice() { return price; }
    public void setPrice(ExhibitionPrice value) { price = value; }
    public Instant getPurchasedAt() { return purchasedAt; }
    public void setPurchasedAt(Instant value) { purchasedAt = value; }
    public BigDecimal getPaidPrice() { return paidPrice; }
    public void setPaidPrice(BigDecimal value) { paidPrice = value; }
    public TicketStatus getStatus() { return status; }
    public void setStatus(TicketStatus value) { status = value; }
    public String getQrToken() { return qrToken; }
    public void setQrToken(String value) { qrToken = value; }
    public Instant getValidUntil() { return validUntil; }
    public void setValidUntil(Instant value) { validUntil = value; }
}
