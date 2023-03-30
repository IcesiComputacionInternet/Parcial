package co.edu.icesi.drafts.controller;

import co.edu.icesi.drafts.api.IcesiDocumentAPI;
import co.edu.icesi.drafts.dto.IcesiDocumentDTO;
import co.edu.icesi.drafts.dto.UpdateDocumentDTO;
import co.edu.icesi.drafts.service.IcesiDocumentService;
import co.edu.icesi.drafts.service.impl.IcesiDocumentServiceImpl;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@AllArgsConstructor
public class IcesiDocumentController implements IcesiDocumentAPI {

    private final IcesiDocumentService documentService;

    @Override
    public List<IcesiDocumentDTO> getAllDocuments() {
        return documentService.getAllDocuments();
    }

    @Override
    public IcesiDocumentDTO createDocument(IcesiDocumentDTO documentDTO) {
        return documentService.createDocument(documentDTO);
    }

    @Override
    public List<IcesiDocumentDTO> createDocuments(List<IcesiDocumentDTO> documentDTOS) {
        return documentService.createDocuments(documentDTOS);
    }


    @Override
    public IcesiDocumentDTO updateDocument(UpdateDocumentDTO updateDocumentDTO) {
        return documentService.updateDocument(updateDocumentDTO);
    }
}
