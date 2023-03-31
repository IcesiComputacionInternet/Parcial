package co.edu.icesi.drafts.mapper;

import co.edu.icesi.drafts.dto.IcesiDocumentDTO;
import co.edu.icesi.drafts.model.IcesiDocument;
import org.springframework.stereotype.Component;

public class NotFunctionalMapper implements IcesiDocumentMapper{
    @Override
    public IcesiDocumentDTO fromIcesiDocument(IcesiDocument icesiDocument) {
        return null;
    }

    @Override
    public IcesiDocument fromIcesiDocumentDTO(IcesiDocumentDTO icesiDocumentDTO) {
        return null;
    }
}
