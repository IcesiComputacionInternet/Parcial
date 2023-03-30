package co.edu.icesi.drafts.service.impl;

import co.edu.icesi.drafts.controller.IcesiDocumentController;
import co.edu.icesi.drafts.dto.IcesiDocumentDTO;
import co.edu.icesi.drafts.error.exception.*;
import co.edu.icesi.drafts.error.util.IcesiExceptionBuilder;
import co.edu.icesi.drafts.mapper.IcesiDocumentMapper;
import co.edu.icesi.drafts.model.IcesiDocument;
import co.edu.icesi.drafts.model.IcesiUser;
import co.edu.icesi.drafts.repository.IcesiDocumentRepository;
import co.edu.icesi.drafts.repository.IcesiUserRepository;
import co.edu.icesi.drafts.service.IcesiDocumentService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.lang.StackWalker.Option;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.swing.text.Document;

import static co.edu.icesi.drafts.error.util.IcesiExceptionBuilder.createIcesiException;


@AllArgsConstructor
class IcesiDocumentServiceImpl implements IcesiDocumentService {


    private final IcesiUserRepository userRepository;
    private final IcesiDocumentRepository documentRepository;
    private final IcesiDocumentMapper documentMapper;

    public IcesiDocumentServiceImpl(IcesiUserRepository userRepository, IcesiDocumentRepository documentRepository, IcesiDocumentMapper documentMapper, IcesiUserRepository icesiUserRepository) {
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
        List<IcesiDocumentDTO> documentsToSave = new ArrayList<>();
        if(documentsDTO.size() == documentsDTO.stream().distinct().count()) {//This validates if exists repeated title within the list of documents
            for (IcesiDocumentDTO document : documentsDTO) {
                IcesiUser user =   userRepository.findById(document.getUserId()).orElseThrow(() -> new RuntimeException("User " + document.getUserId() + " does not exists"));
                documentRepository.findByTitle(document.getTitle()).orElseThrow(() -> new RuntimeException("The title: " + document.getTitle() + " already exists"));
                documentsToSave.add(document);
            }
        }
        else {
            throw new RuntimeException("There are duplicated title within the list of documents to create");
        }

        return documentMapper.fromIcesiDocumentList(documentsToSave.stream()
            .map(documentMapper::fromIcesiDocumentDTO)
            .collect(Collectors.toList()));
    }

    @Override
    public IcesiDocumentDTO updateDocument(String documentId, IcesiDocumentDTO icesiDocumentDTO) {
        IcesiDocument doc = documentRepository.findById(documentId).orElseThrow(() -> new RuntimeException("This document does not exists"));
        if(doc.getStatus().toString().equals("DRAFT") || doc.getStatus().toString().equals("REVISION")) {
            doc.setTitle(icesiDocumentDTO.getTitle());
            doc.setText(icesiDocumentDTO.getText());
        }
        else {
            throw new RuntimeException("This document cannot be updated");
        }
        return documentMapper.fromIcesiDocument(doc);
    }

    @Override
    public IcesiDocumentDTO createDocument(IcesiDocumentDTO icesiDocumentDTO) {
        Optional.ofNullable(icesiDocumentDTO.getUserId())
            .orElseThrow(
                createIcesiException(
                    "field userId is required",
                    HttpStatus.NOT_FOUND,
                    new DetailBuilder(ErrorCode.ERR_REQUIRED_FIELD, "userId", icesiDocumentDTO.getUserId())
            )
            );
        documentRepository.findByTitle(icesiDocumentDTO.getTitle()).ifPresent(
            e -> {
                throw createIcesiException(
                    "resource Document with field Title: Some title, already exists",
                    HttpStatus.NOT_FOUND,
                    new DetailBuilder(ErrorCode.ERR_DUPLICATED, "Document", "Title", icesiDocumentDTO.getTitle())
                ).get();
        }); 
        
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
