package com.trustvault.repository;

import com.trustvault.entity.AppUser;
import com.trustvault.entity.Document;
import com.trustvault.entity.DocumentPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentPermissionRepository extends JpaRepository<DocumentPermission, Long> {

    List<DocumentPermission> findByDocumentOrderByCreatedAtDesc(Document document);

    List<DocumentPermission> findByUserOrderByCreatedAtDesc(AppUser user);

    Optional<DocumentPermission> findByDocumentAndUser(Document document, AppUser user);

    void deleteByDocumentAndUser(Document document, AppUser user);

    void deleteByDocument(Document document);

    long countByDocument(Document document);
}
