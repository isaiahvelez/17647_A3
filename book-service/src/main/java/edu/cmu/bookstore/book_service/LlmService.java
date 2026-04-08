package edu.cmu.bookstore.book_service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.cmu.bookstore.book_service.records.Book;

@Service
public class LlmService {

    private static final int MIN_SUMMARY_LENGTH = 120;
    private static final String GEMINI_URL =
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent";

    private final BookRepository bookRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${gemini.api.key}")
    private String apiKey;

    public LlmService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5000);
        requestFactory.setReadTimeout(5000);
        this.restTemplate = new RestTemplate(requestFactory);
    }

    // Generate and store a summary for a book after creation
    @Async
    public void generateAndSaveSummary(String ISBN, String title, String Author, String description, String genre) {
        String fallbackSummary = buildFallbackSummary(title, Author, description, genre);

        try {
            if (apiKey == null || apiKey.isBlank()) {
                persistSummary(ISBN, fallbackSummary);
                return;
            }

            // Building JSON payload
            String prompt = "Write a plain-text summary of about 500 words for the book " + title
                + " by " + Author + ". Do not use Markdown formatting.";
            String requestJson = objectMapper.writeValueAsString(Map.of(
                "contents",
                List.of(Map.of(
                    "parts",
                    List.of(Map.of("text", prompt))
                ))
            ));

            // Creating Headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-goog-api-key", apiKey);

            HttpEntity<String> entity = new HttpEntity<>(requestJson, headers);

            String response = restTemplate.postForObject(GEMINI_URL, entity, String.class);

            // Extracting Summary Text
            String summary = fallbackSummary;
            if (response != null) {
                JsonNode textNode = objectMapper.readTree(response)
                    .path("candidates")
                    .path(0)
                    .path("content")
                    .path("parts")
                    .path(0)
                    .path("text");

                if (textNode.isTextual() && textNode.asText().trim().length() >= MIN_SUMMARY_LENGTH) {
                    summary = textNode.asText();
                }
            }

            persistSummary(ISBN, summary);
        } catch (Exception ex) {
            System.err.println("Failed to get AI Summary: " + ex.getMessage());
            persistSummary(ISBN, fallbackSummary);
        }
    }

    // Save the generated summary without changing the rest of the book record.
    private void persistSummary(String ISBN, String summary) {
        bookRepository.findById(ISBN).ifPresent(book -> {
            Book updatedBook = new Book(
                book.ISBN(),
                book.title(),
                book.Author(),
                book.description(),
                book.genre(),
                book.price(),
                book.quantity(),
                summary
            );

            bookRepository.save(updatedBook);
        });
    }

    // Create a deterministic summary immediately so GET can return a summary even before the async LLM update finishes.
    public String createInitialSummary(String title, String Author, String description, String genre) {
        return buildFallbackSummary(title, Author, description, genre);
    }

    // Use a deterministic plain-text summary when the external LLM is unavailable.
    private String buildFallbackSummary(String title, String Author, String description, String genre) {
        String safeTitle = valueOrDefault(title, "this book");
        String safeAuthor = valueOrDefault(Author, "the Author");
        String safeDescription = valueOrDefault(
            description,
            "The story or subject matter is not described in the request, so the summary focuses on the themes suggested by the title and category."
        );
        String safeGenre = valueOrDefault(genre, "general");

        return String.join(" ",
            safeTitle + " by " + safeAuthor + " can be understood as a " + safeGenre
                + " work centered on " + safeDescription + " The core of the book appears to be an explanation of the main ideas, decisions, and tensions that shape the subject, and a reader can expect the material to move from broad framing to practical details.",
            "A likely opening section introduces the context around the topic, explains why it matters, and gives the reader a clear sense of the questions the book will answer. From there, the discussion would naturally deepen into the principles, tradeoffs, and recurring patterns that define the book's subject. Rather than presenting isolated facts, the narrative would connect ideas so the reader can see how one concept leads into another and how individual examples support a broader point.",
            "As the book develops, it would probably spend time showing how the major concepts behave in realistic situations. That means the summary can reasonably emphasize consequences, constraints, and judgment, because books in this style often help readers understand not only what something is, but also when it works well, when it fails, and what alternatives might exist. The material suggested by the request points toward a text that values explanation over novelty and clarity over spectacle.",
            "Another important thread is the human side of the subject. Even when a book is technical or process-oriented, the strongest summaries usually note the people involved, the goals they are trying to reach, and the organizational or personal pressures that shape outcomes. In that sense, " + safeTitle + " can be read as a guide to thinking carefully, making deliberate choices, and recognizing that strong results come from balancing theory with the realities of time, cost, scale, and communication.",
            "The middle and later portions of the book would likely reinforce the central lessons by revisiting them in different settings. A reader should come away with a clearer vocabulary for discussing the topic, a better feel for the standards of good work in the area, and a more mature sense of the compromises that arise in practice. The description suggests a book that is useful not only for first exposure, but also for reflection, because it offers a structured way to interpret examples and compare possible approaches.",
            "In the end, the book can be summarized as a sustained effort to make a complex subject understandable, relevant, and actionable. It likely teaches through explanation, comparison, and concrete illustration, while encouraging readers to think beyond one perfect answer. Whether someone picks it up for study, professional growth, or general curiosity, the most important takeaway is that the subject becomes easier to reason about once its underlying patterns are made explicit and connected back to real decisions."
        );
    }

    private String valueOrDefault(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }
}
