package co.edu.icesi.drafts.mapper;

import co.edu.icesi.drafts.dto.IcesiDocumentDTO;
import co.edu.icesi.drafts.model.IcesiDocument;
import org.mapstruct.Mapper;
import org.springframework.context.annotation.Primary;

@Mapper(componentModel = "spring")
public interface IcesiDocumentMapper {

    IcesiDocumentDTO fromIcesiDocument(IcesiDocument icesiDocument);

    IcesiDocument fromIcesiDocumentDTO(IcesiDocumentDTO icesiDocumentDTO);

}
