package co.edu.icesi.drafts.mapper;

import co.edu.icesi.drafts.dto.IcesiUserDTO;
import co.edu.icesi.drafts.model.IcesiUser;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface IcesiUserMapper {

    IcesiUser fromIcesiUserDTO(IcesiUserDTO icesiUserDTO);

    IcesiUserDTO fromIcesiUser(IcesiUser icesiUser);
}
