package co.edu.icesi.drafts.service.impl;

import co.edu.icesi.drafts.controller.IcesiDocumentController;
import co.edu.icesi.drafts.dto.IcesiDocumentDTO;
import co.edu.icesi.drafts.error.exception.*;
import co.edu.icesi.drafts.error.util.IcesiExceptionBuilder;
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

import java.util.*;
import java.util.stream.Collectors;

import static co.edu.icesi.drafts.error.util.IcesiExceptionBuilder.createIcesiError;
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
        List<DetailBuilder> errorDetails = new ArrayList<>();
        errorDetails.addAll (documentsDTO.stream()
                .filter(documentDTO -> userRepository.findById(documentDTO.getUserId()).isEmpty())
                .map(documentDTO -> new DetailBuilder(ErrorCode.ERR_404, "User", "Id", documentDTO.getUserId())
                ).collect(Collectors.toList()));
        errorDetails.addAll(documentsDTO.stream()
                .filter(documentDTO -> documentRepository.findByTitle(documentDTO.getTitle()).isPresent())
                .map(documentDTO -> new DetailBuilder(ErrorCode.ERR_DUPLICATED, "Document", "Title", documentDTO.getTitle())
                )
                .collect(Collectors.toList()));

        if (!errorDetails.isEmpty()) {
            throw createIcesiException(
                    "Error creating documents",
                     errorDetails.toArray(new DetailBuilder[0])
            ).get();

        }
        List<IcesiDocument> savedDocuments = documentRepository.saveAll(documentsDTO.stream()
                .map(documentMapper::fromIcesiDocumentDTO)
                .collect(Collectors.toList()));
        return documentsDTO;

    }

    @Override
    public IcesiDocumentDTO updateDocument(String documentId, IcesiDocumentDTO icesiDocumentDTO) {
        var document = documentRepository.findById(UUID.fromString(documentId))
                .orElseThrow(
                        createIcesiException(
                                "Document not found",
                                HttpStatus.NOT_FOUND,
                                new DetailBuilder(ErrorCode.ERR_404, "Document", "Id", documentId)
                        )
                );
        validateStatus(icesiDocumentDTO);
        IcesiDocumentDTO updated = exchangeDocumentInfo(documentMapper.fromIcesiDocument(document), icesiDocumentDTO);

        return documentMapper.fromIcesiDocument(documentRepository.save(documentMapper.fromIcesiDocumentDTO(updated)));
    }

    public IcesiDocumentDTO exchangeDocumentInfo(IcesiDocumentDTO old, IcesiDocumentDTO newDoc){
        old.setTitle(newDoc.getTitle());
        old.setText(newDoc.getText());
        old.setStatus(newDoc.getStatus());
        return old;
    }

    public void validateStatus(IcesiDocumentDTO icesiDocumentDTO) {

        if (icesiDocumentDTO.getStatus().equals(IcesiDocumentStatus.APPROVED)){
            throw new RuntimeException("Not updatable document");
        }
    }

    public IcesiUser validateUser(IcesiDocumentDTO icesiDocumentDTO){
        var user = userRepository.findById(icesiDocumentDTO.getUserId())
                .orElseThrow(
                        createIcesiException(
                                "User not found",
                                HttpStatus.NOT_FOUND,
                                new DetailBuilder(ErrorCode.ERR_404, "User", "Id", icesiDocumentDTO.getUserId())
                        )
                );
        return user;
    }

    public void validateTitle(IcesiDocumentDTO icesiDocumentDTO){
        documentRepository.findByTitle(icesiDocumentDTO.getTitle()).ifPresent(
                error -> {
                    throw createIcesiException(
                            "resource Document with field Title: " +  icesiDocumentDTO.getTitle()+  " already exists",
                            HttpStatus.CONFLICT,
                            new DetailBuilder(ErrorCode.ERR_DUPLICATED, "Document", "Title", icesiDocumentDTO.getTitle())
                    ).get();
                });
    }

    public void validateUserIdField(IcesiDocumentDTO icesiDocumentDTO){
        Optional.ofNullable(icesiDocumentDTO.getUserId()).orElseThrow(
                createIcesiException(
                        "userId is required" ,
                        HttpStatus.NOT_FOUND,
                        new DetailBuilder(ErrorCode.ERR_REQUIRED_FIELD, "userId")
                ));
    }

    @Override
    public IcesiDocumentDTO createDocument(IcesiDocumentDTO icesiDocumentDTO) {

       /* Optional.ofNullable(icesiDocumentDTO.getTitle()).orElseThrow(
                createIcesiException(
                        "Title is required" ,
                        HttpStatus.NOT_FOUND,
                        new DetailBuilder(ErrorCode.ERR_REQUIRED_FIELD, "Title")
                ));*/

        validateTitle(icesiDocumentDTO);
        validateUserIdField(icesiDocumentDTO);
        var user = validateUser(icesiDocumentDTO);

        var icesiDocument = documentMapper.fromIcesiDocumentDTO(icesiDocumentDTO);
        icesiDocument.setIcesiUser(user);
        return documentMapper.fromIcesiDocument(documentRepository.save(icesiDocument));
    }

}
