package co.edu.icesi.drafts.service.impl;

import co.edu.icesi.drafts.controller.IcesiDocumentController;
import co.edu.icesi.drafts.dto.IcesiDocumentDTO;
import co.edu.icesi.drafts.error.exception.*;
import co.edu.icesi.drafts.mapper.IcesiDocumentMapper;
import co.edu.icesi.drafts.model.IcesiDocumentStatus;
import co.edu.icesi.drafts.repository.IcesiDocumentRepository;
import co.edu.icesi.drafts.repository.IcesiUserRepository;
import co.edu.icesi.drafts.service.IcesiDocumentService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;

import java.util.List;

import static co.edu.icesi.drafts.error.util.IcesiExceptionBuilder.createIcesiException;

@AllArgsConstructor
class IcesiDocumentServiceImpl implements IcesiDocumentService {

    private final IcesiUserRepository userRepository;
    private final IcesiDocumentRepository documentRepository;
    private final IcesiDocumentMapper documentMapper;

    private IcesiDocumentController documentController;

    public IcesiDocumentServiceImpl(IcesiUserRepository userRepository, IcesiDocumentRepository documentRepository, IcesiDocumentMapper documentMapper) {
        this.userRepository = userRepository;
        this.documentRepository = documentRepository;
        this.documentMapper = documentMapper;
    }

    @Override
    public List<IcesiDocumentDTO> getAllDocuments() {
        return documentRepository.findAll().stream()
                .map(documentMapper::fromIcesiDocument)
                .toList();
    }


    @Override
    public List<IcesiDocumentDTO> createDocuments(List<IcesiDocumentDTO> documentsDTO) {
        return null;
    }

    @Override
    public IcesiDocumentDTO updateDocument(String documentId, IcesiDocumentDTO icesiDocumentDTO) {

        var document = documentRepository.findByTitle(documentId).orElseThrow(() -> new RuntimeException("Document not found"));

        IcesiDocumentDTO documentDTO = documentMapper.fromIcesiDocument(document);

        validateUserUpdate(documentDTO, icesiDocumentDTO);

        validateStatus(documentDTO);

        return modifyDocument(documentDTO, icesiDocumentDTO);
    }

    private void validateUserUpdate(IcesiDocumentDTO document, IcesiDocumentDTO icesiDocumentDTO){
        if (document.getUserId() != icesiDocumentDTO.getUserId()){
            throw new RuntimeException("Can't update user");
        }
    }

    public IcesiDocumentDTO modifyDocument(IcesiDocumentDTO originalDocument, IcesiDocumentDTO newDocument){

        originalDocument.setText(newDocument.getText());

        verifyTitle(newDocument.getTitle());
        originalDocument.setTitle(newDocument.getTitle());

        validateStatusForChangeStatus(originalDocument, newDocument);

        originalDocument.setStatus(newDocument.getStatus());

        return originalDocument;
    }

    private void validateStatus(IcesiDocumentDTO document){
        if(document.getStatus() != IcesiDocumentStatus.DRAFT && document.getStatus() != IcesiDocumentStatus.REVISION){
            throw new RuntimeException("Title and/or text can't be modified");
        }
    }

    private void validateStatusForChangeStatus(IcesiDocumentDTO originalDocument, IcesiDocumentDTO newDocument){
        if (originalDocument.getStatus() != newDocument.getStatus() && originalDocument.getStatus() != IcesiDocumentStatus.REVISION){
            throw new RuntimeException("Status can't be modified");
        }
    }

    private void verifyTitle(String title){
        documentRepository.findByTitle(title).orElseThrow(() -> new RuntimeException("Document with this title already exists"));
    }

    @Override
    public IcesiDocumentDTO createDocument(IcesiDocumentDTO icesiDocumentDTO) {
        var user = userRepository.findById(icesiDocumentDTO.getUserId())
                .orElseThrow(
                        createIcesiException(
                                "User not found",
                                HttpStatus.NOT_FOUND,
                                new DetailBuilder(ErrorCode.ERR_404, "User", "Id", icesiDocumentDTO.getUserId())
                        )
                );
        var icesiDocument = documentMapper.fromIcesiDocumentDTO(icesiDocumentDTO);
        icesiDocument.setIcesiUser(user);
        return documentMapper.fromIcesiDocument(documentRepository.save(icesiDocument));
    }
}
