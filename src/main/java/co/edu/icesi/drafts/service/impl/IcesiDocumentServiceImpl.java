package co.edu.icesi.drafts.service.impl;

import co.edu.icesi.drafts.controller.IcesiDocumentController;
import co.edu.icesi.drafts.dto.IcesiDocumentDTO;
import co.edu.icesi.drafts.error.exception.*;
import co.edu.icesi.drafts.mapper.IcesiDocumentMapper;
import co.edu.icesi.drafts.model.IcesiDocument;
import co.edu.icesi.drafts.model.IcesiDocumentStatus;
import co.edu.icesi.drafts.model.IcesiUser;
import co.edu.icesi.drafts.repository.IcesiDocumentRepository;
import co.edu.icesi.drafts.repository.IcesiUserRepository;
import co.edu.icesi.drafts.service.IcesiDocumentService;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static co.edu.icesi.drafts.error.util.IcesiExceptionBuilder.createIcesiException;


@AllArgsConstructor
@Service
class IcesiDocumentServiceImpl implements IcesiDocumentService {


    private final IcesiUserRepository userRepository;
    private final IcesiDocumentRepository documentRepository;
    private final IcesiDocumentMapper documentMapper;
    private IcesiDocumentController documentController;


    public IcesiDocumentServiceImpl(IcesiUserRepository userRepository, IcesiDocumentRepository documentRepository, @Qualifier("notFunctionalMapper")IcesiDocumentMapper documentMapper) {
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
    public List<IcesiDocumentDTO> createDocuments(List<IcesiDocumentDTO> documentsDTOs) {
        List<DetailBuilder> errors = new ArrayList<>();
        errors.addAll(addUserDoesNotExistErrors(documentsDTOs));
        errors.addAll(addTitleAlreadyExistErrors(documentsDTOs));
        if (!errors.isEmpty()){
            throw createIcesiException(
                    "Some documents have errors",
                    HttpStatus.BAD_REQUEST,
                    errors.stream().toArray(DetailBuilder[]::new)
            ).get();
        }
        List<IcesiDocument> documentsList = assignUserToEachDocument(documentsDTOs);
        documentRepository.saveAll(documentsList);
        return documentsList.stream().map(document -> documentMapper.fromIcesiDocument(document)).toList();
    }

    public List<IcesiDocument> assignUserToEachDocument(List<IcesiDocumentDTO> documentsDTOs){
        return documentsDTOs.stream()
                .map(documentDTO -> {
                    IcesiDocument document = documentMapper.fromIcesiDocumentDTO(documentDTO);
                    document.setIcesiUser(validateIfUserExists(document.getIcesiDocumentId()));
                    return document;
                }).toList();
    }

    public List<DetailBuilder> addUserDoesNotExistErrors(List<IcesiDocumentDTO> documentsDTO){
        return documentsDTO.stream()
                .filter(documentDTO -> !userRepository.findById(documentDTO.getUserId()).isPresent())
                .map(documentDTO -> new DetailBuilder(ErrorCode.ERR_404, "User", "Id", documentDTO.getUserId()))
                .toList();
    }

    public List<DetailBuilder> addTitleAlreadyExistErrors(List<IcesiDocumentDTO> documentsDTO){
        return documentsDTO.stream()
                .filter(documentDTO -> documentRepository.findByTitle(documentDTO.getTitle()).isPresent())
                .map(documentDTO -> new DetailBuilder(ErrorCode.ERR_DUPLICATED, "Document","Title", documentDTO.getTitle()))
                .toList();
    }

    @Override
    public IcesiDocumentDTO updateDocument(String documentId, IcesiDocumentDTO icesiDocumentDTO) {
        var icesiDocument = validateIfDocumentExists(documentId);
        icesiDocument.setStatus(icesiDocumentDTO.getStatus());

        validateDocumentStatus(icesiDocumentDTO.getStatus());
        validateIfNewTitleExists(icesiDocumentDTO.getTitle());

        icesiDocument.setText(icesiDocumentDTO.getText());
        icesiDocument.setTitle(icesiDocumentDTO.getTitle());

        return  documentMapper.fromIcesiDocument(documentRepository.save(icesiDocument));
    }

    private IcesiDocument validateIfDocumentExists(String documentId){
        var document = documentRepository.findByIcesiDocumentId(documentId)
                .orElseThrow(
                        createIcesiException(
                                "Document not found",
                                HttpStatus.NOT_FOUND,
                                new DetailBuilder(ErrorCode.ERR_404, "Document", "Id", documentId)
                        )
                );
        return document;
    }

    private void validateDocumentStatus(IcesiDocumentStatus documentStatus){
        if (documentStatus == IcesiDocumentStatus.APPROVED){
            throw createIcesiException(
                    "Only documents of type draft or revision can be updated",
                    HttpStatus.FORBIDDEN,
                    new DetailBuilder(ErrorCode.ERR_400, "status is", documentStatus.name())
            ).get();
        }
    }

    private void validateIfNewTitleExists(String titleName) {
        var title= documentRepository.findByTitle(titleName);
        if(title.isPresent()){
            throw createIcesiException(
                    "The entered title already exists",
                    HttpStatus.CONFLICT,
                    new DetailBuilder(ErrorCode.ERR_DUPLICATED, "Document","Title", titleName)
            ).get();
        }
    }

    @Override
    public IcesiDocumentDTO createDocument(IcesiDocumentDTO icesiDocumentDTO) {
        validateIfUserIsNotNull(icesiDocumentDTO.getUserId());
        var user = validateIfUserExists(icesiDocumentDTO.getUserId());
        validateIfNewTitleExists(icesiDocumentDTO.getTitle());
        var icesiDocument = documentMapper.fromIcesiDocumentDTO(icesiDocumentDTO);
        icesiDocument.setIcesiUser(user);
        return documentMapper.fromIcesiDocument(documentRepository.save(icesiDocument));
    }

    private void validateIfUserIsNotNull(UUID userId){
        Optional.ofNullable(userId).orElseThrow(
                createIcesiException(
                        "User id is null",
                        HttpStatus.BAD_REQUEST,
                        new DetailBuilder(ErrorCode.ERR_REQUIRED_FIELD, "userId", userId)
                )
        );
    }

    private IcesiUser validateIfUserExists(UUID userId){
        var user = userRepository.findById(userId)
                .orElseThrow(
                        createIcesiException(
                                "User not found",
                                HttpStatus.NOT_FOUND,
                                new DetailBuilder(ErrorCode.ERR_404, "User", "Id", userId)
                        )
                );
        return user;
    }
}
