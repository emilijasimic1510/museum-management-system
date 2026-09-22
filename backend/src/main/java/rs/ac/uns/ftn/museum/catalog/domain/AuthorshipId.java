package rs.ac.uns.ftn.museum.catalog.domain;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

@Embeddable
public class AuthorshipId implements Serializable {
    @Column(name = "autor_id", nullable = false)
    private Long authorId;
    @Column(name = "eksponat_id", nullable = false)
    private Long exhibitId;
    public AuthorshipId() {}
    public AuthorshipId(Long authorId, Long exhibitId) {
        this.authorId = authorId;
        this.exhibitId = exhibitId;
    }
    public Long getAuthorId() { return authorId; }
    public Long getExhibitId() { return exhibitId; }
    @Override public boolean equals(Object other) {
        if (!(other instanceof AuthorshipId that)) return false;
        return Objects.equals(authorId, that.authorId) && Objects.equals(exhibitId, that.exhibitId);
    }
    @Override public int hashCode() { return Objects.hash(authorId, exhibitId); }
}
