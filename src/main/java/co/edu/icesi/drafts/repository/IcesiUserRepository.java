package co.edu.icesi.drafts.repository;

import co.edu.icesi.drafts.model.IcesiUser;
import org.hibernate.type.StringNVarcharType;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface IcesiUserRepository extends JpaRepository<IcesiUser, UUID> {

    Optional<IcesiUser> findByEmail(String email);
    Optional<IcesiUser> findByPhoneNumber(String phoneNumber);


}
