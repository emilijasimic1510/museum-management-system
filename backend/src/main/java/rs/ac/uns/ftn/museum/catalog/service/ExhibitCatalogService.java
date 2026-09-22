package rs.ac.uns.ftn.museum.catalog.service;

import java.net.URI;
import java.time.Clock;
import java.util.*;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.ac.uns.ftn.museum.catalog.domain.*;
import rs.ac.uns.ftn.museum.catalog.dto.ExhibitDtos.*;
import rs.ac.uns.ftn.museum.catalog.exception.*;
import rs.ac.uns.ftn.museum.catalog.repository.*;

@Service
@Transactional
public class ExhibitCatalogService {
    private final ExhibitRepository exhibits;
    private final AuthorRepository authors;
    private final AuthorshipRepository authorships;
    private final ExhibitParticipationRepository participations;
    private final DigitalContentRepository contents;
    private final ExhibitionRepository exhibitions;
    private final Clock clock;

    public ExhibitCatalogService(ExhibitRepository exhibits, AuthorRepository authors, AuthorshipRepository authorships,
            ExhibitParticipationRepository participations, DigitalContentRepository contents,
            ExhibitionRepository exhibitions, Clock clock) {
        this.exhibits=exhibits; this.authors=authors; this.authorships=authorships;
        this.participations=participations; this.contents=contents; this.exhibitions=exhibitions; this.clock=clock;
    }

    @Transactional(readOnly=true)
    public List<ExhibitResponse> exhibits() { return exhibits.findAll(Sort.by("id")).stream().map(this::exhibit).toList(); }
    @Transactional(readOnly=true)
    public ExhibitResponse exhibit(Long id) { return exhibit(requireExhibit(id)); }
    @Transactional(readOnly=true)
    public ManagedExhibitResponse managedExhibit(Long id) { return managedExhibit(requireExhibit(id)); }
    @Transactional(readOnly=true)
    public List<AuthorResponse> authors() { return authors.findAll(Sort.by("id")).stream().map(this::author).toList(); }
    @Transactional(readOnly=true)
    public AuthorResponse author(Long id) { return author(requireAuthor(id)); }
    @Transactional(readOnly=true)
    public List<AuthorshipResponse> authorships(Long id) {
        requireExhibit(id);
        return authorships.findByExhibitIdOrderByAuthorId(id).stream().map(a->new AuthorshipResponse(authorSummary(a))).toList();
    }
    @Transactional(readOnly=true)
    public List<ParticipationResponse> exhibitParticipations(Long id) {
        requireExhibit(id);
        return participations.findByExhibitIdAndExhibitionOnlineVisibleTrueOrderById(id).stream().map(this::publicParticipation).toList();
    }
    @Transactional(readOnly=true)
    public List<ParticipationResponse> exhibitionParticipations(Long id) {
        Exhibition exhibition=requireExhibition(id);
        if (!Boolean.TRUE.equals(exhibition.getOnlineVisible())) throw new NotFoundException("Exhibition not found");
        return participations.findByExhibitionIdOrderById(id).stream().map(this::publicParticipation).toList();
    }
    @Transactional(readOnly=true)
    public List<ManagedParticipationResponse> managedParticipations(Long exhibitionId) {
        requireExhibition(exhibitionId);
        return participations.findByExhibitionIdOrderById(exhibitionId).stream().map(this::managedParticipation).toList();
    }
    @Transactional(readOnly=true)
    public ManagedParticipationResponse managedParticipation(Long id) { return managedParticipation(requireParticipation(id)); }

    public ManagedExhibitResponse createExhibit(ExhibitRequest r) {
        Exhibit e=new Exhibit(); apply(e,r); return managedExhibit(exhibits.saveAndFlush(e));
    }
    public ManagedExhibitResponse updateExhibit(Long id, ExhibitRequest r) {
        Exhibit e=requireExhibit(id); apply(e,r); exhibits.flush(); return managedExhibit(e);
    }
    public void deleteExhibit(Long id) { exhibits.delete(requireExhibit(id)); exhibits.flush(); }
    public AuthorResponse createAuthor(AuthorRequest r) {
        Author a=new Author(); apply(a,r); return author(authors.saveAndFlush(a));
    }
    public AuthorResponse updateAuthor(Long id, AuthorRequest r) {
        Author a=requireAuthor(id); apply(a,r); authors.flush(); return author(a);
    }
    public void deleteAuthor(Long id) { authors.delete(requireAuthor(id)); authors.flush(); }
    public ManagedParticipationResponse createParticipation(ParticipationRequest r) {
        ExhibitParticipation p=new ExhibitParticipation();
        p.setExhibition(requireExhibition(r.exhibitionId())); p.setExhibit(requireExhibit(r.exhibitId()));
        apply(p,r); return managedParticipation(participations.saveAndFlush(p));
    }
    public ManagedParticipationResponse updateParticipation(Long id, ParticipationRequest r) {
        ExhibitParticipation p=participations.findLockedById(id).orElseThrow(()->new NotFoundException("Participation not found"));
        // Existing scans/routes refer to this identity. Never move it to another exhibit/exhibition.
        if (!p.getExhibition().getId().equals(r.exhibitionId()) || !p.getExhibit().getId().equals(r.exhibitId()))
            throw new ConflictException("Participation exhibit and exhibition cannot be changed; create a new participation");
        apply(p,r); participations.flush(); return managedParticipation(p);
    }
    public void deleteParticipation(Long id) {
        ExhibitParticipation p=participations.findLockedById(id).orElseThrow(()->new NotFoundException("Participation not found"));
        // RESTRICT foreign keys preserve content, scans and route history; no cascading deletes.
        participations.delete(p); participations.flush();
    }
    @Transactional(readOnly=true)
    public List<DigitalContentResponse> contents(Long participationId) {
        requireParticipation(participationId);
        return contents.findByParticipationIdOrderByVersionDescId(participationId).stream().map(this::content).toList();
    }
    @Transactional(readOnly=true)
    public DigitalContentResponse content(Long id) { return content(requireContent(id)); }
    public DigitalContentResponse createContent(DigitalContentRequest r) {
        DigitalContent d=new DigitalContent(); d.setParticipation(requireParticipation(r.participationId()));
        d.setCreatedAt(clock.instant()); apply(d,r); return content(contents.saveAndFlush(d));
    }
    public DigitalContentResponse updateContent(Long id, DigitalContentRequest r) {
        DigitalContent d=requireContent(id);
        if (!d.getParticipation().getId().equals(r.participationId()))
            throw new ConflictException("Content participation cannot be changed; create a new content entry");
        apply(d,r); contents.flush(); return content(d);
    }
    public void deleteContent(Long id) { contents.delete(requireContent(id)); contents.flush(); }

    private Exhibit requireExhibit(Long id) { return exhibits.findById(id).orElseThrow(()->new NotFoundException("Exhibit not found")); }
    private Author requireAuthor(Long id) { return authors.findById(id).orElseThrow(()->new NotFoundException("Author not found")); }
    private Exhibition requireExhibition(Long id) { return exhibitions.findById(id).orElseThrow(()->new NotFoundException("Exhibition not found")); }
    private ExhibitParticipation requireParticipation(Long id) { return participations.findById(id).orElseThrow(()->new NotFoundException("Participation not found")); }
    private DigitalContent requireContent(Long id) { return contents.findById(id).orElseThrow(()->new NotFoundException("Digital content not found")); }
    private void apply(Exhibit e, ExhibitRequest r) {
        e.setName(r.name().trim()); e.setCreationPeriod(r.creationPeriod()); e.setType(r.type()); e.setMaterial(r.material());
        e.setOrigin(r.origin()); e.setDescription(r.description()); e.setInventoryNumber(r.inventoryNumber().trim());
    }
    private void apply(Author a, AuthorRequest r) {
        if (r.birthYear()!=null && r.deathYear()!=null && r.deathYear()<r.birthYear())
            throw new InvalidRequestException("Death year must not precede birth year");
        a.setFirstName(r.firstName().trim()); a.setLastName(r.lastName()); a.setBirthYear(r.birthYear());
        a.setDeathYear(r.deathYear()); a.setBiography(r.biography());
    }
    private void apply(ExhibitParticipation p, ParticipationRequest r) {
        if (r.installedAt()!=null && r.removedAt()!=null && !r.removedAt().isAfter(r.installedAt()))
            throw new InvalidRequestException("Removal must be after installation");
        if (r.qrToken()!=null && (r.qrToken().isBlank() || r.qrToken().chars().anyMatch(Character::isWhitespace)))
            throw new InvalidRequestException("QR token must be a nonempty value without whitespace");
        p.setInstalledAt(r.installedAt()); p.setRemovedAt(r.removedAt()); p.setPosition(r.position());
        p.setTourOrder(r.tourOrder()); p.setContextDescription(r.contextDescription());
        if (r.qrToken()!=null) p.setQrToken(r.qrToken());
        else if (p.getQrToken()==null) p.setQrToken(UUID.randomUUID().toString());
        if (r.qrPath()!=null && !r.qrPath().isBlank()) validateReference(r.qrPath());
        p.setQrPath(r.qrPath());
    }
    private void apply(DigitalContent d, DigitalContentRequest r) {
        validateReference(r.path());
        d.setType(r.type()); d.setPath(r.path()); d.setLanguage(r.language());
        d.setVersion(r.version()); d.setAutomaticallyGenerated(r.automaticallyGenerated());
    }
    private void validateReference(String value) {
        // Store references only; never fetch remote resources or accept executable/file URLs.
        try {
            URI uri=URI.create(value);
            if (value.contains("\\") || uri.getUserInfo()!=null || value.startsWith("//")
                    || (uri.getScheme()!=null && !Set.of("https","http").contains(uri.getScheme().toLowerCase(Locale.ROOT)))
                    || (uri.isAbsolute() && uri.getHost()==null)
                    || Arrays.asList(Objects.requireNonNullElse(uri.getPath(),"").split("/")).contains(".."))
                throw new IllegalArgumentException();
        } catch (IllegalArgumentException ex) { throw new InvalidRequestException("Use an HTTP(S) URL or relative content reference"); }
    }
    private ExhibitResponse exhibit(Exhibit e) {
        return new ExhibitResponse(e.getId(),e.getName(),e.getCreationPeriod(),e.getType(),e.getMaterial(),e.getOrigin(),e.getDescription(),
                authorships.findByExhibitIdOrderByAuthorId(e.getId()).stream().map(this::authorSummary).toList());
    }
    private ManagedExhibitResponse managedExhibit(Exhibit e) { return new ManagedExhibitResponse(exhibit(e),e.getInventoryNumber()); }
    private AuthorResponse author(Author a) { return new AuthorResponse(a.getId(),a.getFirstName(),a.getLastName(),a.getBirthYear(),a.getDeathYear(),a.getBiography()); }
    private AuthorSummary authorSummary(Authorship a) {
        return new AuthorSummary(a.getAuthor().getId(),(a.getAuthor().getFirstName()+" "+Objects.requireNonNullElse(a.getAuthor().getLastName(),"")).trim(),a.getContributionType());
    }
    private ParticipationResponse publicParticipation(ExhibitParticipation p) {
        return new ParticipationResponse(new CatalogReference(p.getExhibition().getId(),p.getExhibition().getName()),
                new CatalogReference(p.getExhibit().getId(),p.getExhibit().getName()),p.getContextDescription());
    }
    private ManagedParticipationResponse managedParticipation(ExhibitParticipation p) {
        return new ManagedParticipationResponse(p.getId(),p.getExhibition().getId(),p.getExhibit().getId(),p.getInstalledAt(),
                p.getRemovedAt(),p.getPosition(),p.getTourOrder(),p.getContextDescription(),p.getQrToken(),p.getQrPath());
    }
    private DigitalContentResponse content(DigitalContent d) {
        return new DigitalContentResponse(d.getId(),d.getParticipation().getId(),d.getType(),d.getPath(),d.getLanguage(),d.getVersion(),d.getAutomaticallyGenerated(),d.getCreatedAt());
    }
}
