package rs.ac.uns.ftn.museum.people.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "vodic")
@DiscriminatorValue("GUIDE")
@PrimaryKeyJoinColumn(name = "zaposleni_id")
public class Guide extends Employee {
    @Column(name = "primarni_jezik", columnDefinition = "text")
    private String primaryLanguage;
    public String getPrimaryLanguage() { return primaryLanguage; }
    public void setPrimaryLanguage(String value) { this.primaryLanguage = value; }
    @Override public EmployeeType getType() { return EmployeeType.GUIDE; }
}
