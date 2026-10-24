package com.trustvault.repository;

import com.trustvault.entity.AppUser;
import com.trustvault.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {

    List<Document> findByOwnerOrderByCreatedAtDesc(AppUser owner);

    Optional<Document> findByIdAndOwner(UUID id, AppUser owner);

    long countByOwner(AppUser owner);

    @Query("SELECT p.document FROM DocumentPermission p WHERE p.user = :user ORDER BY p.createdAt DESC")
    List<Document> findDocumentsSharedWithUser(@Param("user") AppUser user);

    @Query("SELECT COUNT(p) FROM DocumentPermission p WHERE p.user = :user")
    long countDocumentsSharedWithUser(@Param("user") AppUser user);

    @Query("""
        SELECT d FROM Document d
        WHERE (d.owner = :user OR d.id IN (SELECT p.document.id FROM DocumentPermission p WHERE p.user = :user))
          AND (:search IS NULL OR LOWER(d.title) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(d.originalFilename) LIKE LOWER(CONCAT('%', :search, '%')))
          AND (:categoryId IS NULL OR d.category.id = :categoryId)
          AND (:fromDate IS NULL OR d.createdAt >= :fromDate)
          AND (:toDate IS NULL OR d.createdAt <= :toDate)
        ORDER BY d.createdAt DESC
    """)
    List<Document> searchAccessibleDocuments(
            @Param("user") AppUser user,
            @Param("search") String search,
            @Param("categoryId") Long categoryId,
            @Param("fromDate") Instant fromDate,
            @Param("toDate") Instant toDate
    );

    @Query("""
        SELECT d FROM Document d
        WHERE (:search IS NULL OR LOWER(d.title) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(d.originalFilename) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(d.owner.username) LIKE LOWER(CONCAT('%', :search, '%')))
          AND (:categoryId IS NULL OR d.category.id = :categoryId)
          AND (:fromDate IS NULL OR d.createdAt >= :fromDate)
          AND (:toDate IS NULL OR d.createdAt <= :toDate)
        ORDER BY d.createdAt DESC
    """)
    List<Document> searchAllDocumentsAdmin(
            @Param("search") String search,
            @Param("categoryId") Long categoryId,
            @Param("fromDate") Instant fromDate,
            @Param("toDate") Instant toDate
    );
}
