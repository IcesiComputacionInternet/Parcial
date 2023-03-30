package co.edu.icesi.drafts.service.impl;

import co.edu.icesi.drafts.controller.IcesiDocumentController;
import co.edu.icesi.drafts.dto.IcesiDocumentDTO;
import co.edu.icesi.drafts.error.exception.*;
import co.edu.icesi.drafts.mapper.IcesiDocumentMapper;
import co.edu.icesi.drafts.model.IcesiDocument;
import co.edu.icesi.drafts.model.IcesiDocumentStatus;
import co.edu.icesi.drafts.repository.IcesiDocumentRepository;
import co.edu.icesi.drafts.repository.IcesiUserRepository;
import co.edu.icesi.drafts.service.IcesiDocumentService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.UUID;

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
        List<IcesiDocument> documents = documentsDTO.stream()
                .peek(this::validateUniqueDocumentTitle)
                .peek(icesiDocumentDTO -> validateUserExists(icesiDocumentDTO.getUserId()))
                .map(this::createDocument)
                .map(documentMapper::fromIcesiDocumentDTO)
                .toList();
        return documentRepository.saveAll(documents).stream()
                .map(documentMapper::fromIcesiDocument)
                .toList();
    }

    private void validateUniqueDocumentTitle(IcesiDocumentDTO icesiDocumentDTO){
        if(documentRepository.findByTitle(icesiDocumentDTO.getTitle()).isPresent()){
            throw new IcesiException("Document title already exists"
                    , IcesiError.builder()
                    .status(HttpStatus.BAD_REQUEST)
                    .build());
        }
    }
    private void validateUserExists(UUID userId){
        if(userRepository.findById(userId).isEmpty()){
            throw new IcesiException("User not found"
                    , IcesiError.builder()
                    .status(HttpStatus.BAD_REQUEST)
                    .build());
        }
    }

    @Override
    public IcesiDocumentDTO updateDocument(String documentId, IcesiDocumentDTO icesiDocumentDTO) {
        IcesiDocument documentWithNewInfo = getDocumentById(documentId);
        IcesiDocument documentToUpdate = documentMapper.fromIcesiDocumentDTO(icesiDocumentDTO);

        validateDocumentState(icesiDocumentDTO);

        changeDocumentArguments(documentToUpdate, documentWithNewInfo);

        documentRepository.save(documentToUpdate);
        return documentMapper.fromIcesiDocument(documentToUpdate);
    }

    private void changeDocumentArguments(IcesiDocument documentToUpdate, IcesiDocument documentWithNewInfo){
        documentToUpdate.setText(documentWithNewInfo.getText());
        documentToUpdate.setTitle(documentWithNewInfo.getTitle());
    }

    private void validateDocumentState(IcesiDocumentDTO icesiDocumentDTO){
        if(icesiDocumentDTO.getStatus().equals(IcesiDocumentStatus.APPROVED)){
            throw new IcesiException("Document is already approved"
                    , IcesiError.builder()
                    .status(HttpStatus.BAD_REQUEST)
                    .build());
        }
    }

    private IcesiDocument getDocumentById(String documentId){
        return documentRepository.findById(UUID.fromString(documentId))
                .orElseThrow(
                        createIcesiException(
                                "Document not found",
                                HttpStatus.NOT_FOUND,
                                new DetailBuilder(ErrorCode.ERR_404, "Document", "Id", documentId)
                        )
                );
    }

    @Override
    public IcesiDocumentDTO createDocument(IcesiDocumentDTO icesiDocumentDTO) {
        var user = userRepository.findById(icesiDocumentDTO.getUserId())
                .orElseThrow(
                        () -> new IcesiException("User not found", IcesiError.builder().status(HttpStatus.BAD_REQUEST).build())
                );
        var icesiDocument = documentMapper.fromIcesiDocumentDTO(icesiDocumentDTO);
        icesiDocument.setIcesiUser(user);
        return documentMapper.fromIcesiDocument(documentRepository.save(icesiDocument));
    }
}
