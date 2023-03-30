package co.edu.icesi.drafts.mapper;

import co.edu.icesi.drafts.dto.IcesiDocumentDTO;
import co.edu.icesi.drafts.dto.IcesiUserDTO;
import co.edu.icesi.drafts.model.IcesiDocument;
import co.edu.icesi.drafts.model.IcesiUser;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface IcesiUserMapper {

    IcesiUserDTO fromIcesiUser(IcesiUser icesiUser);

    IcesiUser fromIcesiUserDTO(IcesiUserDTO icesiUserDTO);
}
