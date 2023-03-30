package co.edu.icesi.drafts.service.impl;

import co.edu.icesi.drafts.controller.IcesiDocumentController;
import co.edu.icesi.drafts.dto.IcesiDocumentDTO;
import co.edu.icesi.drafts.dto.IcesiUserDTO;
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
import java.util.Optional;

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

        List<IcesiDocumentDTO> errors = documentsDTO.stream(()->).toList();

    }

    @Override
    public IcesiDocumentDTO updateDocument(String documentId, IcesiDocumentDTO icesiDocumentDTO) {
        var document = documentRepository.findById(icesiDocumentDTO.getIcesiDocumentId()).orElseThrow(createIcesiException(
                "Document not found",
                HttpStatus.NOT_FOUND,
                new DetailBuilder(ErrorCode.ERR_404, "Document", "Id", documentId)
        ));

        if(document.getIcesiUser().getIcesiUserId() != icesiDocumentDTO.getUserId()){
            createIcesiException(
                    "Try to update user, invalid action",
                    HttpStatus.NOT_ACCEPTABLE,
                    new DetailBuilder(ErrorCode.ERR_404, "Document", "Id", documentId)
            );
        }
        if(document.getStatus() == IcesiDocumentStatus.DRAFT || document.getStatus() == IcesiDocumentStatus.REVISION ){
            createIcesiException(
                    "Try to update approved document, invalid action",
                    HttpStatus.NOT_ACCEPTABLE,
                    new DetailBuilder(ErrorCode.ERR_404, "Document", "Id", documentId)
            );
        }

        if(documentRepository.findByTitle(icesiDocumentDTO.getTitle()).isPresent()){
            createIcesiException(
                    "Title must be unique",
                    HttpStatus.NOT_ACCEPTABLE,
                    new DetailBuilder(ErrorCode.ERR_404, "Document", "Id", documentId)
            );
        }

        document.setText(icesiDocumentDTO.getText());
        document.setTitle(icesiDocumentDTO.getTitle());
        document.setStatus(icesiDocumentDTO.getStatus());

        return documentMapper.fromIcesiDocument(documentRepository.save(document));
    }

    @Override
    public IcesiDocumentDTO createDocument(IcesiDocumentDTO icesiDocumentDTO) {

        IcesiDocumentDTO opDto = Optional.of(icesiDocumentDTO).orElseThrow(
                createIcesiException(
                        "IcesiDocumentDTO null",
                        HttpStatus.NOT_FOUND,
                        new DetailBuilder(ErrorCode.ERR_404, "DTO", "null", icesiDocumentDTO.getUserId())
                )
        );

        var user = userRepository.findById(icesiDocumentDTO.getUserId())
                .orElseThrow(
                        createIcesiException(
                                "User not found",
                                HttpStatus.NOT_FOUND,
                                new DetailBuilder(ErrorCode.ERR_404, "User", "Id", icesiDocumentDTO.getUserId())
                        )
                );

        var icesiDocument = documentMapper.fromIcesiDocumentDTO(opDto);
        icesiDocument.setIcesiUser(user);
        return documentMapper.fromIcesiDocument(documentRepository.save(icesiDocument));
    }
}
