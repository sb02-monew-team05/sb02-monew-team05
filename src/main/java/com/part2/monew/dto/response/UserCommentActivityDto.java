package com.part2.monew.dto.response;

import com.part2.monew.entity.CommentsManagement;
import com.part2.monew.entity.NewsArticle;
import lombok.Builder;
import lombok.Getter;

import java.sql.Timestamp;
import java.util.UUID;

@Getter
@Builder
public class UserCommentActivityDto {
  private UUID id;
  private UUID articleId;
  private String articleTitle;
  private UUID userId;
  private String userNickname;
  private String content;
  private int likeCount;
  private Timestamp createdAt;

  public static UserCommentActivityDto of(CommentsManagement comment) {
    NewsArticle article = comment.getNewsArticle();
    return UserCommentActivityDto.builder()
        .id(comment.getId())
        .articleId(article.getId())
        .articleTitle(article.getTitle())
        .userId(comment.getUser().getId())
        .userNickname(comment.getUser().getNickname())
        .content(comment.getContent())
        .likeCount(comment.getLikeCount())
        .createdAt(comment.getCreatedAt())
        .build();
  }
}