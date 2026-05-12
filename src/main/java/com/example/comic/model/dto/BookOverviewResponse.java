package com.example.comic.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.awt.print.Book;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookOverviewResponse {
    private String message;
    private BookOverviewDTO bookOverviewData;
}
