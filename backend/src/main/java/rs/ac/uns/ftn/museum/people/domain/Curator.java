package rs.ac.uns.ftn.museum.people.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "kustos")
@DiscriminatorValue("CURATOR")
@PrimaryKeyJoinColumn(name = "zaposleni_id")
public class Curator extends Employee {
    @Column(name = "specijalizacija", columnDefinition = "text")
    private String specialization;
    public String getSpecialization() { return specialization; }
    public void setSpecialization(String value) { this.specialization = value; }
    @Override public EmployeeType getType() { return EmployeeType.CURATOR; }
}
