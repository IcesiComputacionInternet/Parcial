package co.edu.icesi.drafts.repository;

import co.edu.icesi.drafts.model.IcesiDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.Optional;
import java.util.UUID;

@NoRepositoryBean
public interface IcesiDocumentRepository extends JpaRepository<IcesiDocument, UUID> {

    Optional<IcesiDocument> findByTitle(String title);
    Optional<IcesiDocument> findById(String documentId);

}
