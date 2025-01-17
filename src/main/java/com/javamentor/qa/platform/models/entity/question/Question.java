package com.javamentor.qa.platform.models.entity.question;

import com.javamentor.qa.platform.dao.search.bridge.AnswersCountBinder;
import com.javamentor.qa.platform.dao.search.bridge.TagsBinder;
import com.javamentor.qa.platform.dao.search.bridge.VotesQuestionCountBinder;
import com.javamentor.qa.platform.exception.ConstrainException;
import com.javamentor.qa.platform.models.entity.question.answer.Answer;
import com.javamentor.qa.platform.models.entity.question.answer.VoteAnswer;
import com.javamentor.qa.platform.models.entity.user.User;
import com.javamentor.qa.platform.models.entity.user.UserFavoriteQuestion;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.PropertyBinderRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.*;

import javax.persistence.*;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "question")
@Indexed
public class Question implements Serializable {

    private static final long serialVersionUID = -4612026867697897418L;
    @Id
    @GeneratedValue(generator = "Question_seq")
    @GenericField(projectable = Projectable.YES)
    private Long id;

    @Column
    @NotNull
    @FullTextField(projectable = Projectable.YES, analyzer = "russian")
    private String title;

    @NotNull
    @Column
    @Type(type = "org.hibernate.type.TextType")
    @FullTextField(projectable = Projectable.YES, analyzer = "russian")
    private String description;

    @CreationTimestamp
    @Column(name = "persist_date", updatable = false)
    @Type(type = "org.hibernate.type.LocalDateTimeType")
    @GenericField(projectable = Projectable.YES, sortable = Sortable.YES)
    private LocalDateTime persistDateTime;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    @IndexedEmbedded(includePaths = {"id", "fullName"})
    @IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
    private User user;

    @NotNull
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "question_has_tag",
            joinColumns = @JoinColumn(name = "question_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    @PropertyBinding(binder = @PropertyBinderRef(type = TagsBinder.class))
    private List<Tag> tags = new ArrayList<>();

    @Column(name = "last_redaction_date", nullable = false)
    @Type(type = "org.hibernate.type.LocalDateTimeType")
    @UpdateTimestamp
    private LocalDateTime lastUpdateDateTime;

    @Column(name = "is_deleted")
    private Boolean isDeleted;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "question", orphanRemoval = true)
    @PropertyBinding(binder = @PropertyBinderRef(type = AnswersCountBinder.class))
    private List<Answer> answers = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "question", orphanRemoval = true)
    private List<CommentQuestion> commentQuestions;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "question", orphanRemoval = true)
    private List<UserFavoriteQuestion> userFavoriteQuestions;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "question", orphanRemoval = true)
    @PropertyBinding(binder = @PropertyBinderRef(type = VotesQuestionCountBinder.class))
    private List<VoteQuestion> voteQuestions = new ArrayList<>();

    @PrePersist
    private void prePersistFunction() {
        checkConstraints();
    }

    @PreUpdate
    private void preUpdateFunction() {
        checkConstraints();
    }

    private void checkConstraints() {
        if (this.tags == null || this.tags.isEmpty()) {
            throw new ConstrainException("Экземпляр Question должен иметь в поле tags хотя бы один элемент");
        }
        if (this.isDeleted == null) {
            this.isDeleted = false;
        }
        try {
            if (this.user.getId() <= 0) {
                throw new EntityNotFoundException("User id must be > 0 on create or update.");
            }
        } catch (NullPointerException e) {
            throw new EntityNotFoundException("User id must be not null on create.");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Question)) return false;
        Question question = (Question) o;
        return Objects.equals(id, question.id) &&
                Objects.equals(title, question.title) &&
                Objects.equals(description, question.description) &&
                Objects.equals(persistDateTime, question.persistDateTime) &&
                Objects.equals(user, question.user) &&
                Objects.equals(tags, question.tags) &&
                Objects.equals(lastUpdateDateTime, question.lastUpdateDateTime) &&
                Objects.equals(isDeleted, question.isDeleted);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, description, persistDateTime, user, tags, lastUpdateDateTime, isDeleted);
    }
}
