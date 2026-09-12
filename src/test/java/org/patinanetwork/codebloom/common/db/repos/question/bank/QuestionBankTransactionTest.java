package org.patinanetwork.codebloom.common.db.repos.question.bank;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.patinanetwork.codebloom.common.db.models.question.QuestionDifficulty;
import org.patinanetwork.codebloom.common.db.models.question.bank.QuestionBank;
import org.patinanetwork.codebloom.common.db.models.question.topic.LeetcodeTopicEnum;
import org.patinanetwork.codebloom.common.db.models.question.topic.QuestionTopic;
import org.patinanetwork.codebloom.common.db.repos.question.questionbank.QuestionBankRepository;
import org.patinanetwork.codebloom.common.db.repos.question.questionbank.QuestionBankSqlRepository;
import org.patinanetwork.codebloom.common.db.repos.question.topic.QuestionTopicRepository;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

class QuestionBankTransactionTest {
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void commitsTogetherOrRollsBackOnTopicFailure(boolean topicFails) throws Exception {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getAutoCommit()).thenReturn(true);
        JdbcClient jdbc = mock(JdbcClient.class);
        JdbcClient.StatementSpec statement = mock(JdbcClient.StatementSpec.class, RETURNS_SELF);
        when(jdbc.sql(anyString())).thenReturn(statement);
        when(statement.update()).thenReturn(1);
        QuestionTopicRepository topics = mock(QuestionTopicRepository.class);
        if (topicFails) {
            doNothing()
                    .doThrow(new IllegalStateException("Topic save failed"))
                    .when(topics)
                    .createQuestionTopic(any());
        }
        var target = new QuestionBankSqlRepository(jdbc, topics);
        var factory = new ProxyFactory(target);
        factory.addAdvice(new TransactionInterceptor(
                new DataSourceTransactionManager(dataSource), new AnnotationTransactionAttributeSource()));
        var repository = (QuestionBankRepository) factory.getProxy();
        var topic = QuestionTopic.builder()
                .topicSlug("array")
                .topic(LeetcodeTopicEnum.fromValue("array"))
                .build();
        var question = QuestionBank.builder()
                .questionSlug("example")
                .questionDifficulty(QuestionDifficulty.Easy)
                .topics(List.of(topic, topic))
                .isPaidOnly(true)
                .build();

        if (topicFails) {
            assertThrows(IllegalStateException.class, () -> repository.createQuestionWithTopics(question));
            verify(connection).rollback();
            verify(connection, never()).commit();
        } else {
            repository.createQuestionWithTopics(question);
            verify(connection).commit();
            verify(connection, never()).rollback();
        }
        verify(statement).update();
        verify(topics, times(2))
                .createQuestionTopic(
                        argThat(saved -> saved.getQuestionBankId().orElseThrow().equals(question.getId())));
    }
}
