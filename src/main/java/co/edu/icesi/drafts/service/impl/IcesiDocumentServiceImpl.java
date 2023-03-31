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

@Service
//@AllArgsConstructor
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

        documentsDTO.stream().map(doc -> {
            Optional.ofNullable(userRepository.findById(doc.getUserId())
                    .orElse())
        })

        List<IcesiUser> users = documentsDTO
                .stream().map(document -> document.getUserId())
                .map(user -> userRepository.findById(user).get()).toList();


        List<IcesiDocument> icesiDocuments = documentsDTO.stream().map(doc -> {
            IcesiDocument icesiDoc = documentMapper.fromIcesiDocumentDTO(doc);
            icesiDoc.setIcesiUser(userRepository.findById(doc.getUserId()).get());
            return icesiDoc;
        }).toList();

        documentRepository.saveAll(icesiDocuments);

        return icesiDocuments.stream().map(doc -> documentMapper.fromIcesiDocument(doc)).toList();
    }

    @Override
    public IcesiDocumentDTO updateDocument(String documentId, IcesiDocumentDTO icesiDocumentDTO) {

        IcesiDocument icesiDocument = documentRepository.findById(UUID.fromString(documentId)).get();

        if(icesiDocument.getStatus() == IcesiDocumentStatus.APPROVED){
            icesiDocument.setStatus(icesiDocument.getStatus());
            return documentMapper.fromIcesiDocument(icesiDocument);
        }if(documentRepository.findByTitle(icesiDocumentDTO.getTitle()).isPresent()){
            throw createIcesiException(
                    "Title duplicated",
                    HttpStatus.BAD_REQUEST,
                    new DetailBuilder(ErrorCode.ERR_DUPLICATED,"Document", "Title", icesiDocumentDTO.getTitle())
            ).get();
        }

        icesiDocument.setText(icesiDocument.getText());
        icesiDocument.setStatus(icesiDocument.getStatus());
        icesiDocument.setTitle(icesiDocument.getTitle());

        return documentMapper.fromIcesiDocument(icesiDocument);
    }

    @Override
    public IcesiDocumentDTO createDocument(IcesiDocumentDTO icesiDocumentDTO) {

        Optional.ofNullable(icesiDocumentDTO.getUserId()).orElseThrow(createIcesiException(
                "User not found",
                HttpStatus.BAD_REQUEST,
                new DetailBuilder(ErrorCode.ERR_REQUIRED_FIELD, "userId", icesiDocumentDTO.getUserId())
        ));


        var user = userRepository.findById(icesiDocumentDTO.getUserId())
                .orElseThrow(
                        createIcesiException(
                                "User not found",
                                HttpStatus.NOT_FOUND,
                                new DetailBuilder(ErrorCode.ERR_404, "User", "Id", icesiDocumentDTO.getUserId())
                        )
                );

        if(documentRepository.findByTitle(icesiDocumentDTO.getTitle()).isPresent()){
            throw createIcesiException(
                    "Title duplicated",
                    HttpStatus.BAD_REQUEST,
                    new DetailBuilder(ErrorCode.ERR_DUPLICATED,"Document", "Title", icesiDocumentDTO.getTitle())
            ).get();
        };
        var icesiDocument = documentMapper.fromIcesiDocumentDTO(icesiDocumentDTO);
        icesiDocument.setIcesiUser(user);
        return documentMapper.fromIcesiDocument(documentRepository.save(icesiDocument));
    }
}
