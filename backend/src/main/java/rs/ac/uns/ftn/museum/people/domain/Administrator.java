package rs.ac.uns.ftn.museum.people.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "administrator")
@DiscriminatorValue("ADMINISTRATOR")
@PrimaryKeyJoinColumn(name = "zaposleni_id")
public class Administrator extends Employee {
    @Column(name = "nivo_administracije", columnDefinition = "text")
    private String administrationLevel;
    public String getAdministrationLevel() { return administrationLevel; }
    public void setAdministrationLevel(String value) { this.administrationLevel = value; }
    @Override public EmployeeType getType() { return EmployeeType.ADMINISTRATOR; }
}
