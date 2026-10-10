package org.patinanetwork.codebloom.common.db.models.question.topic;

import java.time.LocalDateTime;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@AllArgsConstructor
@Builder
@EqualsAndHashCode
@ToString
public class QuestionTopic {

    private String id;

    private Optional<String> questionId;

    private Optional<String> questionBankId;

    private String topicSlug;

    private LeetcodeTopicEnum topic;

    private LocalDateTime createdAt;

    public static class QuestionTopicBuilder {
        public QuestionTopicBuilder questionId(String questionId) {
            this.questionId = Optional.ofNullable(questionId);
            return this;
        }

        public QuestionTopicBuilder questionBankId(String questionBankId) {
            this.questionBankId = Optional.ofNullable(questionBankId);
            return this;
        }

        public QuestionTopic build() {
            if (this.questionId == null) {
                this.questionId = Optional.empty();
            }
            if (this.questionBankId == null) {
                this.questionBankId = Optional.empty();
            }
            return new QuestionTopic(id, questionId, questionBankId, topicSlug, topic, createdAt);
        }
    }
}
