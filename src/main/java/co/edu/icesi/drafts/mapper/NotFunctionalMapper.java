package co.edu.icesi.drafts.mapper;

import co.edu.icesi.drafts.dto.IcesiDocumentDTO;
import co.edu.icesi.drafts.model.IcesiDocument;
import org.springframework.stereotype.Component;

@Component
public class NotFunctionalMapper implements IcesiDocumentMapper{
    @Override
    public IcesiDocumentDTO fromIcesiDocument(IcesiDocument icesiDocument) {
        return null;
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
