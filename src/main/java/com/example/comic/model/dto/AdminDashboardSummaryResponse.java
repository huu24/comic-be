package com.example.comic.model.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardSummaryResponse {

    private long totalUsers;
    private long activeUsers;
    private long lockedUsers;
    private long totalComics;
    private long totalChapters;
    private long totalPages;
    private long totalRatings;
    private long totalReadingHistories;
    private List<AdminTopComicResponse> topComics;
}
