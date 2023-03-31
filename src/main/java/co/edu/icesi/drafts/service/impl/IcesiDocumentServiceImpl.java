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

        // Check general errors in the documents
        // First, it validates whether we have any title repeated in the documents
        // Then, it checks if each document has a valid user.
        checkErrorsInDocumentList(documentsDTO);


        // Then we map the dto list to turn it into model list, adding the user to it in the process
        List<IcesiDocument> icesiDocuments = documentsDTO.stream().map(document -> {
            IcesiDocument icesiDocument = documentMapper.fromIcesiDocumentDTO(document);
            IcesiUser user = getUserById(document.getUserId());
            icesiDocument.setIcesiUser(user);
            return icesiDocument;
        }).toList();

        // We save the list as we checked the constraints before.
        documentRepository.saveAll(icesiDocuments);

        // Then we return the dto list of the icesiDocuments list we saved
        return icesiDocuments.stream()
                .map(documentMapper::fromIcesiDocument)
                .toList();
    }

    public List<IcesiErrorDetail> findTitleErrors(List<IcesiDocumentDTO> documentsDTO) {
        // It checks if there is any title repeated in a IcesiDocumentDTO list
        List<IcesiErrorDetail> titleErrors = new ArrayList<>(documentsDTO.stream()
                .filter(documentDTO -> documentRepository.findByTitle(documentDTO.getTitle()).isPresent())
                .map(documentDTO -> new IcesiErrorDetail(
                        "ERR_DUPLICATED",
                        "resource Document with field Title: %s, already exists".formatted(documentDTO.getTitle())
                ))
                .toList());

        return titleErrors;
    }

    public List<IcesiErrorDetail> findNotFoundUserErrors(List<IcesiDocumentDTO> documentsDTO) {
        // It checks if there is any not ofund user in a IcesiDocumentDTO list
        List<IcesiErrorDetail> notFoundUserErrors = new ArrayList<>(documentsDTO.stream()
                .filter(documentDTO -> userRepository.findById(documentDTO.getUserId()).isEmpty())
                .map(documentDTO -> new IcesiErrorDetail(
                        "ERR_404",
                        "User with Id: %s not found".formatted(documentDTO.getUserId())
                ))
                .toList());

        return notFoundUserErrors;
    }

    public void checkErrorsInDocumentList(List<IcesiDocumentDTO> documentsDTO) {
        // Pending
        // I need to do this method so that I can check all conditions while creating a list of documents
        // This document uses the findTitleErrors and findNotFoundUserErrors to determine whether there is any error in those categories
        // Then, it proceeds to save them all in an array and throw an exception whith the data required of the errors
        List<IcesiErrorDetail> errors = new ArrayList<>();

        errors.addAll(findTitleErrors(documentsDTO));
        errors.addAll(findNotFoundUserErrors(documentsDTO));

        if(errors.size() > 0)
            throw new IcesiException(
                    "Errores encontrados al intentar añadir",
                    IcesiError.builder()
                            .status(HttpStatus.BAD_REQUEST)
                            .details(errors)
                            .build()
            );
    }

    public IcesiUser getUserById(UUID userId) {
        // It checks wheter the userID field is null
        if (userId == null)
            throw createIcesiException(
                    "User Id field is empty",
                    HttpStatus.NOT_FOUND,
                    new DetailBuilder(ErrorCode.ERR_REQUIRED_FIELD, "userId", "Id", null)
            ).get();
        // If it wasn't null, returns the user found by thet userId or an exception if that ID doesn't exist.
        return userRepository.findById(userId)
                .orElseThrow(
                        createIcesiException(
                                "User not found",
                                HttpStatus.NOT_FOUND,
                                new DetailBuilder(ErrorCode.ERR_REQUIRED_FIELD, "userId", "Id", userId)
                        )
                );
    }



    @Override
    public IcesiDocumentDTO updateDocument(String documentId, IcesiDocumentDTO icesiDocumentDTO) {
        // It checks if the state is approved so it won't be able to update
        if(icesiDocumentDTO.getStatus() == IcesiDocumentStatus.APPROVED) {
            throw createIcesiException(
                    "Can't update a document with APPROVED status",
                    HttpStatus.BAD_REQUEST,
                    new DetailBuilder(ErrorCode.ERR_500)
            ).get();
        }
        // Finds if the new title already exists. Doesn't use the method as it has another error code
        documentRepository.findByTitle(icesiDocumentDTO.getTitle()).ifPresent(
                document -> {
                    throw createIcesiException(
                            "There's already a document with that title",
                            HttpStatus.BAD_REQUEST,
                            new DetailBuilder(ErrorCode.ERR_500, "Document", "Title", icesiDocumentDTO.getTitle())
                    ).get();
                }
        );

        IcesiDocument document = documentRepository.findById(icesiDocumentDTO.getIcesiDocumentId()).orElseThrow(
                createIcesiException(
                        "Document not found",
                        HttpStatus.NOT_FOUND,
                        new DetailBuilder(ErrorCode.ERR_404, "Document", "Id", documentId)
                )
        );

        document.setTitle(icesiDocumentDTO.getTitle());
        document.setText(icesiDocumentDTO.getText());
        document.setStatus(icesiDocumentDTO.getStatus());
        documentRepository.save(document);
        return documentMapper.fromIcesiDocument(document);
    }

    public void checkIfTitleIsRepeated(IcesiDocumentDTO icesiDocumentDTO) {
        documentRepository.findByTitle(icesiDocumentDTO.getTitle()).ifPresent(
                document -> {
                    throw createIcesiException(
                            "Document with that title already exists",
                            HttpStatus.BAD_REQUEST,
                            new DetailBuilder(ErrorCode.ERR_DUPLICATED, "Document", "Title", icesiDocumentDTO.getTitle())
                    ).get();
                }
        );
    }

    @Override
    public IcesiDocumentDTO createDocument(IcesiDocumentDTO icesiDocumentDTO) {
        if (icesiDocumentDTO.getUserId() == null) {
            throw createIcesiException(
                    "User id field is null",
                    HttpStatus.NOT_FOUND,
                    new DetailBuilder(ErrorCode.ERR_REQUIRED_FIELD, "userId", "Id", icesiDocumentDTO.getUserId())
            ).get();
        }

        checkIfTitleIsRepeated(icesiDocumentDTO);

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
