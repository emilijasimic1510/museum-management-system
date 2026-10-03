package rs.ac.uns.ftn.museum.manager;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;

public final class ManagerDtos {
    private ManagerDtos() {}
    public record CountRow(String key, String label, long count) {}
    public record MoneyRow(String key, String label, BigDecimal amount) {}
    public record Attendance(long total, List<CountRow> museums, List<CountRow> exhibitions,
            List<CountRow> terms, List<CountRow> days) {}
    public record Revenue(BigDecimal total, BigDecimal paidTicketValue, List<MoneyRow> exhibitions,
            List<MoneyRow> categories, List<MoneyRow> methods, List<MoneyRow> days) {}
    public record Popularity(long participationId, long exhibitId, String exhibit, String exhibition,
            long scans, long visits, Double averageSeconds, long measuredScans) {}
    public record EventRow(long id, String name, Instant start, String status, int capacity,
            long registrations, long registered, long attended, long cancelled, Double occupancyPercent) {}
    public record RatingRow(long exhibitionId, String exhibition, long count, double average) {}
    public record Comment(long id, String exhibition, int rating, String comment, Instant ratedAt) {}
    public record ContentRow(long participationId, String exhibit, String exhibition, String language, String type, long records) {}
    public record Dashboard(LocalDate from, LocalDate to, String zone, Instant generatedAt, Attendance attendance,
            Revenue revenue, List<CountRow> tickets, List<Popularity> popularity, List<EventRow> events,
            List<RatingRow> ratings, List<Comment> comments, boolean commentsTruncated,
            List<CountRow> transitions, List<ContentRow> contentInventory, List<String> limitations) {}
}
