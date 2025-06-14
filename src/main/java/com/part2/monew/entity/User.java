package com.part2.monew.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

@Builder
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name="users")
public class User {
    @Id
    @UuidGenerator
    @Column(name = "user_id")
    private UUID id;

    private String nickname;

    @Column(nullable = false, unique = true, length = 50)
    private String email;

    @Column(nullable = false, length = 30)
    private String password;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false,updatable = false)
    private Timestamp createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Timestamp updatedAt;

    @OneToMany(mappedBy = "user")
    private List<UserSubscriber> userSubscribe;

    @OneToMany(mappedBy = "user")
    private List<CommentLike> commentLikes;

    @OneToMany(mappedBy = "user")
    private List<CommentsManagement> commentManagement;

    @OneToMany(mappedBy = "user")
    private List<ActivityDetail> activityDetail;

    @OneToMany(mappedBy = "user")
    private List<Notification> notification;

    public User(String username, String email, String password, boolean active, Timestamp createdAt) {
        this.nickname = username;
        this.email = email;
        this.password = password;
        this.active = active;
        this.createdAt = createdAt;
    }
}
