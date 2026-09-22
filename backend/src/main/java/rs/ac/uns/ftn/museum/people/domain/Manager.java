package rs.ac.uns.ftn.museum.people.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "menadzer")
@DiscriminatorValue("MANAGER")
@PrimaryKeyJoinColumn(name = "zaposleni_id")
public class Manager extends Employee {
    @Column(name = "nivo_izvestavanja", columnDefinition = "text")
    private String reportingLevel;
    public String getReportingLevel() { return reportingLevel; }
    public void setReportingLevel(String value) { this.reportingLevel = value; }
    @Override public EmployeeType getType() { return EmployeeType.MANAGER; }
}
