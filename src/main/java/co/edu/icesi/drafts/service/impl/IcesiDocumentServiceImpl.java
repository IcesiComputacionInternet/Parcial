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

import javax.swing.text.html.Option;
import java.util.*;

import static co.edu.icesi.drafts.error.util.IcesiExceptionBuilder.createIcesiException;

@Service
public class IcesiDocumentServiceImpl implements IcesiDocumentService {


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
        List<IcesiErrorDetail> errors = saveTitleErrors(documentsDTO);

        errors.addAll(saveUserErrors(documentsDTO));

        if (!errors.isEmpty()) {
            throw new IcesiException(
                    "Errores",
                    IcesiError.builder()
                            .status(HttpStatus.BAD_REQUEST)
                            .details(errors)
                            .build()
            );
        }

        List<IcesiDocument> documents = documentsDTO.stream()
                        .map((doc) ->{
                            var d = documentMapper.fromIcesiDocumentDTO(doc);
                            d.setIcesiUser(validateUserNotFound(doc.getUserId()));
                            return d;
                        })
                .toList();

        documentRepository.saveAll(documents);

        return documents.stream()
                .map(documentMapper::fromIcesiDocument)
                .toList();



    }

    private List<IcesiErrorDetail> saveTitleErrors(List<IcesiDocumentDTO> docDTO){
        return new ArrayList<>(
                docDTO.stream()
                        .filter(doc -> documentRepository.findByTitle(doc.getTitle()).isPresent())
                        .map(doc -> new IcesiErrorDetail(
                                "ERR_DUPLICATED",
                                "resource Document with field Title: %s, already exists".formatted(doc.getTitle())
                        ))
                        .toList()

        );
    }

    private List<IcesiErrorDetail> saveUserErrors(List<IcesiDocumentDTO> docDTO){
        return new ArrayList<>(
                docDTO.stream()
                        .filter(doc -> userRepository.findById(doc.getUserId()).isEmpty())
                        .map(doc -> new IcesiErrorDetail(
                                "ERR_404",
                                "%s with %s: %s not found".formatted("User", "Id", doc.getUserId())
                        ))
                        .toList()

        );
    }



    @Override
    public IcesiDocumentDTO updateDocument(String documentId, IcesiDocumentDTO icesiDocumentDTO) {
        validateStatusDocument(icesiDocumentDTO);
        var document = documentRepository.findById(icesiDocumentDTO.getIcesiDocumentId()).orElseThrow(
                createIcesiException(
                        "Document not found",
                        HttpStatus.NOT_FOUND,
                        new DetailBuilder(ErrorCode.ERR_404, "Document", "Id", documentId)
                )
        );
        document.setTitle(icesiDocumentDTO.getTitle());
        validateTitle(icesiDocumentDTO.getTitle());
        document.setText(icesiDocumentDTO.getText());
        document.setStatus(icesiDocumentDTO.getStatus());
        return documentMapper.fromIcesiDocument(documentRepository.save(document));
    }

    private void validateStatusDocument(IcesiDocumentDTO icesiDocumentDTO) {
        if (icesiDocumentDTO.getStatus() == IcesiDocumentStatus.APPROVED) {
            throw createIcesiException(
                    "The status of the document is not valid to update",
                    HttpStatus.BAD_REQUEST,
                    new DetailBuilder(ErrorCode.ERR_500)).get();
        }
    }

    @Override
    public IcesiDocumentDTO createDocument(IcesiDocumentDTO icesiDocumentDTO) {
        validateTitle(icesiDocumentDTO.getTitle());

        validateUserIdNotNull(icesiDocumentDTO.getUserId());
        var user = validateUserNotFound(icesiDocumentDTO.getUserId());

        var icesiDocument = documentMapper.fromIcesiDocumentDTO(icesiDocumentDTO);
        icesiDocument.setIcesiUser(user);
        return documentMapper.fromIcesiDocument(documentRepository.save(icesiDocument));
    }

    private void validateTitle(String title) {
        documentRepository.findByTitle(title)
                .ifPresent((doc) -> {
                    throw createIcesiException(
                            "Document is present",
                            HttpStatus.NOT_FOUND,
                            new DetailBuilder(ErrorCode.ERR_DUPLICATED, "Document", "Title", title)).get();
                });

    }

    private IcesiUser validateUserNotFound(UUID userId){
        return userRepository.findById(userId)
                .orElseThrow(
                        createIcesiException(
                                "User not found",
                                HttpStatus.NOT_FOUND,
                                new DetailBuilder(ErrorCode.ERR_404, "User", "Id", userId.toString())
                        )
                );

    }
    private void validateUserIdNotNull(UUID userId) {
        if (userId == null) {
            throw createIcesiException(
                    "User id is required",
                    HttpStatus.NOT_FOUND,
                    new DetailBuilder(ErrorCode.ERR_REQUIRED_FIELD, "userId")).get();
        }
    }




}
