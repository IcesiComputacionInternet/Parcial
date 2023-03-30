package co.edu.icesi.drafts.repository;

import co.edu.icesi.drafts.model.IcesiDocument;
import co.edu.icesi.drafts.model.IcesiDocumentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface IcesiDocumentRepository extends JpaRepository<IcesiDocument, UUID> {

    Optional<IcesiDocument> findByTitle(String title);

    @Modifying
    @Query("UPDATE IcesiDocument idoc SET idoc.icesiDocumentId = :documentId, idoc.title = :title, idoc.text = :text, idoc.status = :status")
    int updateDocument(String documentId, String title, String text, IcesiDocumentStatus status);
}
