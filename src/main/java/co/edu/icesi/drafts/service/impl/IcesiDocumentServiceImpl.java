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
import lombok.NoArgsConstructor;
//import lombok.var;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

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
        return documentsDTO.stream().map(this::createDocument).toList();
    }

    @Override
    public IcesiDocumentDTO updateDocument(String documentId, IcesiDocumentDTO icesiDocumentDTO) {
        documentRepository.findById(UUID.fromString(documentId)).orElseThrow(
                createIcesiException("Document not found",
                        HttpStatus.NOT_FOUND,
                        new DetailBuilder(ErrorCode.ERR_400, "Document","Id", documentId)
                )
        );
        IcesiDocument upatedDocument = documentMapper.fromIcesiDocumentDTO(icesiDocumentDTO);
        validateStatus(upatedDocument);
        validateTitle(upatedDocument.getTitle());
        return documentMapper.fromIcesiDocument(documentRepository.save(upatedDocument));
    }

    @Override
    public IcesiDocumentDTO createDocument(IcesiDocumentDTO icesiDocumentDTO) {
        validateTitle(icesiDocumentDTO.getTitle());
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

    private  void  validateTitle(String title){
     documentRepository.findByTitle(title).ifPresent(e -> {
       throw createIcesiException(
                 "Title already exist",
                 HttpStatus.INTERNAL_SERVER_ERROR,
                 new DetailBuilder(ErrorCode.ERR_DUPLICATED, "Title", title)
         ).get();
    });

    }


    private void validateStatus(IcesiDocument document){
        if(!(document.getStatus().equals(IcesiDocumentStatus.DRAFT) || document.getStatus().equals(IcesiDocumentStatus.REVISION))){
            throw  new RuntimeException("The document: "+document.getIcesiDocumentId()+" can not be updated, it must have status DRAFT or REVISION");
        }
    }
}
