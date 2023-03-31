package co.edu.icesi.drafts.repository;

import co.edu.icesi.drafts.model.IcesiDocument;
import co.edu.icesi.drafts.model.IcesiUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface IcesiDocumentRepository extends JpaRepository<IcesiDocument, UUID> {

    @Query(value = "UPDATE icesi_document d SET d.title=?2, d.text=?3 WHERE d.icesi_document_id=?1")
    Optional<IcesiDocument> updateById(UUID icesiDocumentId, String title, String text);

    Optional<IcesiDocument> findByTitle(String title);
}
