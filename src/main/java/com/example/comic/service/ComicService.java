package com.example.comic.service;

import com.example.comic.event.ComicDeletedEvent;
import com.example.comic.event.ComicSavedEvent;
import com.example.comic.exception.AlreadyExistsException;
import com.example.comic.exception.NotFoundException;
import com.example.comic.model.*;
import com.example.comic.model.dto.*;
import com.example.comic.repository.*;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ComicService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;
    private static final long MAX_UPLOAD_SIZE_BYTES = 5 * 1024 * 1024L;
    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");

    private final ComicRepository comicRepository;
    private final ChapterRepository chapterRepository;
    private final ChapterPageRepository chapterPageRepository;
    private final ComicRatingRepository comicRatingRepository;
    private final CategoryRepository categoryRepository;
    private final ComicCategoryRepository comicCategoryRepository;
    private final ChapterCommentRepository chapterCommentRepository;
    private final CurrentUserService currentUserService;
    private final MinioStorageService minioStorageService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final PipelineProducerService pipelineProducerService;
    private final UserLibraryRepository userLibraryRepository;
    private final ReadingHistoryRepository readingHistoryRepository;
    private final StringRedisTemplate redisTemplate;

    @Transactional
    public ComicCreateResponse createComic(ComicCreateRequest request, MultipartFile coverImage) {
        currentUserService.requireAdmin();

        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new IllegalArgumentException("Tên truyện là bắt buộc.");
        }
        if (request.getFormat() == null || request.getFormat().isBlank()) {
            throw new IllegalArgumentException("Định dạng truyện là bắt buộc.");
        }

        if (coverImage == null || coverImage.isEmpty()) {
            throw new IllegalArgumentException("Ảnh bìa là bắt buộc.");
        }

        validateUploadFile(coverImage);

        Comic comic = Comic
                .builder()
                .title(request.getTitle().trim())
                .description(trimToNull(request.getDescription()))
                .author(trimToNull(request.getAuthor()))
                .originalLanguage(trimToNull(request.getOriginalLanguage()))
                .format(request.getFormat().trim())
                .status(trimToNull(request.getStatus()))
                .build();

        Comic savedComic = comicRepository.save(comic);

        String coverUrl = minioStorageService.uploadComicCover(savedComic.getId(), coverImage);
        savedComic.setCoverImageUrl(coverUrl);
        comicRepository.save(savedComic);

        List<Long> genres = request.getGenres();
        if (genres != null && !genres.isEmpty()) {
            List<ComicCategory> comicCategories = genres.stream()
                    .filter(id -> categoryRepository.existsById(id))
                    .map(id -> ComicCategory.builder().comicId(savedComic.getId()).categoryId(id).build())
                    .toList();
            if (!comicCategories.isEmpty()) {
                comicCategoryRepository.saveAll(comicCategories);
            }
        }

        applicationEventPublisher.publishEvent(new ComicSavedEvent(savedComic));
        return ComicCreateResponse
                .builder()
                .id(savedComic.getId())
                .title(savedComic.getTitle())
                .description(savedComic.getDescription())
                .author(savedComic.getAuthor())
                .coverImageUrl(savedComic.getCoverImageUrl())
                .originalLanguage(savedComic.getOriginalLanguage())
                .format(savedComic.getFormat())
                .status(savedComic.getStatus())
                .build();
    }

    @Transactional
    public ComicCreateResponse updateComic(Long comicId, ComicCreateRequest request, MultipartFile coverImage) {
        currentUserService.requireAdmin();

        Comic comic = comicRepository.findById(comicId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bộ truyện."));

        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new IllegalArgumentException("Tên truyện là bắt buộc.");
        }
        if (request.getFormat() == null || request.getFormat().isBlank()) {
            throw new IllegalArgumentException("Định dạng truyện là bắt buộc.");
        }

        comic.setTitle(request.getTitle().trim());
        comic.setDescription(trimToNull(request.getDescription()));
        comic.setAuthor(trimToNull(request.getAuthor()));
        comic.setOriginalLanguage(trimToNull(request.getOriginalLanguage()));
        comic.setFormat(request.getFormat().trim());
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            comic.setStatus(request.getStatus().trim());
        }

        if (coverImage != null && !coverImage.isEmpty()) {
            validateUploadFile(coverImage);
            if (comic.getCoverImageUrl() != null && !comic.getCoverImageUrl().isBlank()) {
                try {
                    minioStorageService.deleteObject(comic.getCoverImageUrl());
                } catch (Exception ex) {
                }
            }
            String coverUrl = minioStorageService.uploadComicCover(comicId, coverImage);
            comic.setCoverImageUrl(coverUrl);
        }

        Comic savedComic = comicRepository.save(comic);

        // Update genres
        if (request.getGenres() != null) {
            comicCategoryRepository.deleteByComicId(comicId);
            List<Long> genres = request.getGenres();
            if (!genres.isEmpty()) {
                List<ComicCategory> comicCategories = genres.stream()
                        .filter(id -> categoryRepository.existsById(id))
                        .map(id -> ComicCategory.builder().comicId(comicId).categoryId(id).build())
                        .toList();
                if (!comicCategories.isEmpty()) {
                    comicCategoryRepository.saveAll(comicCategories);
                }
            }
        }

        applicationEventPublisher.publishEvent(new ComicSavedEvent(savedComic));

        return ComicCreateResponse
                .builder()
                .id(savedComic.getId())
                .title(savedComic.getTitle())
                .description(savedComic.getDescription())
                .author(savedComic.getAuthor())
                .coverImageUrl(savedComic.getCoverImageUrl())
                .originalLanguage(savedComic.getOriginalLanguage())
                .format(savedComic.getFormat())
                .status(savedComic.getStatus())
                .build();
    }

    @Transactional
    public ChapterCreateResponse createChapter(Long comicId, ChapterCreateRequest request) {
        currentUserService.requireAdmin();

        comicRepository.findById(comicId).orElseThrow(() -> new NotFoundException("Không tìm thấy bộ truyện."));

        if (chapterRepository.existsByComicIdAndChapterNumber(comicId, request.getChapterNumber())) {
            throw new AlreadyExistsException("Số chương đã tồn tại trong bộ truyện này.");
        }

        Chapter chapter = Chapter
                .builder()
                .comicId(comicId)
                .chapterNumber(request.getChapterNumber())
                .title(trimToNull(request.getTitle()))
                .build();
        Chapter savedChapter = chapterRepository.save(chapter);

        return ChapterCreateResponse
                .builder()
                .id(savedChapter.getId())
                .comicId(savedChapter.getComicId())
                .chapterNumber(savedChapter.getChapterNumber())
                .title(savedChapter.getTitle())
                .build();
    }

    @Transactional(readOnly = true)
    public PageDataResponse<ComicSummaryResponse> getComics(
            String keyword,
            Long categoryId,
            String originalLanguage,
            String comicStatus,
            int page,
            int size) {
        Pageable pageable = PageRequest.of(normalizePage(page), normalizeSize(size));
        Page<Comic> comics = comicRepository.search(
                normalizeFilter(keyword),
                categoryId,
                normalizeFilter(originalLanguage),
                normalizeFilter(comicStatus),
                pageable);
        List<ComicSummaryResponse> content = comics
                .getContent()
                .stream()
                .map(c -> ComicSummaryResponse
                        .builder()
                        .id(c.getId())
                        .title(c.getTitle())
                        .author(c.getAuthor())
                        .coverImageUrl(c.getCoverImageUrl())
                        .originalLanguage(c.getOriginalLanguage())
                        .status(c.getStatus())
                        .format(c.getFormat())
                        .averageRating(c.getAverageRating() == null ? 0D : c.getAverageRating())
                        .build())
                .toList();

        return PageDataResponse
                .<ComicSummaryResponse>builder()
                .content(content)
                .pageNo(comics.getNumber())
                .pageSize(comics.getSize())
                .totalElements(comics.getTotalElements())
                .totalPages(comics.getTotalPages())
                .last(comics.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public PageDataResponse<ComicByGenreResponse> getComicsByGenre(int page, int size) {
        Pageable pageable = PageRequest.of(normalizePage(page), normalizeSize(size));
        Page<Category> categories = categoryRepository.findAll(pageable);

        List<ComicByGenreResponse> content = categories.getContent().stream()
                .map(category -> {
                    List<Object[]> comicData = comicCategoryRepository.findComicDataByCategoryId(category.getId());
                    List<ComicGenreBookResponse> books = comicData.stream()
                            .map(row -> ComicGenreBookResponse.builder()
                                    .id(((Number) row[0]).longValue())
                                    .title((String) row[1])
                                    .author((String) row[2])
                                    .coverImageUrl((String) row[3])
                                    .averageRating(row[4] == null ? 0D : ((Number) row[4]).doubleValue())
                                    .build())
                            .toList();

                    return ComicByGenreResponse.builder()
                            .genre(category.getName())
                            .books(books)
                            .build();
                })
                .toList();

        return PageDataResponse
                .<ComicByGenreResponse>builder()
                .content(content)
                .pageNo(categories.getNumber())
                .pageSize(categories.getSize())
                .totalElements(categories.getTotalElements())
                .totalPages(categories.getTotalPages())
                .last(categories.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public ComicDetailResponse getComicDetail(Long comicId) {
        Comic comic = comicRepository.findById(comicId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bộ truyện."));

        List<String> categoryNames = comicCategoryRepository.findCategoryNamesByComicId(comicId);

        return ComicDetailResponse.builder()
                .id(comic.getId())
                .title(comic.getTitle())
                .description(comic.getDescription())
                .author(comic.getAuthor())
                .coverImageUrl(comic.getCoverImageUrl())
                .originalLanguage(comic.getOriginalLanguage())
                .format(comic.getFormat())
                .status(comic.getStatus())
                .averageRating(comic.getAverageRating() == null ? 0D : comic.getAverageRating())
                .totalRatings(comic.getTotalRatings() == null ? 0 : comic.getTotalRatings())
                .createdAt(comic.getCreatedAt())
                .updatedAt(comic.getUpdatedAt())
                .categories(categoryNames)
                .build();
    }

    @Transactional(readOnly = true)
    public PageDataResponse<ChapterSummaryResponse> getChapters(Long comicId, int page, int size) {
        comicRepository.findById(comicId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bộ truyện."));

        Pageable pageable = PageRequest.of(normalizePage(page), normalizeSize(size));
        Page<Chapter> chapters = chapterRepository.findByComicIdOrderByChapterNumberAsc(comicId, pageable);

        List<ChapterSummaryResponse> content = chapters.getContent().stream()
                .map(ch -> ChapterSummaryResponse.builder()
                        .id(ch.getId())
                        .chapterNumber(ch.getChapterNumber())
                        .title(ch.getTitle())
                        .createdAt(ch.getCreatedAt())
                        .totalPages((int) chapterPageRepository.countByChapterId(ch.getId()))
                        .build())
                .toList();

        return PageDataResponse.<ChapterSummaryResponse>builder()
                .content(content)
                .pageNo(chapters.getNumber())
                .pageSize(chapters.getSize())
                .totalElements(chapters.getTotalElements())
                .totalPages(chapters.getTotalPages())
                .last(chapters.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public List<ChapterPageResponse> getChapterPages(Long chapterId) {
        Chapter chapter = chapterRepository
                .findById(chapterId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy chương truyện."));

        List<ChapterPage> pages = chapterPageRepository.findByChapterIdOrderByPageNumberAsc(chapter.getId());

        return pages
                .stream()
                .map(this::toPageResponse)
                .toList();
    }

    @Transactional
    public List<ChapterPageResponse> uploadChapterPages(Long chapterId, int startPageNumber,
            List<MultipartFile> files) {
        return uploadChapterPages(chapterId, startPageNumber, files, List.of());
    }

    @Transactional
    public List<ChapterPageResponse> uploadChapterPages(Long chapterId, int startPageNumber,
            List<MultipartFile> files, List<String> targetLangs) {
        currentUserService.requireAdmin();

        Chapter chapter = chapterRepository
                .findById(chapterId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy chương truyện."));

        Comic comic = comicRepository
                .findById(chapter.getComicId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bộ truyện."));

        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn ít nhất một ảnh để tải lên.");
        }

        validateUploadFiles(files);

        int pageNumber = Math.max(1, startPageNumber);
        int endPageNumber = pageNumber + files.size() - 1;
        if (chapterPageRepository.existsByChapterIdAndPageNumberBetween(chapterId, pageNumber, endPageNumber)) {
            throw new AlreadyExistsException(
                    "Khoảng số trang bị trùng với dữ liệu đã có. Vui lòng đổi startPageNumber.");
        }

        List<ChapterPage> pagesToSave = new java.util.ArrayList<>(files.size());
        List<String> rawObjectNames = new java.util.ArrayList<>(files.size());

        for (MultipartFile file : files) {
            String objectName = minioStorageService.uploadComicPage(chapterId, pageNumber, file);
            rawObjectNames.add(objectName);

            ChapterPage page = ChapterPage
                    .builder()
                    .chapterId(chapterId)
                    .pageNumber(pageNumber)
                    .imageUrl(minioStorageService.resolvePublicUrl(objectName))
                    .status(ProcessStatus.PENDING)
                    .build();
            pagesToSave.add(page);
            pageNumber++;
        }

        List<ChapterPage> savedPages = chapterPageRepository.saveAll(pagesToSave);

        for (int i = 0; i < savedPages.size(); i++) {
            ChapterPage page = savedPages.get(i);
            PipelineJobRequest jobRequest = PipelineJobRequest.builder()
                    .job_id("chapter_" + chapterId + "_page_" + page.getPageNumber())
                    .chapter_id(String.valueOf(chapterId))
                    .page_id(String.valueOf(page.getId()))
                    .image_url(minioStorageService.resolveInternalUrl(rawObjectNames.get(i)))
                    .source_lang(comic.getOriginalLanguage() != null ? comic.getOriginalLanguage() : "ja")
                    .target_langs(targetLangs)
                    .comic_type(comic.getFormat() != null ? comic.getFormat().toLowerCase() : "manga")
                    .skip_translate(false)
                    .webhook_url("http://comic-backend:8080/comic/api/internal/webhook/processing-result")
                    .build();
            System.out.println(page.getImageUrl());
            pipelineProducerService.sendToPipeline(jobRequest);
        }

        return savedPages.stream().map(this::toPageResponse).toList();
    }

    @Transactional
    public void deleteChapterPage(Long pageId) {
        currentUserService.requireAdmin();

        ChapterPage page = chapterPageRepository
                .findById(pageId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy trang truyện."));

        minioStorageService.deleteObject(page.getImageUrl());
        minioStorageService.deleteObject(page.getCleanedImageUrl());
        chapterPageRepository.delete(page);
    }

    @Transactional
    public void deleteChapterPages(Long chapterId) {
        currentUserService.requireAdmin();

        chapterRepository
                .findById(chapterId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy chương truyện."));

        List<ChapterPage> pages = chapterPageRepository.findByChapterIdOrderByPageNumberAsc(chapterId);
        for (ChapterPage page : pages) {
            minioStorageService.deleteObject(page.getImageUrl());
            minioStorageService.deleteObject(page.getCleanedImageUrl());
        }
        chapterPageRepository.deleteByChapterId(chapterId);
    }

    @Transactional
    public ComicRatingResponse rateComic(Long comicId, int score) {
        User current = currentUserService.requireUser();
        Comic comic = comicRepository
                .findById(comicId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bộ truyện."));

        ComicRating rating = comicRatingRepository
                .findByUserIdAndComicId(current.getId(), comicId)
                .orElse(
                        ComicRating.builder().userId(current.getId()).comicId(comicId).build());
        rating.setScore(score);
        comicRatingRepository.save(rating);

        Double avg = comicRatingRepository.avgScoreByComicId(comicId);
        long total = comicRatingRepository.countByComicId(comicId);

        comic.setAverageRating(avg);
        comic.setTotalRatings((int) total);
        comicRepository.save(comic);

        return ComicRatingResponse.builder().newAverageRating(avg).totalRatings(total).build();
    }

    private int normalizePage(int page) {
        return Math.max(DEFAULT_PAGE, page);
    }

    private int normalizeSize(int size) {
        if (size <= 0) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }

    private String normalizeFilter(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void validateUploadFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Tệp ảnh không hợp lệ hoặc đang rỗng.");
        }

        if (file.getSize() > MAX_UPLOAD_SIZE_BYTES) {
            throw new IllegalArgumentException("Ảnh vượt quá dung lượng cho phép 5MB.");
        }

        String fileName = file.getOriginalFilename();
        String extension = extractExtension(fileName);
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        boolean validContentType = contentType.startsWith("image/");
        boolean validExtension = extension != null
                && ALLOWED_IMAGE_EXTENSIONS.contains(extension.toLowerCase(Locale.ROOT));

        if (!validContentType || !validExtension) {
            throw new IllegalArgumentException("Chỉ chấp nhận các định dạng hình ảnh: JPG, PNG, WEBP.");
        }
    }

    private void validateUploadFiles(List<MultipartFile> files) {
        for (MultipartFile file : files) {
            validateUploadFile(file);
        }
    }

    private String extractExtension(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return null;
        }
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return null;
        }
        return fileName.substring(dot + 1);
    }

    private ChapterPageResponse toPageResponse(ChapterPage page) {
        return ChapterPageResponse
                .builder()
                .id(page.getId())
                .pageNumber(page.getPageNumber())
                .imageUrl(minioStorageService.resolvePublicUrl(page.getImageUrl()))
                .cleanedImageUrl(minioStorageService.resolvePublicUrl(page.getCleanedImageUrl()))
                .originalMetadataUrl(minioStorageService.resolvePublicUrl(page.getOriginalMetadataUrl()))
                .build();
    }

    @Transactional
    public void deleteChapter(Long chapterId) {
        currentUserService.requireAdmin();

        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy chương truyện."));

        List<ChapterPage> pages = chapterPageRepository.findByChapterIdOrderByPageNumberAsc(chapterId);
        for (ChapterPage page : pages) {
            if (page.getImageUrl() != null) {
                minioStorageService.deleteObject(page.getImageUrl());
            }
            if (page.getCleanedImageUrl() != null) {
                minioStorageService.deleteObject(page.getCleanedImageUrl());
            }
        }
        chapterPageRepository.deleteByChapterId(chapterId);
        chapterCommentRepository.deleteByChapterId(chapterId);
        chapterRepository.delete(chapter);
    }

    @Transactional
    public void deleteComic(Long comicId) {
        currentUserService.requireAdmin();

        Comic comic = comicRepository.findById(comicId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bộ truyện."));

        if (comic.getCoverImageUrl() != null && !comic.getCoverImageUrl().isBlank()) {
            try {
                minioStorageService.deleteObject(comic.getCoverImageUrl());
            } catch (Exception ex) {
            }
        }

        comicCategoryRepository.deleteByComicId(comicId);
        comicRatingRepository.deleteByComicId(comicId);
        
        List<Chapter> chapters = chapterRepository.findByComicIdOrderByChapterNumberAsc(comicId);
        for (Chapter chapter : chapters) {
            List<ChapterPage> pages = chapterPageRepository.findByChapterIdOrderByPageNumberAsc(chapter.getId());
            for (ChapterPage page : pages) {
                if (page.getImageUrl() != null) {
                    minioStorageService.deleteObject(page.getImageUrl());
                }
                if (page.getCleanedImageUrl() != null) {
                    minioStorageService.deleteObject(page.getCleanedImageUrl());
                }
            }
            chapterPageRepository.deleteAll(pages);
        }
        readingHistoryRepository.deleteByComicId(comicId);
        userLibraryRepository.deleteByComicId(comicId);
        chapterRepository.deleteAll(chapters);
        
        comicRepository.delete(comic);
        applicationEventPublisher.publishEvent(new ComicDeletedEvent(comicId));
    }

    @Transactional(readOnly = true)
    public BookOverviewDTO getBookOverview(Long comicId) {
        Comic comic = comicRepository.findById(comicId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bộ truyện."));
        List<Chapter> chapterList = chapterRepository.findByComicIdOrderByChapterNumberAsc(comicId);

        String libraryType = null;
        User user = currentUserService.getCurrentUser();
        if (user != null) {
            Optional<UserLibrary> userLibrary = userLibraryRepository.findByUserIdAndComicId(user.getId(), comicId);
            if (userLibrary.isPresent()) {
                libraryType = userLibrary.get().getListType().name();
            }
        }

        return BookOverviewDTO.builder()
                .id(comicId)
                .title(comic.getTitle())
                .coverImageUrl(comic.getCoverImageUrl())
                .author(comic.getAuthor())
                .description(comic.getDescription())
                .averageRating(comic.getAverageRating())
                .totalRatings(comic.getTotalRatings())
                .libraryType(libraryType)
                .chapters(chapterList.stream()
                        .map(chapter -> ChapterSummaryResponse.builder()
                                .title(chapter.getTitle())
                                .id(chapter.getId())
                                .chapterNumber(chapter.getChapterNumber())
                                .createdAt(chapter.getCreatedAt())
                                .totalPages((int) chapterPageRepository.countByChapterId(chapter.getId()))
                                .build()
                        ).collect(Collectors.toList()))
                .build();
    }

    public ChapterOverviewResponse getChapterOverview(Long comicId, Integer chapterNumber) {
        Comic comic = comicRepository.findById(comicId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bộ truyện."));
        Chapter chapter = chapterRepository.findByComicIdAndChapterNumber(comicId, chapterNumber);
        return ChapterOverviewResponse.builder()
                .id(chapter.getId())
                .title(chapter.getTitle())
                .chapterNumber(chapterNumber)
                .build();
    }

    public List<Category> getGenreList() {
        return categoryRepository.findAll();
    }

    @Transactional(readOnly = true)
    public int reindexAllComics() {
        currentUserService.requireAdmin();
        List<Comic> allComics = comicRepository.findAll();
        for (Comic comic : allComics) {
            applicationEventPublisher.publishEvent(new ComicSavedEvent(comic));
        }
        return allComics.size();
    }
    public List<ComicOverviewDTO> getTrendingToday() {

        Set<String> comicIds =
                redisTemplate.opsForZSet()
                        .reverseRange(
                                "comic:trending:" + LocalDate.now().toString(),
                                0,
                                9
                        );

        if (comicIds == null || comicIds.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, Comic> comicMap =
                comicRepository.findAllById(
                                comicIds.stream()
                                        .map(Long::parseLong)
                                        .toList()
                        )
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        Comic::getId,
                                        Function.identity()
                                )
                        );

        return comicIds.stream()
                .map(Long::parseLong)
                .map(comicMap::get)
                .filter(Objects::nonNull)
                .map(comic -> {

                    String cachedViewStr =
                            redisTemplate.opsForValue()
                                    .get("comic:view:" + comic.getId());

                    int cachedView =
                            cachedViewStr == null
                                    ? 0
                                    : Integer.parseInt(cachedViewStr);

                    return ComicOverviewDTO.builder()
                            .id(comic.getId())
                            .title(comic.getTitle())
                            .coverImageUrl(comic.getCoverImageUrl())
                            .author(comic.getAuthor())
                            .description(comic.getDescription())
                            .averageRating(comic.getAverageRating())
                            .totalRatings(comic.getTotalRatings())
                            .views(comic.getViews() + cachedView)
                            .build();
                })
                .toList();
    }
}
