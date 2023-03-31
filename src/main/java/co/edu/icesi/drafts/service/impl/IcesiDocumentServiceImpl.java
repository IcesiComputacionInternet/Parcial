package co.edu.icesi.drafts.service.impl;

import co.edu.icesi.drafts.dto.IcesiDocumentDTO;
import co.edu.icesi.drafts.error.exception.*;
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
import java.util.UUID;
import java.util.stream.Stream;

import static co.edu.icesi.drafts.error.util.IcesiExceptionBuilder.createIcesiException;

@Service
@AllArgsConstructor
class IcesiDocumentServiceImpl implements IcesiDocumentService {
    private final IcesiUserRepository userRepository;
    private final IcesiDocumentRepository documentRepository;
    private final IcesiDocumentMapper documentMapper;

    @Override
    public List<IcesiDocumentDTO> getAllDocuments() {
        return documentRepository.findAll().stream()
                .map(documentMapper::fromIcesiDocument)
                .toList();
    }

    @Override
    public List<IcesiDocumentDTO> createDocuments(List<IcesiDocumentDTO> documentsDTO) {
        validateErrorsDocuments(documentsDTO);
        List<IcesiDocument> documents = documentsDTO.stream().map(dto -> {
            var icesiDocument = documentMapper.fromIcesiDocumentDTO(dto);
            icesiDocument.setIcesiUser(userRepository.findById(dto.getUserId())
                    .orElseThrow(
                            createIcesiException(
                                    "User not found",
                                    HttpStatus.NOT_FOUND,
                                    new DetailBuilder(ErrorCode.ERR_404, "User", "Id", dto.getUserId())
                            )
                    ));
            return icesiDocument;
        }).toList();
        documentRepository.saveAll(documents);
        return documents.stream().map(documentMapper::fromIcesiDocument).toList();
    }

    private void validateErrorsDocuments(List<IcesiDocumentDTO> documentsDTO) {
        List<IcesiErrorDetail> errors = new ArrayList<>();
        errors.addAll(validateAllTitles(documentsDTO));
        errors.addAll(validateAllUsers(documentsDTO));
        if (!errors.isEmpty())
            throw new IcesiException(HttpStatus.BAD_REQUEST.toString(), IcesiError.builder().status(HttpStatus.BAD_REQUEST).details(errors).build());
    }

    private List<IcesiErrorDetail> validateAllTitles(List<IcesiDocumentDTO> documentsDTO) {
        return documentsDTO.stream().filter(dto -> documentRepository.findByTitle(dto.getTitle()).isPresent())
                .map(dto -> new IcesiErrorDetail("ERR_DUPLICATED", "resource Document with field Title: " +
                        dto.getTitle() + ", already exists")).toList();
    }

    private List<IcesiErrorDetail> validateAllUsers(List<IcesiDocumentDTO> documentsDTO) {
        return documentsDTO.stream().filter(dto -> userRepository.findById(dto.getUserId()).isEmpty()).map(dto -> new IcesiErrorDetail("ERR_404", "User with Id: " + dto.getUserId() + " not found")).toList();
    }

    @Override
    public IcesiDocumentDTO updateDocument(String documentId, IcesiDocumentDTO icesiDocumentDTO) {
        UUID documentUUID = UUID.fromString(documentId);
        IcesiDocument document = documentRepository.findById(documentUUID)
                .orElseThrow(
                    createIcesiException(
                            "Document not found",
                            HttpStatus.NOT_FOUND,
                            new DetailBuilder(ErrorCode.ERR_404, "Document", "Id", UUID.fromString(documentId))
                    )
        );
        validateTitleOrTextChange(document, icesiDocumentDTO);
        validateUserNotChange(document, icesiDocumentDTO);
        validateTitleChange(document, icesiDocumentDTO);
        return documentMapper.fromIcesiDocument(documentRepository.updateById(documentUUID, icesiDocumentDTO.getTitle(), icesiDocumentDTO.getText()).orElse(null));
    }

    private void validateUserNotChange(IcesiDocument document, IcesiDocumentDTO documentDTO) {
        if (!document.getIcesiUser().getIcesiUserId().equals(documentDTO.getUserId()))
            throw new IcesiException(HttpStatus.BAD_REQUEST.toString(), IcesiError.builder().status(HttpStatus.BAD_REQUEST).details(Stream.of(IcesiErrorDetail.builder().errorCode(ErrorCode.ERR_404.getCode()).errorMessage("THE USER CAN'T BE CHANGED").build()).toList()).build());
    }

    private void validateTitleOrTextChange(IcesiDocument document, IcesiDocumentDTO documentDTO) {
        if (document.getStatus() == IcesiDocumentStatus.APPROVED &&
                !document.getTitle().equals(documentDTO.getTitle()) || !document.getText().equals(documentDTO.getText()))
            throw new IcesiException(HttpStatus.BAD_REQUEST.toString(), IcesiError.builder().status(HttpStatus.BAD_REQUEST).details(Stream.of(IcesiErrorDetail.builder().errorCode(ErrorCode.ERR_404.getCode()).errorMessage("CAN'T MODIFY AN APPROVED DOCUMENT").build()).toList()).build());
    }

    private void validateTitleChange(IcesiDocument document, IcesiDocumentDTO documentDTO) {
        if (!document.getTitle().equals(documentDTO.getTitle()))
            validateUniqueTitle(documentDTO);
    }

    @Override
    public IcesiDocumentDTO createDocument(IcesiDocumentDTO icesiDocumentDTO) {
        validateUserIdNotNull(icesiDocumentDTO);
        validateUniqueTitle(icesiDocumentDTO);
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

    private void validateUserIdNotNull(IcesiDocumentDTO icesiDocumentDTO) {
        if (icesiDocumentDTO.getUserId() == null)
            throw createIcesiException("USER ID MUST NOT BE NULL", HttpStatus.BAD_REQUEST, new DetailBuilder(ErrorCode.ERR_REQUIRED_FIELD, "userId")).get();
    }

    private void validateUniqueTitle(IcesiDocumentDTO icesiDocumentDTO) {
        if (documentRepository.findByTitle(icesiDocumentDTO.getTitle()).isPresent())
            throw createIcesiException("TITLE MUST BE UNIQUE", HttpStatus.BAD_REQUEST, new DetailBuilder(ErrorCode.ERR_DUPLICATED, "Document", "Title", icesiDocumentDTO.getTitle())).get();
    }
}
