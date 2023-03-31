package co.edu.icesi.drafts.service;

import co.edu.icesi.drafts.dto.IcesiDocumentDTO;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.List;

public interface IcesiDocumentService {
    List<IcesiDocumentDTO> getAllDocuments();

    List<IcesiDocumentDTO> createDocuments(List<IcesiDocumentDTO> documentsDTO);

    IcesiDocumentDTO updateDocument(String documentId, IcesiDocumentDTO icesiDocumentDTO);

    IcesiDocumentDTO createDocument(IcesiDocumentDTO icesiDocumentDTO);
}
