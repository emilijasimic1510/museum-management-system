package rs.ac.uns.ftn.museum.audio;
import java.nio.file.Path;
public interface TtsService { void generate(String text, String language, Path output); }
