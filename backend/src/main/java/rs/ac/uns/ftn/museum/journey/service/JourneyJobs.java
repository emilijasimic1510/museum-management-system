package rs.ac.uns.ftn.museum.journey.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.*;

@Configuration
@EnableScheduling
@ConditionalOnProperty(name="museum.automation.enabled",havingValue="true",matchIfMissing=true)
public class JourneyJobs {
    private static final Logger LOG=LoggerFactory.getLogger(JourneyJobs.class);
    private final JourneyMaintenance maintenance;
    private final GraphProjection graph;
    public JourneyJobs(JourneyMaintenance maintenance,GraphProjection graph) { this.maintenance=maintenance;this.graph=graph; }
    @Scheduled(fixedDelayString="${museum.automation.delay-ms:60000}",initialDelayString="${museum.automation.delay-ms:60000}")
    public void lifecycle() { maintenance.advanceStatuses(); }
    @Scheduled(fixedDelayString="${museum.graph.sync-delay-ms:60000}",initialDelayString="${museum.graph.sync-delay-ms:60000}")
    public void synchronize() {
        try { graph.synchronize(); }
        catch(RuntimeException ex) { LOG.warn("Graph synchronization unavailable; PostgreSQL projection will be retried on the next run"); }
    }
}
