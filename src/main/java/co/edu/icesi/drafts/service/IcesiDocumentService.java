package co.edu.icesi.drafts.service;

import co.edu.icesi.drafts.dto.IcesiDocumentDTO;
import co.edu.icesi.drafts.dto.UpdateDocumentDTO;
import org.springframework.stereotype.Service;

import java.util.List;


public interface IcesiDocumentService {
    List<IcesiDocumentDTO> getAllDocuments();

    List<IcesiDocumentDTO> createDocuments(List<IcesiDocumentDTO> documentsDTO);

    IcesiDocumentDTO updateDocument(UpdateDocumentDTO updateDocumentDTO);

    IcesiDocumentDTO createDocument(IcesiDocumentDTO icesiDocumentDTO);
}
