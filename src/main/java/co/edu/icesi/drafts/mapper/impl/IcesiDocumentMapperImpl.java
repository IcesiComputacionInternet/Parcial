package co.edu.icesi.drafts.mapper.impl;

import co.edu.icesi.drafts.dto.IcesiDocumentDTO;
import co.edu.icesi.drafts.mapper.IcesiDocumentMapper;
import co.edu.icesi.drafts.model.IcesiDocument;
import org.springframework.stereotype.Component;

@Component
public class IcesiDocumentMapperImpl implements IcesiDocumentMapper {

    @Override
    public IcesiDocumentDTO fromIcesiDocument(IcesiDocument icesiDocument) {
        if ( icesiDocument == null ) {
            return null;
        }

        IcesiDocumentDTO.IcesiDocumentDTOBuilder icesiDocumentDTO = IcesiDocumentDTO.builder();

        icesiDocumentDTO.icesiDocumentId( icesiDocument.getIcesiDocumentId() );
        icesiDocumentDTO.title( icesiDocument.getTitle() );
        icesiDocumentDTO.text( icesiDocument.getText() );
        icesiDocumentDTO.status( icesiDocument.getStatus() );

        return icesiDocumentDTO.build();
    }

    @Override
    public IcesiDocument fromIcesiDocumentDTO(IcesiDocumentDTO icesiDocumentDTO) {
        if ( icesiDocumentDTO == null ) {
            return null;
        }

        IcesiDocument.IcesiDocumentBuilder icesiDocument = IcesiDocument.builder();

        icesiDocument.icesiDocumentId( icesiDocumentDTO.getIcesiDocumentId() );
        icesiDocument.title( icesiDocumentDTO.getTitle() );
        icesiDocument.text( icesiDocumentDTO.getText() );
        icesiDocument.status( icesiDocumentDTO.getStatus() );

        return icesiDocument.build();
    }
}