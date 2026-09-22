package rs.ac.uns.ftn.museum.people.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "blagajnik")
@DiscriminatorValue("CASHIER")
@PrimaryKeyJoinColumn(name = "zaposleni_id")
public class Cashier extends Employee {
    @Column(name = "oznaka_saltera", columnDefinition = "text")
    private String counterLabel;
    public String getCounterLabel() { return counterLabel; }
    public void setCounterLabel(String value) { this.counterLabel = value; }
    @Override public EmployeeType getType() { return EmployeeType.CASHIER; }
}
