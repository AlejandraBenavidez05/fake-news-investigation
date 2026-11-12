package com.konrad.konradquiz.contract;

import com.konrad.konradquiz.entity.Question;
import com.konrad.konradquiz.repository.QuestionRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Black-box contract test of the HTTP API consumed by the Angular frontend
 * (repo Josevermu/-fake-news-investigation-front-end, branch develop).
 *
 * <p>Boots the real application (Tomcat, Spring Security, CORS, Jackson) on a random
 * port backed by an in-memory H2 database in MSSQLServer mode, and replays every
 * request the frontend issues — same URLs, methods, headers and bodies.
 *
 * <p>Besides explicit assertions on the contract, every exchange is written in
 * normalized form to {@code target/contract-snapshot.txt} so the snapshot taken before
 * a refactor can be diffed against the one taken after it.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:contractdb;MODE=MSSQLServer;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "app.encryption.secret-key=test-secret-32chars-change!!!!!",
        "app.admin.password=" + ApiContractTest.ADMIN_PASSWORD,
        "app.cors.allowed-origins=http://localhost:3000,http://localhost:4200,http://localhost:5173"
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("API contract consumed by the Angular frontend")
class ApiContractTest {

    static final String ADMIN_PASSWORD = "test-admin-password";
    private static final String FRONTEND_ORIGIN = "http://localhost:4200";
    private static final String FOREIGN_ORIGIN = "http://evil.example.com";
    private static final int PROFILE_QUESTIONS = 18;
    private static final int NEWS_PER_SET = 12;

    /** Response headers whose values change between runs or JVMs. */
    private static final Set<String> VOLATILE_HEADERS = Set.of("date", "content-length", "keep-alive", "connection");
    /** JSON properties whose values depend on the clock. */
    private static final Set<String> VOLATILE_JSON_FIELDS = Set.of("timestamp", "registeredAt", "answeredAt");
    /** Session fields identical for every NEWS question of a participant (the rest depends on random selection). */
    private static final Set<String> NEWS_STABLE_FIELDS = Set.of("questionType", "presentationFormat", "phase",
            "category", "referenceApa", "supportingQuote", "scaleOptions");

    private static final List<String> CSV_HEADER = List.of(
            "participantId", "alias", "email", "sex", "age", "region", "feedbackTiming",
            "presentationFormat", "newsSet", "completionTimeSeconds", "registeredAt",
            "consentAcademicPurpose", "consentParticipationProcess", "consentDataProcessing",
            "consentNoRisk", "consentNoPayment", "consentProjectInfo",
            "questionCode", "questionType", "constructo", "subCategory", "subCategory2", "itemText",
            "correctAnswer", "referenceApa", "supportingQuote", "phase", "category", "novelty",
            "sourceVerificationUrl", "factCheckUrl", "originName", "fileName", "scaleOptions",
            "score", "questionOrder", "answeredAt", "answerType", "isCorrect", "sdtCategory");

    private final JsonMapper json = JsonMapper.builder().build();
    private final HttpClient http = HttpClient.newHttpClient();
    private final StringBuilder snapshot = new StringBuilder();

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private JsonNode firstSession;

    // ── Fixture ──────────────────────────────────────────────────────────────

    @BeforeAll
    void seedQuestions() {
        List<Question> questions = new ArrayList<>();
        for (int i = 1; i <= PROFILE_QUESTIONS; i++) {
            questions.add(Question.builder()
                    .questionCode("P" + i)
                    .constructo("Creador")
                    .subCategory(i % 2 == 0 ? "Humano Real" : null)
                    .itemText("Profile statement " + i + ", with \"quotes\"")
                    .questionType(Question.QuestionType.PROFILE)
                    .scaleOptions("nada,poco,bastante,mucho")
                    .referenceApa("Ref " + i)
                    .build());
        }
        for (Question.NewsSet set : Question.NewsSet.values()) {
            int exp = set == Question.NewsSet.ENVIRONMENT ? 1 : 2;
            for (int i = 1; i <= NEWS_PER_SET; i++) {
                questions.add(Question.builder()
                        .questionCode(i + "_exp" + exp)
                        .constructo("Contenido")
                        .itemText("News " + i + " of " + set)
                        .questionType(Question.QuestionType.NEWS)
                        .newsSet(set)
                        // leave one question per set without ground truth to cover null handling
                        .correctAnswer(i == NEWS_PER_SET ? null
                                : (i % 2 == 0 ? Question.CorrectAnswer.REAL : Question.CorrectAnswer.FAKE))
                        .phase("FASE 1")
                        .category("DETECCION")
                        .novelty(i % 3 == 0 ? "Vieja" : "Nueva")
                        .sourceVerificationUrl("https://source.example/" + i)
                        .factCheckUrl(i % 2 == 0 ? "https://check.example/" + i : null)
                        .originName("Origin " + i)
                        .fileName(i + "_exp" + exp + ".png")
                        .build());
            }
        }
        questionRepository.saveAll(questions);
    }

    @AfterAll
    void writeSnapshot() throws IOException {
        Path target = Path.of("target", "contract-snapshot.txt");
        Files.createDirectories(target.getParent());
        Files.writeString(target, snapshot.toString(), StandardCharsets.UTF_8);
    }

    // ── CORS preflight (browser → backend) ───────────────────────────────────

    @Test
    @Order(1)
    void preflight_registerFromFrontendOrigin_isAllowed() throws Exception {
        HttpResponse<String> res = preflight("/api/v1/participants/register", FRONTEND_ORIGIN, "POST", "content-type");
        assertThat(res.statusCode()).isEqualTo(200);
        assertThat(header(res, "Access-Control-Allow-Origin")).isEqualTo(FRONTEND_ORIGIN);
        assertThat(header(res, "Access-Control-Allow-Credentials")).isEqualTo("true");
        assertThat(header(res, "Access-Control-Allow-Methods")).contains("POST");
    }

    @Test
    @Order(2)
    void preflight_adminExportWithAuthorizationHeader_isAllowed() throws Exception {
        HttpResponse<String> res = preflight("/api/v1/admin/export/csv", FRONTEND_ORIGIN, "GET", "authorization");
        assertThat(res.statusCode()).isEqualTo(200);
        assertThat(header(res, "Access-Control-Allow-Origin")).isEqualTo(FRONTEND_ORIGIN);
        assertThat(header(res, "Access-Control-Allow-Headers")).containsIgnoringCase("authorization");
    }

    @Test
    @Order(3)
    void preflight_submitBatchAndReward_isAllowed() throws Exception {
        assertThat(preflight("/api/v1/participants/1/answers/batch", FRONTEND_ORIGIN, "POST", "content-type")
                .statusCode()).isEqualTo(200);
        assertThat(preflight("/api/v1/participants/1/reward", FRONTEND_ORIGIN, "POST", "content-type")
                .statusCode()).isEqualTo(200);
    }

    @Test
    @Order(4)
    void preflight_foreignOriginOrDisallowedMethod_isRejected() throws Exception {
        assertThat(preflight("/api/v1/participants/register", FOREIGN_ORIGIN, "POST", "content-type")
                .statusCode()).isEqualTo(403);
        assertThat(preflight("/api/v1/participants/register", FRONTEND_ORIGIN, "DELETE", "content-type")
                .statusCode()).isEqualTo(403);
    }

    // ── POST /participants/register ──────────────────────────────────────────

    @Test
    @Order(10)
    void register_returns201WithExperimentSession() throws Exception {
        HttpResponse<String> res = send("register-ok", "POST", "/api/v1/participants/register",
                registrationBody("ana@example.com", true), null);

        assertThat(res.statusCode()).isEqualTo(201);
        assertThat(header(res, "Content-Type")).startsWith("application/json");
        assertThat(header(res, "Access-Control-Allow-Origin")).isEqualTo(FRONTEND_ORIGIN);

        JsonNode body = json.readTree(res.body());
        assertThat(fieldNames(body)).containsExactlyInAnyOrder("participantId", "alias", "feedbackTiming",
                "presentationFormat", "newsSet", "profileQuestions", "newsPart1Questions", "newsPart2Questions");
        assertThat(body.get("participantId").isIntegralNumber()).isTrue();
        assertThat(body.get("feedbackTiming").asString()).isEqualTo("GROUP_A");
        assertThat(body.get("presentationFormat").asString()).isEqualTo("INSTAGRAM");
        assertThat(body.get("newsSet").asString()).isEqualTo("ENVIRONMENT");

        assertSessionList(body.get("profileQuestions"), PROFILE_QUESTIONS, "TEXT");
        assertSessionList(body.get("newsPart1Questions"), 8, "INSTAGRAM");
        assertSessionList(body.get("newsPart2Questions"), 8, "INSTAGRAM");

        Set<String> part1 = codes(body.get("newsPart1Questions"));
        Set<String> part2 = codes(body.get("newsPart2Questions"));
        assertThat(part2.stream().filter(part1::contains).count()).isEqualTo(4);
        assertThat(part2.stream().filter(c -> !part1.contains(c)).count()).isEqualTo(4);

        JsonNode news = body.get("newsPart1Questions").get(0);
        assertThat(fieldNames(news)).containsExactlyInAnyOrder("questionCode", "questionType", "itemText", "assignedOrder",
                "presentationFormat", "correctAnswer", "referenceApa", "supportingQuote", "phase", "category",
                "novelty", "sourceVerificationUrl", "factCheckUrl", "originName", "fileName", "scaleOptions");
        // null properties are serialized as explicit nulls, not omitted
        assertThat(body.get("profileQuestions").get(0).get("correctAnswer").isNull()).isTrue();
        assertThat(news.get("scaleOptions").isNull()).isTrue();

        firstSession = body;
    }

    @Test
    @Order(11)
    void register_secondParticipant_isBalancedIntoNextGroupAndSet() throws Exception {
        HttpResponse<String> res = send("register-second", "POST", "/api/v1/participants/register",
                registrationBody("bruno@example.com", true), null);
        assertThat(res.statusCode()).isEqualTo(201);
        JsonNode body = json.readTree(res.body());
        assertThat(body.get("feedbackTiming").asString()).isEqualTo("GROUP_A");
        assertThat(body.get("presentationFormat").asString()).isEqualTo("WHATSAPP");
        assertThat(body.get("newsSet").asString()).isEqualTo("TECHNOLOGY");
    }

    @Test
    @Order(12)
    void register_duplicateEmailIgnoringCase_returns409() throws Exception {
        HttpResponse<String> res = send("register-duplicate", "POST", "/api/v1/participants/register",
                registrationBody("ANA@Example.com", true), null);
        assertError(res, 409, "Business Rule Violation", "A participant with this email already exists.");
    }

    @Test
    @Order(13)
    void register_consentNotGiven_returns409() throws Exception {
        HttpResponse<String> res = send("register-no-consent", "POST", "/api/v1/participants/register",
                registrationBody("carla@example.com", false), null);
        assertError(res, 409, "Business Rule Violation",
                "All consent declarations must be accepted to participate.");
    }

    @Test
    @Order(14)
    void register_invalidFields_returns400() throws Exception {
        ObjectNode body = registrationBody("not-an-email", true);
        body.put("age", 3);
        body.putNull("sex");
        HttpResponse<String> res = send("register-invalid", "POST", "/api/v1/participants/register", body, null);
        assertThat(res.statusCode()).isEqualTo(400);
        JsonNode error = json.readTree(res.body());
        assertErrorShape(error);
        assertThat(error.get("error").asString()).isEqualTo("Validation Failed");
        assertThat(error.get("message").asString()).contains("email: ", "age: ", "sex: ");
    }

    @Test
    @Order(15)
    void register_unknownEnumOrMalformedJson_preservesCurrentStatus() throws Exception {
        ObjectNode body = registrationBody("dario@example.com", true);
        body.put("sex", "UNKNOWN");
        HttpResponse<String> badEnum = send("register-bad-enum", "POST", "/api/v1/participants/register", body, null);
        HttpResponse<String> malformed = sendRaw("register-malformed", "POST", "/api/v1/participants/register",
                "{not json", null);
        assertThat(badEnum.statusCode()).isEqualTo(malformed.statusCode());
    }

    // ── GET /participants/{id} ───────────────────────────────────────────────

    @Test
    @Order(20)
    void getParticipant_returns200() throws Exception {
        long id = firstSession.get("participantId").asLong();
        HttpResponse<String> res = send("participant-get", "GET", "/api/v1/participants/" + id, null, null);
        assertThat(res.statusCode()).isEqualTo(200);
        JsonNode body = json.readTree(res.body());
        assertThat(fieldNames(body)).containsExactlyInAnyOrder("id", "alias", "email", "sex", "age", "registeredAt",
                "region", "completionTimeSeconds", "feedbackTiming", "presentationFormat");
        assertThat(body.get("email").asString()).isEqualTo("ana@example.com");
        assertThat(body.get("completionTimeSeconds").isNull()).isTrue();
    }

    @Test
    @Order(21)
    void getParticipant_unknownId_returns404() throws Exception {
        HttpResponse<String> res = send("participant-404", "GET", "/api/v1/participants/999999", null, null);
        assertError(res, 404, "Not Found", "Participant not found: 999999");
    }

    @Test
    @Order(22)
    void getParticipant_nonNumericId_preservesCurrentStatus() throws Exception {
        send("participant-bad-id", "GET", "/api/v1/participants/abc", null, null);
    }

    // ── POST /participants/{id}/answers/batch ────────────────────────────────

    @Test
    @Order(30)
    void submitBatch_wrongCounts_returns409() throws Exception {
        long id = firstSession.get("participantId").asLong();
        ObjectNode body = batchBody(firstSession);
        ((ArrayNode) body.get("answers")).remove(0);
        HttpResponse<String> res = send("batch-missing-profile", "POST",
                "/api/v1/participants/" + id + "/answers/batch", body, null);
        assertError(res, 409, "Business Rule Violation",
                "Must answer all 18 profile questions. Received: 17");

        ObjectNode part1 = batchBody(firstSession);
        ((ArrayNode) part1.get("answers")).remove(PROFILE_QUESTIONS);
        assertError(send("batch-missing-part1", "POST", "/api/v1/participants/" + id + "/answers/batch", part1, null),
                409, "Business Rule Violation", "Must answer exactly 8 Part 1 news questions. Received: 7");

        ObjectNode part2 = batchBody(firstSession);
        ArrayNode part2Answers = (ArrayNode) part2.get("answers");
        part2Answers.remove(part2Answers.size() - 1);
        assertError(send("batch-missing-part2", "POST", "/api/v1/participants/" + id + "/answers/batch", part2, null),
                409, "Business Rule Violation", "Must answer exactly 8 Part 2 news questions. Received: 7");
    }

    @Test
    @Order(31)
    void submitBatch_unknownQuestion_returns404() throws Exception {
        long id = firstSession.get("participantId").asLong();
        ObjectNode body = batchBody(firstSession);
        ((ObjectNode) body.get("answers").get(3)).put("questionCode", "NOPE");
        HttpResponse<String> res = send("batch-unknown-question", "POST",
                "/api/v1/participants/" + id + "/answers/batch", body, null);
        assertError(res, 404, "Not Found", "Question not found: NOPE");
    }

    @Test
    @Order(32)
    void submitBatch_invalidPayload_returns400() throws Exception {
        long id = firstSession.get("participantId").asLong();
        ObjectNode empty = json.createObjectNode();
        empty.putArray("answers");
        empty.put("startedAt", Instant.now().getEpochSecond());
        HttpResponse<String> res = send("batch-empty", "POST",
                "/api/v1/participants/" + id + "/answers/batch", empty, null);
        assertThat(res.statusCode()).isEqualTo(400);
        assertThat(json.readTree(res.body()).get("error").asString()).isEqualTo("Validation Failed");

        ObjectNode outOfRange = batchBody(firstSession);
        ((ObjectNode) outOfRange.get("answers").get(0)).put("score", 101);
        HttpResponse<String> res2 = send("batch-score-out-of-range", "POST",
                "/api/v1/participants/" + id + "/answers/batch", outOfRange, null);
        assertThat(res2.statusCode()).isEqualTo(400);
        assertThat(json.readTree(res2.body()).get("message").asString()).startsWith("answers[0].score: ");
    }

    @Test
    @Order(33)
    void submitBatch_unknownParticipant_returns404() throws Exception {
        HttpResponse<String> res = send("batch-unknown-participant", "POST",
                "/api/v1/participants/999999/answers/batch", batchBody(firstSession), null);
        assertError(res, 404, "Not Found", "Participant not found: 999999");
    }

    /**
     * KNOWN PRE-EXISTING DEFECT (documented, intentionally not fixed here):
     * {@code Answer} maps UNIQUE(participant_id, question_code) while the session design repeats
     * 4 Part 1 questions in Part 2. On any schema generated from the entities every valid
     * submission violates the constraint and the generic handler answers 500.
     */
    @Test
    @Order(34)
    void submitBatch_onEntityGeneratedSchema_failsOnRepeatedPart2Questions() throws Exception {
        long id = firstSession.get("participantId").asLong();
        HttpResponse<String> res = send("batch-unique-constraint", "POST",
                "/api/v1/participants/" + id + "/answers/batch", batchBody(firstSession), null);
        assertError(res, 500, "Internal Server Error", "An unexpected error occurred. Please try again.");
    }

    /** Simulates a production schema without the unique constraint to exercise the success path. */
    @Test
    @Order(35)
    void submitBatch_frontendPayloadWithNovelty_returns201() throws Exception {
        jdbcTemplate.execute("ALTER TABLE answers DROP CONSTRAINT uq_answer_participant_question");
        long id = firstSession.get("participantId").asLong();
        ObjectNode body = batchBody(firstSession);
        body.put("startedAt", Instant.now().getEpochSecond() - 100);
        HttpResponse<String> res = send("batch-ok", "POST",
                "/api/v1/participants/" + id + "/answers/batch", body, null);
        assertThat(res.statusCode()).isEqualTo(201);
        JsonNode result = json.readTree(res.body());
        assertThat(fieldNames(result)).containsExactlyInAnyOrder("participantId", "savedCount");
        assertThat(result.get("participantId").asLong()).isEqualTo(id);
        assertThat(result.get("savedCount").asInt()).isEqualTo(PROFILE_QUESTIONS + 16);

        JsonNode participant = json.readTree(
                send(null, "GET", "/api/v1/participants/" + id, null, null).body());
        assertThat(participant.get("completionTimeSeconds").asLong()).isBetween(100L, 110L);
    }

    @Test
    @Order(36)
    void submitBatch_twice_returns409() throws Exception {
        long id = firstSession.get("participantId").asLong();
        HttpResponse<String> res = send("batch-twice", "POST",
                "/api/v1/participants/" + id + "/answers/batch", batchBody(firstSession), null);
        assertError(res, 409, "Business Rule Violation", "Participant has already submitted answers.");
    }

    // ── POST /participants/{id}/reward (called by the frontend, not implemented) ─

    @Test
    @Order(40)
    void selectReward_hasNoBackendEndpoint_preservesCurrentStatus() throws Exception {
        long id = firstSession.get("participantId").asLong();
        ObjectNode body = json.createObjectNode().put("rewardId", 1);
        send("reward", "POST", "/api/v1/participants/" + id + "/reward", body, null);
    }

    // ── Admin endpoints (HTTP Basic) ─────────────────────────────────────────

    @Test
    @Order(50)
    void exportCsv_withoutOrWithWrongCredentials_returns401() throws Exception {
        assertThat(send("export-no-auth", "GET", "/api/v1/admin/export/csv", null, null).statusCode())
                .isEqualTo(401);
        assertThat(send("export-bad-auth", "GET", "/api/v1/admin/export/csv", null, basic("admin", "wrong"))
                .statusCode()).isEqualTo(401);
        assertThat(send("export-bad-user", "GET", "/api/v1/admin/export/csv", null, basic("root", ADMIN_PASSWORD))
                .statusCode()).isEqualTo(401);
    }

    @Test
    @Order(51)
    void exportCsv_withAdminCredentials_returnsCsvParsedByDashboard() throws Exception {
        HttpResponse<String> res = send("export-ok", "GET", "/api/v1/admin/export/csv", null,
                basic("admin", ADMIN_PASSWORD));
        assertThat(res.statusCode()).isEqualTo(200);
        assertThat(header(res, "Content-Type")).isEqualTo("text/csv;charset=UTF-8");
        assertThat(header(res, "Content-Disposition")).isEqualTo("attachment; filename=\"konradquiz_export.csv\"");
        assertThat(header(res, "Access-Control-Allow-Origin")).isEqualTo(FRONTEND_ORIGIN);

        List<String> lines = res.body().lines().toList();
        assertThat(lines.get(0)).isEqualTo(String.join(",", CSV_HEADER));
        assertThat(lines).hasSize(1 + PROFILE_QUESTIONS + 16);
        for (String line : lines.subList(1, lines.size())) {
            assertThat(line).startsWith("\"").endsWith("\"");
        }
        assertThat(res.body()).contains("\"ana@example.com\"", "\"Profile statement 1, with \"\"quotes\"\"\"");

        // Independent oracle for the computed columns and the joined question data of every row
        for (String line : lines.subList(1, lines.size())) {
            List<String> cells = csvCells(line);
            assertThat(cells).hasSize(CSV_HEADER.size());
            String code = cell(cells, "questionCode");
            String correct = cell(cells, "correctAnswer");
            int score = Integer.parseInt(cell(cells, "score"));
            Question seeded = questionRepository.findById(code).orElseThrow();
            assertThat(cell(cells, "itemText")).isEqualTo(seeded.getItemText());
            assertThat(correct).isEqualTo(seeded.getCorrectAnswer() == null ? "" : seeded.getCorrectAnswer().name());
            assertThat(cell(cells, "isCorrect")).isEqualTo(String.valueOf(
                    ("REAL".equals(correct) && score >= 50) || ("FAKE".equals(correct) && score < 50)));
            String expectedSdt = correct.isEmpty() ? ""
                    : score == 50 ? "UNSURE"
                    : "FAKE".equals(correct) ? (score < 50 ? "HIT" : "MISS")
                    : (score < 50 ? "FALSE_ALARM" : "CORRECT_RESPONSE");
            assertThat(cell(cells, "sdtCategory")).isEqualTo(expectedSdt);
            assertThat(cell(cells, "completionTimeSeconds")).matches("1(0\\d|10)");
        }
    }

    private static List<String> csvCells(String line) {
        // every cell is quoted; the seed data contains no '","' sequence
        String inner = line.substring(1, line.length() - 1);
        return Arrays.stream(inner.split("\",\"", -1)).map(c -> c.replace("\"\"", "\"")).toList();
    }

    private static String cell(List<String> cells, String column) {
        return cells.get(CSV_HEADER.indexOf(column));
    }

    @Test
    @Order(52)
    void updateCorrectAnswer_returns200() throws Exception {
        ObjectNode body = json.createObjectNode().put("correctAnswer", "FAKE");
        HttpResponse<String> res = send("admin-patch-ok", "PATCH", "/api/v1/admin/questions/12_exp1/answer",
                body, basic("admin", ADMIN_PASSWORD));
        assertThat(res.statusCode()).isEqualTo(200);
        JsonNode result = json.readTree(res.body());
        assertThat(fieldNames(result)).containsExactlyInAnyOrder("questionCode", "itemText", "correctAnswer");
        assertThat(result.get("correctAnswer").asString()).isEqualTo("FAKE");

        assertError(send("admin-patch-404", "PATCH", "/api/v1/admin/questions/NOPE/answer", body,
                basic("admin", ADMIN_PASSWORD)), 404, "Not Found", "Question not found: NOPE");
        assertThat(send("admin-patch-400", "PATCH", "/api/v1/admin/questions/12_exp1/answer",
                json.createObjectNode(), basic("admin", ADMIN_PASSWORD)).statusCode()).isEqualTo(400);
        assertThat(send("admin-patch-401", "PATCH", "/api/v1/admin/questions/12_exp1/answer", body, null)
                .statusCode()).isEqualTo(401);
    }

    // ── Everything else is denied ────────────────────────────────────────────

    @Test
    @Order(60)
    void unmappedRoutes_preserveCurrentStatus() throws Exception {
        send("deny-other-prefix", "GET", "/api/v1/questions", null, null);
        send("deny-root", "GET", "/", null, null);
        send("admin-unknown-route", "GET", "/api/v1/admin/unknown", null, basic("admin", ADMIN_PASSWORD));
        send("participants-wrong-method", "DELETE", "/api/v1/participants/1", null, null);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private ObjectNode registrationBody(String email, boolean consent) {
        ObjectNode body = json.createObjectNode();
        body.put("alias", "Tester");
        body.put("email", email);
        body.put("sex", "FEMALE");
        body.put("age", 30);
        body.put("region", "Bogotá");
        for (String c : List.of("consentAcademicPurpose", "consentParticipationProcess", "consentDataProcessing",
                "consentNoRisk", "consentNoPayment", "consentProjectInfo")) {
            body.put(c, true);
        }
        body.put("consentNoPayment", consent);
        return body;
    }

    /** Builds the batch exactly as SurveyService.submitBatch() does (including the extra "novelty" key). */
    private ObjectNode batchBody(JsonNode session) {
        ObjectNode body = json.createObjectNode();
        ArrayNode answers = body.putArray("answers");
        int score = 0;
        for (JsonNode q : session.get("profileQuestions")) {
            answers.addObject().put("questionCode", q.get("questionCode").asString()).put("answerType", "PROFILE")
                    .put("score", (score++ % 4) * 33).put("questionOrder", q.get("assignedOrder").asInt())
                    .putNull("novelty");
        }
        Set<String> part1 = codes(session.get("newsPart1Questions"));
        for (JsonNode q : session.get("newsPart1Questions")) {
            answers.addObject().put("questionCode", q.get("questionCode").asString())
                    .put("answerType", "FAKE_DETECTION").put("score", (score++ * 17) % 101)
                    .put("questionOrder", q.get("assignedOrder").asInt()).putNull("novelty");
        }
        for (JsonNode q : session.get("newsPart2Questions")) {
            String code = q.get("questionCode").asString();
            answers.addObject().put("questionCode", code).put("answerType", "MEMORY_TEST")
                    .put("score", (score++ * 23) % 101).put("questionOrder", q.get("assignedOrder").asInt())
                    .put("novelty", part1.contains(code) ? "V" : "N");
        }
        // boundary scores for the isCorrect / SDT computations (50 = unsure, 49 = says FAKE)
        ((ObjectNode) answers.get(PROFILE_QUESTIONS)).put("score", 50);
        ((ObjectNode) answers.get(PROFILE_QUESTIONS + 1)).put("score", 49);
        ((ObjectNode) answers.get(PROFILE_QUESTIONS + 8)).put("score", 50);
        body.put("startedAt", Instant.now().getEpochSecond() - 100);
        return body;
    }

    private void assertSessionList(JsonNode list, int size, String format) {
        assertThat(list.isArray()).isTrue();
        assertThat(list.size()).isEqualTo(size);
        List<Integer> orders = new ArrayList<>();
        list.forEach(q -> {
            orders.add(q.get("assignedOrder").asInt());
            assertThat(q.get("presentationFormat").asString()).isEqualTo(format);
        });
        assertThat(orders).containsExactlyElementsOf(IntStream.rangeClosed(1, size).boxed().toList());
    }

    private void assertError(HttpResponse<String> res, int status, String error, String message) throws IOException {
        assertThat(res.statusCode()).isEqualTo(status);
        JsonNode body = json.readTree(res.body());
        assertErrorShape(body);
        assertThat(body.get("status").asInt()).isEqualTo(status);
        assertThat(body.get("error").asString()).isEqualTo(error);
        assertThat(body.get("message").asString()).isEqualTo(message);
    }

    private void assertErrorShape(JsonNode body) {
        assertThat(fieldNames(body)).containsExactlyInAnyOrder("status", "error", "message", "path", "timestamp");
    }

    private static Set<String> codes(JsonNode list) {
        Set<String> codes = new java.util.HashSet<>();
        list.forEach(q -> codes.add(q.get("questionCode").asString()));
        return codes;
    }

    private static List<String> fieldNames(JsonNode node) {
        return new ArrayList<>(node.propertyNames());
    }

    private static String basic(String user, String password) {
        return "Basic " + Base64.getEncoder()
                .encodeToString((user + ":" + password).getBytes(StandardCharsets.UTF_8));
    }

    private static String header(HttpResponse<?> res, String name) {
        return res.headers().firstValue(name).orElse(null);
    }

    private HttpResponse<String> preflight(String path, String origin, String method, String requestHeaders)
            throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                .header("Origin", origin)
                .header("Access-Control-Request-Method", method)
                .header("Access-Control-Request-Headers", requestHeaders)
                .build();
        HttpResponse<String> res = http.send(request, HttpResponse.BodyHandlers.ofString());
        record("preflight " + method + " " + path + " origin=" + origin + " headers=" + requestHeaders, res);
        return res;
    }

    private HttpResponse<String> send(String label, String method, String path, JsonNode body, String authorization)
            throws Exception {
        return sendRaw(label, method, path, body == null ? null : json.writeValueAsString(body), authorization);
    }

    private HttpResponse<String> sendRaw(String label, String method, String path, String body, String authorization)
            throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .method(method, body == null
                        ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .header("Origin", FRONTEND_ORIGIN)
                .header("Accept", "application/json, text/plain, */*");
        if (body != null) {
            builder.header("Content-Type", "application/json");
        }
        if (authorization != null) {
            builder.header("Authorization", authorization);
        }
        HttpResponse<String> res = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        if (label != null) {
            record(label + " " + method + " " + path, res);
        }
        return res;
    }

    // ── Snapshot normalization ───────────────────────────────────────────────

    private void record(String label, HttpResponse<String> res) {
        snapshot.append("### ").append(label).append('\n');
        snapshot.append("status: ").append(res.statusCode()).append('\n');
        Map<String, List<String>> headers = new TreeMap<>(res.headers().map());
        headers.forEach((name, values) -> {
            if (!name.startsWith(":") && !VOLATILE_HEADERS.contains(name.toLowerCase())) {
                snapshot.append("header ").append(name.toLowerCase()).append(": ").append(values).append('\n');
            }
        });
        snapshot.append("body: ").append(normalizeBody(res)).append("\n\n");
    }

    private String normalizeBody(HttpResponse<String> res) {
        String body = res.body();
        String contentType = header(res, "Content-Type");
        if (body == null || body.isEmpty()) {
            return "<empty>";
        }
        if (contentType != null && contentType.startsWith("text/csv")) {
            return normalizeCsv(body);
        }
        try {
            JsonNode node = json.readTree(body);
            normalizeJson(node);
            return json.writeValueAsString(node);
        } catch (RuntimeException notJson) {
            return body;
        }
    }

    private void normalizeJson(JsonNode node) {
        if (node instanceof ObjectNode object) {
            for (String name : new ArrayList<>(object.propertyNames())) {
                JsonNode value = object.get(name);
                if (VOLATILE_JSON_FIELDS.contains(name) && !value.isNull()) {
                    object.put(name, "<" + name + ">");
                } else if ("assignedOrder".equals(name)) {
                    object.put(name, "<order>");
                } else if (isNewsQuestion(object) && !NEWS_STABLE_FIELDS.contains(name)) {
                    // which news land in Part 1 / Part 2 is random by design: keep structure, mask identity
                    object.put(name, "<news>");
                } else if ("message".equals(name) && value.isString() && value.asString().contains(": ")
                        && object.has("error") && "Validation Failed".equals(object.get("error").asString())) {
                    // field-error order is not guaranteed by Bean Validation
                    object.put(name, Arrays.stream(value.asString().split(", ")).sorted()
                            .collect(Collectors.joining(", ")));
                } else {
                    normalizeJson(value);
                }
            }
        } else if (node instanceof ArrayNode array) {
            array.forEach(this::normalizeJson);
            // session lists are shuffled on purpose: compare them as sets
            List<JsonNode> sorted = new ArrayList<>();
            array.forEach(sorted::add);
            if (!sorted.isEmpty() && sorted.get(0).has("questionCode")) {
                sorted.sort(Comparator.comparing(n -> n.get("questionCode").asString()));
                array.removeAll();
                sorted.forEach(array::add);
            }
        }
    }

    private static boolean isNewsQuestion(ObjectNode object) {
        return object.has("questionCode") && object.has("questionType")
                && "NEWS".equals(object.get("questionType").asString());
    }

    /**
     * Rows are ordered by participant and questionOrder, which has ties: compare as a sorted multiset.
     * NEWS rows depend on the random Part 1 / Part 2 selection, so their question-specific cells are masked
     * here; their content is verified by the oracle in {@link #exportCsv_withAdminCredentials_returnsCsvParsedByDashboard()}.
     */
    private String normalizeCsv(String csv) {
        List<String> lines = csv.lines().toList();
        int questionType = CSV_HEADER.indexOf("questionType");
        Set<Integer> volatileCells = Set.of(CSV_HEADER.indexOf("registeredAt"), CSV_HEADER.indexOf("answeredAt"),
                CSV_HEADER.indexOf("completionTimeSeconds"));
        Set<Integer> newsSelectionCells = IntStream.range(CSV_HEADER.indexOf("questionCode"), CSV_HEADER.size())
                .filter(i -> i != questionType && i != CSV_HEADER.indexOf("answerType"))
                .boxed().collect(Collectors.toSet());
        List<String> rows = lines.subList(1, lines.size()).stream()
                .map(line -> {
                    String[] cells = line.split("\",\"", -1);
                    boolean news = "NEWS".equals(cells[questionType]);
                    for (int i = 0; i < cells.length; i++) {
                        if (volatileCells.contains(i) || (news && newsSelectionCells.contains(i))) {
                            cells[i] = "<masked>";
                        }
                    }
                    return String.join("\",\"", cells);
                })
                .sorted()
                .toList();
        String separator = csv.contains("\r\n") ? "CRLF" : "LF";
        return "line-separator=" + separator + "\n" + lines.get(0) + "\n" + String.join("\n", rows);
    }
}
