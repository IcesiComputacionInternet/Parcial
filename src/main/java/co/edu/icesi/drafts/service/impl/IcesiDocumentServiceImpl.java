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

        checkErrorsInDocumentList(documentsDTO);

        List<IcesiDocument> icesiDocuments = documentsDTO.stream().map(document -> {
            IcesiDocument icesiDocument = documentMapper.fromIcesiDocumentDTO(document);
            IcesiUser user = getUserById(document.getUserId());
            icesiDocument.setIcesiUser(user);
            return icesiDocument;
        }).toList();

        documentRepository.saveAll(icesiDocuments);

        return icesiDocuments.stream()
                .map(documentMapper::fromIcesiDocument)
                .toList();
    }

    public void checkErrorsInDocumentList(List<IcesiDocumentDTO> documentsDTO) {
        // Pending
        // I need to do this method so that I can check all conditions while creating a list of documents
    }

    public IcesiUser getUserById(UUID userId) {
        if (userId == null)
            throw createIcesiException(
                    "User Id field is empty",
                    HttpStatus.NOT_FOUND,
                    new DetailBuilder(ErrorCode.ERR_REQUIRED_FIELD, "userId", "Id", null)
            ).get();

        return userRepository.findById(userId)
                .orElseThrow(
                        createIcesiException(
                                "User not found",
                                HttpStatus.NOT_FOUND,
                                new DetailBuilder(ErrorCode.ERR_REQUIRED_FIELD, "userId", "Id", userId)
                        )
                );
    }

    @Override
    public IcesiDocumentDTO updateDocument(String documentId, IcesiDocumentDTO icesiDocumentDTO) {
        if(icesiDocumentDTO.getStatus() == IcesiDocumentStatus.APPROVED) {
            throw createIcesiException(
                    "Can't update a document with APPROVED status",
                    HttpStatus.BAD_REQUEST,
                    new DetailBuilder(ErrorCode.ERR_500)
            ).get();
        }

        documentRepository.findByTitle(icesiDocumentDTO.getTitle()).ifPresent(
                document -> {
                    throw createIcesiException(
                            "There's already a document with that title",
                            HttpStatus.BAD_REQUEST,
                            new DetailBuilder(ErrorCode.ERR_500, "Document", "Title", icesiDocumentDTO.getTitle())
                    ).get();
                }
        );

        IcesiDocument document = documentRepository.findById(icesiDocumentDTO.getIcesiDocumentId()).orElseThrow(
                createIcesiException(
                        "Document not found",
                        HttpStatus.NOT_FOUND,
                        new DetailBuilder(ErrorCode.ERR_404, "Document", "Id", documentId)
                )
        );

        document.setTitle(icesiDocumentDTO.getTitle());
        document.setText(icesiDocumentDTO.getText());
        document.setStatus(icesiDocumentDTO.getStatus());
        documentRepository.save(document);
        return documentMapper.fromIcesiDocument(document);
    }

    @Override
    public IcesiDocumentDTO createDocument(IcesiDocumentDTO icesiDocumentDTO) {
        if (icesiDocumentDTO.getUserId() == null) {
            throw createIcesiException(
                    "User id field is null",
                    HttpStatus.NOT_FOUND,
                    new DetailBuilder(ErrorCode.ERR_REQUIRED_FIELD, "userId", "Id", icesiDocumentDTO.getUserId())
            ).get();
        }

        documentRepository.findByTitle(icesiDocumentDTO.getTitle()).ifPresent(
                document -> {
                    throw createIcesiException(
                            "Document with that title already exists",
                            HttpStatus.BAD_REQUEST,
                            new DetailBuilder(ErrorCode.ERR_DUPLICATED, "Document", "Title", icesiDocumentDTO.getTitle())
                    ).get();
                }
        );

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
