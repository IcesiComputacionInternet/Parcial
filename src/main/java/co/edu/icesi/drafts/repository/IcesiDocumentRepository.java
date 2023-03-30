package co.edu.icesi.drafts.repository;

import co.edu.icesi.drafts.model.IcesiDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.Optional;
import java.util.UUID;


public interface IcesiDocumentRepository extends JpaRepository<IcesiDocument, UUID> {
    @Query("select d from IcesiDocument d where d.title = ?1")
    Optional<IcesiDocument> findByTitle(String title);

    @Query("select d from IcesiDocument d where d.id = ?1")
    Optional<IcesiDocument> findById(String id);

}
