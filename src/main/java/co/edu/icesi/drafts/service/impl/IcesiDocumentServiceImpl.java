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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.Null;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static co.edu.icesi.drafts.error.util.IcesiExceptionBuilder.createIcesiException;

@Service
class IcesiDocumentServiceImpl implements IcesiDocumentService {


    private final IcesiUserRepository userRepository;
    private final IcesiDocumentRepository documentRepository;
    private final IcesiDocumentMapper documentMapper;
    private IcesiDocumentController documentController;

    public IcesiDocumentServiceImpl(IcesiUserRepository userRepository, IcesiDocumentRepository documentRepository, @Qualifier("notFunctionalMapper") IcesiDocumentMapper documentMapper) {
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
        List<DetailBuilder> errorList = titleErrors(documentsDTO);
        errorList.addAll(userErrors(documentsDTO));

        if(!errorList.isEmpty()){
            throw createIcesiException(
                    "Errors creating new documents",
                    HttpStatus.BAD_REQUEST,
                    errorList.toArray(DetailBuilder[]::new)).get();

        }

        List<IcesiDocument> documents = documentsDTO.stream().map(docs -> {
            var doc = documentMapper.fromIcesiDocumentDTO(docs);
            doc.setIcesiUser(checkUserExists(docs.getUserId()));
            return doc;
        }).toList();

        return documentRepository.saveAll(documents).stream().map(documentMapper::fromIcesiDocument).toList();
    }

    private List<DetailBuilder> titleErrors(List<IcesiDocumentDTO> icesiDocumentDTO){
        return new ArrayList<>(
                icesiDocumentDTO.stream()
                        .filter(documentDTO -> documentRepository.findByTitle(documentDTO.getTitle()).isPresent())
                        .map(documentDTO -> new DetailBuilder(ErrorCode.ERR_DUPLICATED, "Document", "Title",documentDTO.getTitle()))
                        .toList());

    }

    private List<DetailBuilder> userErrors(List<IcesiDocumentDTO> icesiDocumentDTO){
        return new ArrayList<>(
                icesiDocumentDTO.stream()
                        .filter(documentDTO -> userRepository.findById(documentDTO.getUserId()).isEmpty())
                        .map(documentDTO -> new DetailBuilder(ErrorCode.ERR_404, "User", "Id", documentDTO.getUserId()))
                        .toList());
    }


    @Override
    public IcesiDocumentDTO updateDocument(String documentId, IcesiDocumentDTO icesiDocumentDTO) {
        var doc = documentRepository.findById(icesiDocumentDTO.getIcesiDocumentId()).orElseThrow(
                createIcesiException(
                        "Document does not exist",
                        HttpStatus.NOT_FOUND,
                        new DetailBuilder(ErrorCode.ERR_404, "Document", "Id",documentId))
        );
        doc.setStatus((icesiDocumentDTO.getStatus()));
        checkDocumentStatus(icesiDocumentDTO);
        doc.setTitle(icesiDocumentDTO.getTitle());
        checkTitle(icesiDocumentDTO.getTitle());
        doc.setText(icesiDocumentDTO.getText());
        return documentMapper.fromIcesiDocument(documentRepository.save(doc));
    }

    @Override
    public IcesiDocumentDTO createDocument(IcesiDocumentDTO icesiDocumentDTO) {
        checkTitle(icesiDocumentDTO.getTitle());
        checkUserNotNull(icesiDocumentDTO.getUserId());
        var user = checkUserExists(icesiDocumentDTO.getUserId());

        var icesiDocument = documentMapper.fromIcesiDocumentDTO(icesiDocumentDTO);
        icesiDocument.setIcesiUser(user);
        return documentMapper.fromIcesiDocument(documentRepository.save(icesiDocument));
    }

    private void checkTitle(String title){
        var titlePresent = documentRepository.findByTitle(title);
        titlePresent.ifPresent((document -> {
            throw createIcesiException(
                    "Document with this title already exists",
                    HttpStatus.CONFLICT,
                    new DetailBuilder(ErrorCode.ERR_DUPLICATED, "Document", "Title",title)).get();
        }));
    }

    private void checkDocumentStatus(IcesiDocumentDTO icesiDocumentDTO){
        if(icesiDocumentDTO.getStatus().equals(IcesiDocumentStatus.APPROVED)) {
            throw createIcesiException(
                    "Cannot do any modifications, status must be DRAFT or REVISION",
                    HttpStatus.BAD_REQUEST,
                    new DetailBuilder(ErrorCode.ERR_500)).get();
        }
    }

    private IcesiUser checkUserExists(UUID userId){
        return userRepository.findById(userId)
                .orElseThrow(
                        createIcesiException(
                                "User not found",
                                HttpStatus.NOT_FOUND,
                                new DetailBuilder(ErrorCode.ERR_404, "User", "Id", userId)
                        )
                );
    }

    private void checkUserNotNull(UUID userId){
        if(userId == null){
            throw createIcesiException(
                    "User cant be null",
                    HttpStatus.BAD_REQUEST,
                    new DetailBuilder(ErrorCode.ERR_REQUIRED_FIELD, "userId")).get();
        }
    }

}
