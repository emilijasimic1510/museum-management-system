package rs.ac.uns.ftn.museum.catalog.service;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.ac.uns.ftn.museum.catalog.domain.*;
import rs.ac.uns.ftn.museum.catalog.dto.PriceManagementDtos.*;
import rs.ac.uns.ftn.museum.catalog.exception.*;

@Service
@Transactional
public class PriceManagementService {
    @PersistenceContext private EntityManager em;
    @Transactional(readOnly=true)
    public List<Response> list(Long exhibitionId) {
        require(exhibitionId,false);
        return entries(exhibitionId).stream().map(this::response).toList();
    }
    public Response create(Long exhibitionId,Create r) {
        Exhibition exhibition=require(exhibitionId,true);
        // Match PostgreSQL timestamp precision so keys round-trip through the API exactly.
        Instant from=r.validFrom().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        Instant to=normalize(r.validTo());String category=r.category().trim();
        ExhibitionPriceId id=new ExhibitionPriceId(exhibitionId,from,category);
        if(em.find(ExhibitionPrice.class,id)!=null)throw new ConflictException("Price period already exists");
        validate(id,to,null);
        ExhibitionPrice p=new ExhibitionPrice();p.setId(id);p.setExhibition(exhibition);
        p.setValidTo(to);p.setPrice(r.price());p.setNote(r.note());em.persist(p);em.flush();return response(p);
    }
    public Response update(Long exhibitionId,String category,Instant from,Update r) {
        require(exhibitionId,true);
        ExhibitionPriceId id=new ExhibitionPriceId(exhibitionId,from,category);
        ExhibitionPrice p=em.find(ExhibitionPrice.class,id,LockModeType.PESSIMISTIC_WRITE);
        if(p==null)throw new NotFoundException("Price period not found");
        Instant to=normalize(r.validTo());validate(id,to,id);
        if(to!=null && em.createQuery("select count(t) from Ticket t where t.price.id=:id and t.purchasedAt>=:end",Long.class)
                .setParameter("id",id).setParameter("end",to).getSingleResult()>0)
            throw new ConflictException("Price validity must preserve existing purchases");
        p.setValidTo(to);p.setPrice(r.price());p.setNote(r.note());em.flush();return response(p);
    }
    private void validate(ExhibitionPriceId id,Instant to,ExhibitionPriceId exclude) {
        if(to!=null)ScheduleRules.interval(id.getValidFrom(),to);
        for(ExhibitionPrice p:entries(id.getExhibitionId())) {
            if(p.getId().equals(exclude) || !p.getId().getCategory().equals(id.getCategory()))continue;
            if((to==null || p.getId().getValidFrom().isBefore(to)) && (p.getValidTo()==null || id.getValidFrom().isBefore(p.getValidTo())))
                throw new ConflictException("Price periods for the same category cannot overlap");
        }
    }
    private Exhibition require(Long id,boolean lock) {
        Exhibition e=lock?em.find(Exhibition.class,id,LockModeType.PESSIMISTIC_WRITE):em.find(Exhibition.class,id);
        if(e==null)throw new NotFoundException("Exhibition not found");return e;
    }
    private List<ExhibitionPrice> entries(Long id) {
        return em.createQuery("select p from ExhibitionPrice p where p.exhibition.id=:id order by p.id.category,p.id.validFrom desc",ExhibitionPrice.class).setParameter("id",id).getResultList();
    }
    private Instant normalize(Instant value){return value==null?null:value.truncatedTo(java.time.temporal.ChronoUnit.MICROS);}
    private Response response(ExhibitionPrice p){return new Response(p.getId().getExhibitionId(),p.getId().getCategory(),p.getId().getValidFrom(),p.getValidTo(),p.getPrice(),p.getNote());}
}
