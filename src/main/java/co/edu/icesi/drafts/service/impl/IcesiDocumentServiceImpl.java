package co.edu.icesi.drafts.service.impl;

import co.edu.icesi.drafts.controller.IcesiDocumentController;
import co.edu.icesi.drafts.dto.IcesiDocumentDTO;
import co.edu.icesi.drafts.error.exception.*;
import co.edu.icesi.drafts.error.util.IcesiExceptionBuilder;
import co.edu.icesi.drafts.mapper.IcesiDocumentMapper;
import co.edu.icesi.drafts.model.IcesiDocument;
import co.edu.icesi.drafts.model.IcesiDocumentStatus;
import co.edu.icesi.drafts.repository.IcesiDocumentRepository;
import co.edu.icesi.drafts.repository.IcesiUserRepository;
import co.edu.icesi.drafts.service.IcesiDocumentService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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

    //Get a document by id
    private IcesiDocumentDTO getDocumentById(String documentId) {
        return documentRepository.findById(UUID.fromString(documentId))
                .map(documentMapper::fromIcesiDocument)
                .orElseThrow(
                        createIcesiException(
                                "Document not found",
                                HttpStatus.NOT_FOUND,
                                new DetailBuilder(ErrorCode.ERR_404, "Document", "Id", documentId)
                        )
                );
    }


    @Override
    public List<IcesiDocumentDTO> createDocuments(List<IcesiDocumentDTO> documentsDTO) {
        return null;
    }

    @Override
    public IcesiDocumentDTO updateDocument(String documentId, IcesiDocumentDTO icesiDocumentDTO) {
        validateDocumentStatus(documentId);
        var document = documentRepository.findById(UUID.fromString(documentId))
                .orElseThrow(
                        createIcesiException(
                                "Document not found",
                                HttpStatus.NOT_FOUND,
                                new DetailBuilder(ErrorCode.ERR_404, "Document", "Id", documentId)
                        )
                );
        validateDocumentUser(UUID.fromString(documentId), icesiDocumentDTO.getUserId());
        validateDocumentTitle(icesiDocumentDTO.getTitle());
        IcesiDocument updatedDocument = documentMapper.fromIcesiDocumentDTO(icesiDocumentDTO);
        return documentMapper.fromIcesiDocument(documentRepository.save(updatedDocument));
    }

    //Validate if the document is on approved status
    private void validateDocumentStatus(String actualDocumentId) {
        IcesiDocumentDTO actualDocument = getDocumentById(actualDocumentId);
        if (Objects.equals(actualDocument.getStatus(), IcesiDocumentStatus.APPROVED)) {
            throw createIcesiException(
                    "Document is on approved status",
                    HttpStatus.BAD_REQUEST,
                    new DetailBuilder(ErrorCode.ERR_400, "Document", "Status", actualDocument.getStatus())
            ).get();
        }
    }

    //Validate if you are trying to change the user of the document
    private void validateDocumentUser(UUID actualDocumentId, UUID newUserId) {
        if (!Objects.equals(actualDocumentId, newUserId)) {
            throw createIcesiException(
                    "You are trying to change the user of the document",
                    HttpStatus.BAD_REQUEST,
                    new DetailBuilder(ErrorCode.ERR_400, "Document", "User", newUserId)
            ).get();
        }
    }

    //Validate the title of the document, it must be unique
    private void validateDocumentTitle(String title) {
        if (documentRepository.findByTitle(title).isPresent()) {
            throw createIcesiException(
                    "Document title already exists",
                    HttpStatus.BAD_REQUEST,
                    new DetailBuilder(ErrorCode.ERR_400, "Document", "Title", title)
            ).get();
        }
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
