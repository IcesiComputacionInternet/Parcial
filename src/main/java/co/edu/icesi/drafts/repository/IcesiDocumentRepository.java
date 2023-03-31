package co.edu.icesi.drafts.repository;

import co.edu.icesi.drafts.model.IcesiDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface IcesiDocumentRepository extends JpaRepository<IcesiDocument, UUID> {


    @Query("SELECT document FROM IcesiDocument document WHERE  document.title= :title")
    Optional<IcesiDocument> findByTitle(String title);

    @Query("SELECT document FROM IcesiDocument document WHERE  document.icesiDocumentId= :icesiDocumentId")
    Optional<IcesiDocument> findById(UUID icesiDocumentId);
}
