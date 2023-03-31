package co.edu.icesi.drafts.service.impl;

import co.edu.icesi.drafts.dto.IcesiDocumentDTO;
import co.edu.icesi.drafts.error.exception.*;
import co.edu.icesi.drafts.mapper.IcesiDocumentMapper;
import co.edu.icesi.drafts.model.IcesiDocument;
import co.edu.icesi.drafts.model.IcesiDocumentStatus;
import co.edu.icesi.drafts.model.IcesiUser;
import co.edu.icesi.drafts.repository.IcesiDocumentRepository;
import co.edu.icesi.drafts.repository.IcesiUserRepository;
import co.edu.icesi.drafts.service.IcesiDocumentService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

import static co.edu.icesi.drafts.error.util.IcesiExceptionBuilder.createIcesiException;

@Service
@AllArgsConstructor
class IcesiDocumentServiceImpl implements IcesiDocumentService {

    private final IcesiUserRepository userRepository;
    private final IcesiDocumentRepository documentRepository;
    private final IcesiDocumentMapper documentMapper;

    @Override
    public List<IcesiDocumentDTO> getAllDocuments() {
        return documentRepository.findAll().stream()
                .map(documentMapper::fromIcesiDocument)
                .toList();
    }

    @Override
    public List<IcesiDocumentDTO> createDocuments(List<IcesiDocumentDTO> documentsDTO) {
        documentsDTO.forEach(this::validateUser);
        documentsDTO.forEach(documentDTO -> validateTitle(documentDTO.getTitle()));
        documentsDTO.forEach(this::createDocument);

        return null;
    }

    @Override
    public IcesiDocumentDTO updateDocument(String documentId, IcesiDocumentDTO icesiDocumentDTO) {

        var document = findDocument(documentId);

        validateStatus(document);

        validateUserUpdate(document, icesiDocumentDTO);

        return modifyDocument(document, icesiDocumentDTO);
    }

    private void validateUserUpdate(IcesiDocument document, IcesiDocumentDTO icesiDocumentDTO){

        if (document.getIcesiUser().getIcesiUserId() == icesiDocumentDTO.getUserId()){
            throw new RuntimeException("Can't update user");
        }
    }

    public IcesiDocumentDTO modifyDocument(IcesiDocument originalDocument, IcesiDocumentDTO newDocument){

        originalDocument.setText(newDocument.getText());

        validateTitle(newDocument.getTitle());
        originalDocument.setTitle(newDocument.getTitle());

        validateStatusForChangeStatus(originalDocument, newDocument);

        originalDocument.setStatus(newDocument.getStatus());

        return documentMapper.fromIcesiDocument(originalDocument);
    }

    private void validateStatus(IcesiDocument document){

        if(document.getStatus() != IcesiDocumentStatus.DRAFT && document.getStatus() != IcesiDocumentStatus.REVISION){
            throw new RuntimeException("Title and/or text can't be modified");
        }
    }

    private void validateStatusForChangeStatus(IcesiDocument originalDocument, IcesiDocumentDTO newDocument){
        if (originalDocument.getStatus() != newDocument.getStatus() && originalDocument.getStatus() != IcesiDocumentStatus.REVISION){
            throw new RuntimeException("Status can't be modified");
        }
    }

    @Override
    public IcesiDocumentDTO createDocument(IcesiDocumentDTO icesiDocumentDTO) {

        var user = validateUser(icesiDocumentDTO);
        var icesiDocument = documentMapper.fromIcesiDocumentDTO(icesiDocumentDTO);
        icesiDocument.setIcesiUser(user);

        validateTitle(icesiDocument.getTitle());

        return documentMapper.fromIcesiDocument(documentRepository.save(icesiDocument));
    }

    public IcesiDocument findDocument(String documentId){

        if(documentId == null){
            throw createIcesiException(
                    "Document not found",
                    HttpStatus.NOT_FOUND,
                    new DetailBuilder(ErrorCode.ERR_404, "document","title", null, null)
            ).get();
        }
        return documentRepository.findById(UUID.fromString(documentId)).orElse(null);
    }

    public IcesiUser validateUser(IcesiDocumentDTO documentDTO){

        if (documentDTO.getUserId() == null){
            throw createIcesiException(
                    "User not found",
                    HttpStatus.NOT_FOUND,
                    new DetailBuilder(ErrorCode.ERR_REQUIRED_FIELD, "userId", null)
            ).get();
        }
        return userRepository.findById(documentDTO.getUserId()).orElse(null);
    }

    public void validateTitle(String titleOfDocument){
        if (documentRepository.findByTitle(titleOfDocument).isPresent()){
            throw createIcesiException(
                    "User not found",
                    HttpStatus.NOT_FOUND,
                    new DetailBuilder(ErrorCode.ERR_DUPLICATED, "Document", "Title", titleOfDocument, null)
            ).get();
        }
    }
}
