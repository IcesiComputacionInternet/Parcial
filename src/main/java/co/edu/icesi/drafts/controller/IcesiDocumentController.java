package co.edu.icesi.drafts.controller;

import co.edu.icesi.drafts.api.IcesiDocumentAPI;
import co.edu.icesi.drafts.dto.IcesiDocumentDTO;
import co.edu.icesi.drafts.service.IcesiDocumentService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class IcesiDocumentController implements IcesiDocumentAPI {

    private final IcesiDocumentService icesiDocumentService;

    public IcesiDocumentController(@Qualifier("icesiDocumentServiceImpl") IcesiDocumentService icesiDocumentService) {
        this.icesiDocumentService = icesiDocumentService;
    }

    @Override
    public List<IcesiDocumentDTO> getAllDocuments() {
        return icesiDocumentService.getAllDocuments();
    }

    @Override
    public IcesiDocumentDTO createDocument(IcesiDocumentDTO documentDTO) {
        return icesiDocumentService.createDocument(documentDTO);
    }

    @Override
    public List<IcesiDocumentDTO> createDocuments(List<IcesiDocumentDTO> documentDTOS) {
        return icesiDocumentService.createDocuments(documentDTOS);
    }

    @Override
    public IcesiDocumentDTO updateDocument(String documentId, IcesiDocumentDTO documentDTO) {
        return icesiDocumentService.updateDocument(documentId, documentDTO);
    }
}
