package com.example.comic.repository;

import com.example.comic.model.LibraryListType;
import com.example.comic.model.UserLibrary;
import com.example.comic.model.id.UserLibraryId;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserLibraryRepository extends JpaRepository<UserLibrary, UserLibraryId> {
    Page<UserLibrary> findByUserId(Long userId, Pageable pageable);

    Page<UserLibrary> findByUserIdAndListType(Long userId, LibraryListType listType, Pageable pageable);

    Optional<UserLibrary> findByUserIdAndComicId(Long userId, Long comicId);

    void deleteByUserIdAndComicId(Long userId, Long comicId);
}
