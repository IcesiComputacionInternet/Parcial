package co.edu.icesi.drafts.service.impl;

import co.edu.icesi.drafts.dto.IcesiDocumentDTO;
import co.edu.icesi.drafts.error.exception.*;
import co.edu.icesi.drafts.mapper.IcesiDocumentMapper;
import co.edu.icesi.drafts.model.IcesiDocument;
import co.edu.icesi.drafts.model.IcesiDocumentStatus;
import co.edu.icesi.drafts.model.IcesiUser;
import co.edu.icesi.drafts.repository.IcesiDocumentRepository;
import co.edu.icesi.drafts.repository.IcesiUserRepository;
import co.edu.icesi.drafts.service.IcesiDocumentService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static co.edu.icesi.drafts.error.util.IcesiExceptionBuilder.createIcesiException;


@Service
class IcesiDocumentServiceImpl implements IcesiDocumentService {

    private final IcesiUserRepository userRepository;
    private final IcesiDocumentRepository documentRepository;
    private final IcesiDocumentMapper documentMapper;

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
        List<IcesiErrorDetail> errorDetails = validateErrors(documentsDTO);
        if (!errorDetails.isEmpty()) {
            throw new IcesiException(
                    "Error creating the documents",
                    IcesiError.builder()
                            .status(HttpStatus.BAD_REQUEST)
                            .details(errorDetails)
                            .build()
            );
        }
        List<IcesiDocument> documents = documentsDTO.stream()
                .map(docDto -> {
                    IcesiDocument document = documentMapper.fromIcesiDocumentDTO(docDto);
                    IcesiUser user = getUser(docDto);
                    document.setIcesiUser(user);
                    return document;
                })
                .collect(Collectors.toList());
        documentRepository.saveAll(documents);
        return documents.stream().map(documentMapper::fromIcesiDocument).collect(Collectors.toList());
    }

    @Override
    public IcesiDocumentDTO updateDocument(String documentId, IcesiDocumentDTO icesiDocumentDTO) {
        IcesiDocument documentToUpdate = documentRepository.findById(UUID.fromString(documentId))
                .orElseThrow(createIcesiException(
                        "Not found",
                        HttpStatus.NOT_FOUND,
                        new DetailBuilder(ErrorCode.ERR_404, "Document","Id", documentId)));
        validateDocumentTitle(icesiDocumentDTO.getTitle());
        IcesiDocument updatedDocument = documentMapper.fromIcesiDocumentDTO(icesiDocumentDTO);
        updatedDocument.setIcesiDocumentId(documentToUpdate.getIcesiDocumentId());
        validateDocumentStatus(updatedDocument);
        IcesiDocument savedDocument = documentRepository.save(updatedDocument);
        return documentMapper.fromIcesiDocument(savedDocument);
    }

    @Override
    public IcesiDocumentDTO createDocument(IcesiDocumentDTO icesiDocumentDTO) {
        validateDocumentUserId(icesiDocumentDTO.getUserId());
        validateDocumentTitle(icesiDocumentDTO.getTitle());
        IcesiUser user = getUser(icesiDocumentDTO);
        IcesiDocument icesiDocument = documentMapper.fromIcesiDocumentDTO(icesiDocumentDTO);
        icesiDocument.setIcesiUser(user);
        return documentMapper.fromIcesiDocument(documentRepository.save(icesiDocument));
    }



    private List<IcesiErrorDetail> validateErrors(List<IcesiDocumentDTO> documentsDTO){
        List<IcesiErrorDetail> errors = new ArrayList<>();
        documentsDTO.forEach(doc -> {
            try {
                validateDocumentTitle(doc.getTitle());
            } catch (IcesiException ex) {
                errors.addAll(ex.getError().getDetails());
            }
            try {
                userRepository.findById(doc.getUserId())
                        .orElseThrow(
                                createIcesiException(
                                        "User not found",
                                        HttpStatus.NOT_FOUND,
                                        new DetailBuilder(ErrorCode.ERR_404, "User", "Id", doc.getUserId())
                                )
                        );
            }catch (IcesiException exception){
                errors.add(exception.getError().getDetails().get(0));
            }
        });
        return errors;
    }

    private void validateDocumentUserId(UUID idUser){
        if (idUser == null) {
            throw createIcesiException(
                    "The user ID is not valid",
                    HttpStatus.NOT_FOUND,
                    new DetailBuilder(ErrorCode.ERR_REQUIRED_FIELD, "userId", "User ID can not be null")
            ).get();
        }
    }

    private  void  validateDocumentTitle(String title){
        documentRepository.findByTitle(title).ifPresent(e -> {
            throw createIcesiException(
                    "This title already exist",
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    new DetailBuilder(ErrorCode.ERR_DUPLICATED, "Document","Title",title)
            ).get();
        });

    }

    private void validateDocumentStatus(IcesiDocument document) {
        if(!(document.getStatus().equals(IcesiDocumentStatus.DRAFT) || document.getStatus().equals(IcesiDocumentStatus.REVISION))) {
            throw createIcesiException(
                    "Invalid status",
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    new DetailBuilder(ErrorCode.ERR_400, "status", document.getStatus().toString())
            ).get();
        }
    }

    private IcesiUser getUser(IcesiDocumentDTO icesiDocumentDTO) {
        return userRepository.findById(icesiDocumentDTO.getUserId())
                .orElseThrow(
                        createIcesiException(
                                "User not found",
                                HttpStatus.NOT_FOUND,
                                new DetailBuilder(ErrorCode.ERR_404, "User", "Id", icesiDocumentDTO.getUserId())
                        )
                );
    }












}