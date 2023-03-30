package co.edu.icesi.drafts.mapper;

import co.edu.icesi.drafts.dto.IcesiDocumentDTO;
import co.edu.icesi.drafts.model.IcesiDocument;

import java.util.List;

import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface IcesiDocumentMapper {

    IcesiDocumentDTO fromIcesiDocument(IcesiDocument icesiDocument);

    IcesiDocument fromIcesiDocumentDTO(IcesiDocumentDTO icesiDocumentDTO);

    List<IcesiDocumentDTO> fromIcesiDocumentList(List<IcesiDocument> icesiDocuments);

}
