package co.edu.icesi.drafts.repository;

import co.edu.icesi.drafts.model.IcesiDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface IcesiDocumentRepository extends JpaRepository<IcesiDocument, UUID> {

    Optional<IcesiDocument> findByTitle(String title);

    @Query("SELECT a FROM IcesiDocument a WHERE a.icesiDocumentId = :icesiDocumentId AND a.status <> 3")
    Optional<IcesiDocument> getTypeofDocument(@Param("icesiDocumentId") String icesiDocumentId);

    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END AS titleExist FROM IcesiDocument u WHERE u.title = :title")
    Boolean existsByTitle(@Param("title")String title);
}
