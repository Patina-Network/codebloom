package org.patinanetwork.codebloom.common.db.repos.question;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.patinanetwork.codebloom.common.db.repos.question.topic.QuestionTopicRepository;
import org.patinanetwork.codebloom.common.db.repos.question.topic.service.QuestionTopicService;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

@EnabledIfEnvironmentVariable(named = "TOPIC_LOOKUP_TEST_URL", matches = ".+")
public class QuestionTopicLookupIntegrationTest {

    @Test
    void exclusionPersistsWithoutChangingHistoricalMetadata() throws Exception {
        var ds = new SingleConnectionDataSource(System.getenv("TOPIC_LOOKUP_TEST_URL"), true);
        try {
            var jdbc = JdbcClient.create(ds);
            jdbc.sql("""
                    CREATE TEMP TABLE "Question" (
                        id uuid PRIMARY KEY, "userId" uuid, "questionSlug" text,
                        "questionDifficulty" text DEFAULT 'Easy', "questionNumber" smallint,
                        "questionLink" text, "pointsAwarded" integer, "questionTitle" text,
                        description text, "acceptanceRate" real DEFAULT 0,
                        "createdAt" timestamptz DEFAULT NOW(), "submittedAt" timestamptz DEFAULT NOW(),
                        runtime text, memory text, code text, language text, "submissionId" text
                    )
                    """).update();
            jdbc.sql("CREATE TEMP TABLE \"QuestionTopic\" (\"questionId\" uuid)")
                    .update();
            jdbc.sql(Files.readString(Path.of("db/migration/V0082__Skip_topic_lookup_for_obsolete_question.SQL")))
                    .update();
            var missingId = UUID.randomUUID();
            var validId = UUID.randomUUID();
            for (var id : new UUID[] {missingId, validId}) {
                jdbc.sql("""
                        INSERT INTO "Question" (id, "questionNumber", "questionSlug", "questionTitle",
                            "questionLink", description, code)
                        VALUES (:id, 596, 'original-slug', 'Original title', 'original link',
                            'original description', 'original code')
                        """).param("id", id).update();
            }
            var repository = new QuestionSqlRepository(
                    ds, jdbc, mock(QuestionTopicRepository.class), mock(QuestionTopicService.class));
            var before = repository.getQuestionById(missingId.toString()).orElseThrow();
            assertEquals(2, repository.getAllQuestionsWithNoTopics().size());

            repository.skipTopicLookup(missingId.toString());
            repository.skipTopicLookup(missingId.toString());
            for (int run = 0; run < 2; run++) {
                var eligible = repository.getAllQuestionsWithNoTopics();
                assertEquals(1, eligible.size());
                assertEquals(validId.toString(), eligible.getFirst().getId());
            }
            assertEquals(
                    before, repository.getQuestionById(missingId.toString()).orElseThrow());
        } finally {
            ds.destroy();
        }
    }
}
