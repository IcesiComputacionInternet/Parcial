package co.edu.icesi.drafts.mapper;

import co.edu.icesi.drafts.dto.IcesiDocumentDTO;
import co.edu.icesi.drafts.model.IcesiDocument;

import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class NotFunctionalMapper implements IcesiDocumentMapper{
    @Override
    public IcesiDocumentDTO fromIcesiDocument(IcesiDocument icesiDocument) {
        return null;
    }

    @Override
    public IcesiDocument fromIcesiDocumentDTO(IcesiDocumentDTO icesiDocumentDTO) {
        return null;
    }

    @Override
    public List<IcesiDocumentDTO> fromIcesiDocumentList(List<IcesiDocument> icesiDocuments) {
        throw new UnsupportedOperationException("Unimplemented method 'fromIcesiDocumentList'");
    }
}
