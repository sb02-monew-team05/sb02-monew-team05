package com.part2.monew.service.newsprovider;

import com.part2.monew.config.NewsProviderProperties.ProviderConfig;
import com.part2.monew.dto.request.NewsArticleDto;
import com.part2.monew.service.CategoryKeywordService;
import com.part2.monew.util.TextRankSummarizer;
import com.part2.monew.util.WebContentExtractor;
import com.rometools.rome.feed.synd.SyndEnclosure;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import java.net.URL;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RssFeedNewsProvider implements NewsProvider {

    private final CategoryKeywordService categoryKeywordService;

    @Override
    public String getProviderKey() {
        return "RSS Feed";
    }

    @Override
    public List<NewsArticleDto> fetchNews(ProviderConfig config, List<String> keywords) {
        log.info("RSS 피드 '{}' 수집 시작 - URL: {}, 키워드: {}", config.getName(), config.getFeedUrl(),
            keywords);

        try {
            // RSS 피드 파싱
            List<SyndEntry> allEntries = parseFeed(config.getFeedUrl());
            log.info("RSS 피드 '{}'에서 총 {}개 기사 파싱", config.getName(), allEntries.size());

            List<NewsArticleDto> filteredNews;

            if (keywords == null || keywords.isEmpty()) {
                // 키워드가 없으면 모든 기사 변환
                filteredNews = allEntries.stream().map(entry -> convertToDto(entry, config))
                    .collect(Collectors.toList());
                log.info("키워드 없음 - 모든 기사 변환: {}건", filteredNews.size());
            } else {
                // 키워드 필터링 적용
                filteredNews = allEntries.stream()
                    .filter(entry -> containsAnyKeyword(entry, keywords))
                    .map(entry -> convertToDto(entry, config)).collect(Collectors.toList());
                log.info("키워드 필터링 적용 - {}개 기사 중 {}건 선택", allEntries.size(), filteredNews.size());
            }

            log.info("RSS 피드 '{}'에서 {}건 수집 완료", config.getName(), filteredNews.size());
            return filteredNews;

        } catch (Exception e) {
            log.error("RSS 피드 '{}' 수집 실패: {}", config.getName(), e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    private List<SyndEntry> parseFeed(String feedUrl) throws Exception {
        try {
            URL url = new URL(feedUrl);
            java.net.URLConnection connection = url.openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(10000);
            
            connection.setRequestProperty("User-Agent", 
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            
            String contentType = connection.getContentType();
            if (contentType != null && !contentType.toLowerCase().contains("xml") && 
                !contentType.toLowerCase().contains("rss") && 
                !contentType.toLowerCase().contains("application/rss")) {
                log.warn("RSS 피드가 아닌 콘텐츠 타입: {} (URL: {})", contentType, feedUrl);
                throw new Exception("RSS 피드가 아닌 콘텐츠 타입: " + contentType);
            }
            
            SyndFeedInput input = new SyndFeedInput();
            input.setPreserveWireFeed(false);
            
            try {
                SyndFeed feed = input.build(new XmlReader(connection.getInputStream()));
                
                if (feed == null || feed.getEntries() == null) {
                    log.warn("RSS 피드가 비어있습니다: {}", feedUrl);
                    return new ArrayList<>();
                }
                
                return feed.getEntries();
                
            } catch (com.rometools.rome.io.FeedException e) {
                log.warn("RSS 피드 XML 구조 오류: {} - 상세: {}", feedUrl, e.getMessage());
                return new ArrayList<>();
            }
            
        } catch (java.net.SocketTimeoutException e) {
            log.warn("RSS 피드 타임아웃: {} - {}", feedUrl, e.getMessage());
            throw new Exception("RSS 피드 연결 타임아웃: " + feedUrl);
        } catch (java.net.ConnectException e) {
            log.warn("RSS 피드 연결 실패: {} - {}", feedUrl, e.getMessage());
            throw new Exception("RSS 피드 연결 실패: " + feedUrl);
        } catch (java.io.IOException e) {
            log.warn("RSS 피드 네트워크 오류: {} - {}", feedUrl, e.getMessage());
            throw new Exception("RSS 피드 네트워크 오류: " + feedUrl);
        } catch (Exception e) {
            log.error("RSS 피드 파싱 실패: {} - {}", feedUrl, e.getMessage());
            throw new Exception("RSS 피드 파싱 실패: " + feedUrl + " - " + e.getMessage());
        }
    }

    private boolean containsAnyKeyword(SyndEntry entry, List<String> keywords) {
        String title = entry.getTitle() != null ? entry.getTitle() : "";
        String description =
            entry.getDescription() != null ? entry.getDescription().getValue() : "";

        // HTML 태그 제거
        description = cleanHtmlTags(description);

        // RSS 카테고리 정보 추출
        String categories = "";
        if (entry.getCategories() != null && !entry.getCategories().isEmpty()) {
            categories = entry.getCategories().stream()
                .map(cat -> cat.getName() != null ? cat.getName() : "")
                .filter(name -> !name.isEmpty()).collect(Collectors.joining(" "));
        }

        String inferredCategory = categoryKeywordService.inferCategoryFromContent(title,
            description);
        log.debug("추론된 카테고리: {}", inferredCategory);

        boolean shouldInclude = false;
        String matchReason = "";

        // 1. 관심사 키워드가 제목/설명에 직접 포함되어 있는지 확인
        String searchText = (title + " " + description).toLowerCase();
        for (String keyword : keywords) {
            if (searchText.contains(keyword.toLowerCase())) {
                shouldInclude = true;
                matchReason = "키워드직접매칭: " + keyword;
                break;
            }
        }

        // 2. 추론된 카테고리가 관심사 목록에 있는지 확인
        if (!shouldInclude) {
            for (String keyword : keywords) {
                if (inferredCategory.equalsIgnoreCase(keyword)) {
                    shouldInclude = true;
                    matchReason = "카테고리매칭: " + inferredCategory + " = " + keyword;
                    break;
                }
            }
        }

        if (!shouldInclude && keywords.stream().anyMatch(k -> k.equalsIgnoreCase("IT"))) {
            if ("IT".equals(inferredCategory)) {
                shouldInclude = true;
                matchReason = "IT카테고리 광범위매칭";
            }
        }

        if (!shouldInclude) {
            String categoryText = categories.toLowerCase();
            for (String keyword : keywords) {
                if (categoryText.contains(keyword.toLowerCase())) {
                    shouldInclude = true;
                    matchReason = "RSS카테고리매칭: " + keyword;
                    break;
                }
            }
        }

        if (shouldInclude) {
            log.debug("포함됨");
        } else {
            log.debug("제외됨");
        }

        return shouldInclude;
    }

    private NewsArticleDto convertToDto(SyndEntry entry, ProviderConfig config) {
        return NewsArticleDto.builder().providerName(config.getName()).title(entry.getTitle())
            .originalLink(entry.getLink())
            .publishedDate(convertToTimestamp(entry.getPublishedDate()))
            .summaryOrContent(getDescription(entry))
            .guid(entry.getUri() != null ? entry.getUri() : entry.getLink())
            .thumbnailUrl(extractThumbnailUrl(entry)).build();
    }

    private Timestamp convertToTimestamp(Date date) {
        if (date == null) {
            return new Timestamp(System.currentTimeMillis());
        }
        return new Timestamp(date.getTime());
    }

    private String getDescription(SyndEntry entry) {
        // 1. 기존 RSS 요약이 있는지 확인
        if (entry.getDescription() != null && entry.getDescription().getValue() != null) {
            String cleanedContent = cleanHtmlTags(entry.getDescription().getValue());
            if (cleanedContent != null && !cleanedContent.trim().isEmpty()
                && cleanedContent.length() > 10) {
                return cleanedContent;
            }
        }

        // 요약 x 생성
        String articleUrl = entry.getLink();
        if (articleUrl != null && !articleUrl.trim().isEmpty()) {
            log.debug("RSS 요약이 없어 웹 스크래핑 시도: {}", articleUrl);

            try {
                // 웹 페이지에서 기사 본문 추출
                String fullContent = WebContentExtractor.extractContent(articleUrl);

                if (!fullContent.trim().isEmpty()) {
                    String generatedSummary = TextRankSummarizer.summarize(fullContent, 3);

                    if (!generatedSummary.trim().isEmpty()) {
                        log.debug("자동 요약 생성 완료: {} chars", generatedSummary.length());
                        return generatedSummary;
                    }
                }
            } catch (Exception e) {
                log.warn("자동 요약 생성 실패 ({}): {}", articleUrl, e.getMessage());
            }
        }

        return "기사 요약 없음";
    }

    private String extractThumbnailUrl(SyndEntry entry) {
        if (entry.getEnclosures() != null && !entry.getEnclosures().isEmpty()) {
            return entry.getEnclosures().stream().filter(
                enclosure -> enclosure.getType() != null && enclosure.getType()
                    .startsWith("image/")).map(SyndEnclosure::getUrl).findFirst().orElse(null);
        }
        return null;
    }

    private String cleanHtmlTags(String text) {
        if (text == null) {
            return null;
        }
        return text.replaceAll("<[^>]*>", "").trim();
    }
}
