package com.part2.monew.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name="comments_managements")
public class CommentsManagement {
    @Id
    @UuidGenerator
    @Column(name="comment_management_id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "news_article_id" )
    private NewsArticle newsArticle;

    private String content;

    @Column(name = "like_count")
    private int likeCount;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Timestamp createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Timestamp updatedAt;

    @Column(nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "commentsManagement", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CommentLike> commentLikes;


    @Builder
    private CommentsManagement(UUID id, User user, NewsArticle newsArticle, String content, int likeCount, Timestamp createdAt) {
        this.id = id;
        this.user = user;
        this.newsArticle = newsArticle;
        this.content = content;
        this.likeCount = likeCount;
        this.createdAt = createdAt;
    }

    public static CommentsManagement create(User user, NewsArticle newsArticle, String content, int likeCount) {
        return CommentsManagement.builder()
                .user(user)
                .newsArticle(newsArticle)
                .content(content)
                .likeCount(likeCount)
                .build();
    }

    public static CommentsManagement create(User user, NewsArticle newsArticle, String content, int likeCount, Timestamp createAt) {
        CommentsManagement cm = CommentsManagement.builder()
                .user(user)
                .newsArticle(newsArticle)
                .content(content)
                .likeCount(likeCount)
                .build();
        cm.createdAt = createAt;
        return cm;
    }

    public void update(String content) {
        this.content = content;
    }

    public void updateTotalCount(int likeCount) {
        this.likeCount = likeCount;
    }

    public void delete() {
        this.active = false;
    }

}
