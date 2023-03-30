package co.edu.icesi.drafts.mapper;

import co.edu.icesi.drafts.dto.IcesiDocumentDTO;
import co.edu.icesi.drafts.model.IcesiDocument;
import org.mapstruct.Mapper;
import org.mapstruct.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;


@Mapper(componentModel = "spring")
@Component
public interface IcesiDocumentMapper {


    IcesiDocumentDTO fromIcesiDocument(IcesiDocument icesiDocument);

    IcesiDocument fromIcesiDocumentDTO(IcesiDocumentDTO icesiDocumentDTO);

}
