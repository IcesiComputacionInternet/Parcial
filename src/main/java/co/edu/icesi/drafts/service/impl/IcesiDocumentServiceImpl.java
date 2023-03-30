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


    @Override
    public List<IcesiDocumentDTO> createDocuments(List<IcesiDocumentDTO> documentsDTO) {
        documentsDTO.stream()
                .map(icesiDocumentDTO -> documentRepository.save(documentMapper.fromIcesiDocumentDTO(icesiDocumentDTO)));
        return documentsDTO;
    }

    @Override
    public IcesiDocumentDTO updateDocument(String documentId, IcesiDocumentDTO icesiDocumentDTO) {
        var document = documentRepository.findById(UUID.fromString(documentId))
                .orElseThrow(
                        createIcesiException(
                                "Document not found",
                                HttpStatus.NOT_FOUND,
                                new DetailBuilder(ErrorCode.ERR_404,"Document","Id",documentId)
                        )
                );
        if(document.getStatus().equals(IcesiDocumentStatus.APPROVED)){
           throw new RuntimeException("El documento ya fue aprovado.");
        }


        return null;
    }

    @Override
    public IcesiDocumentDTO createDocument(IcesiDocumentDTO icesiDocumentDTO) {
        if(documentRepository.findByTitle(icesiDocumentDTO.getTitle()).isPresent()) {
            throw createIcesiException(
                    "Title already exists",
                    HttpStatus.BAD_REQUEST,
                    new DetailBuilder(ErrorCode.ERR_DUPLICATED, "Document", "Title", icesiDocumentDTO.getTitle())
            ).get();
        }
        if(icesiDocumentDTO.getUserId() == null){
            throw createIcesiException(
                    "User is null",
                    HttpStatus.BAD_REQUEST,
                    new DetailBuilder(ErrorCode.ERR_REQUIRED_FIELD, "userId")
            ).get();
        }
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
