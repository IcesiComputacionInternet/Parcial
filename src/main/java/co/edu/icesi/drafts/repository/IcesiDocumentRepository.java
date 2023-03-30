package co.edu.icesi.drafts.repository;

import co.edu.icesi.drafts.model.IcesiDocument;
import co.edu.icesi.drafts.model.IcesiDocumentStatus;
import co.edu.icesi.drafts.model.IcesiUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.Optional;
import java.util.UUID;

@NoRepositoryBean
public interface IcesiDocumentRepository extends JpaRepository<IcesiDocument, UUID> {

    Optional<IcesiDocument> findByTitle(String title);

    @Modifying
    @Query("UPDATE IcesiDocument id SET id.icesiDocumentId = :documentId, id.title = :title, id.text = :text, id.status = :status")
    IcesiDocument updateDocument(String documentId, String title, String text, IcesiDocumentStatus status);
}
