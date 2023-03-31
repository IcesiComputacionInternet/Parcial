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
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static co.edu.icesi.drafts.error.util.IcesiExceptionBuilder.createIcesiException;

@Service
@Component
class IcesiDocumentServiceImpl implements IcesiDocumentService {


    private final IcesiUserRepository userRepository;
    private final IcesiDocumentRepository documentRepository;
    private final IcesiDocumentMapper documentMapper;

    private static final String DOCUMENT = "Document";

    public IcesiDocumentServiceImpl(IcesiUserRepository userRepository, IcesiDocumentRepository documentRepository, @Qualifier("icesiDocumentMapperImpl") IcesiDocumentMapper documentMapper) {
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

    //Get a document by id
    private IcesiDocumentDTO getDocumentById(String documentId) {
        return documentRepository.findById(UUID.fromString(documentId))
                .map(documentMapper::fromIcesiDocument)
                .orElseThrow(
                        createIcesiException(
                                "Document not found",
                                HttpStatus.NOT_FOUND,
                                new DetailBuilder(ErrorCode.ERR_404, DOCUMENT, "Id", documentId)
                        )
                );
    }


    @Override
    public List<IcesiDocumentDTO> createDocuments(List<IcesiDocumentDTO> documentsDTO) {
        validateListOfDocuments(documentsDTO);

        List<IcesiDocument> documents = documentsDTO.stream().collect(ArrayList::new, (list, doc) ->
                {
                    IcesiDocument document = documentMapper.fromIcesiDocumentDTO(doc);
                    IcesiUser user = getUserByID(doc.getUserId());
                    document.setIcesiUser(user);
                    list.add(document);
                },
                ArrayList::addAll
        );

        documentRepository.saveAll(documents);

        return documents.stream().map(documentMapper::fromIcesiDocument).toList();
    }

    private IcesiUser getUserByID(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(
                        createIcesiException(
                                "User not found",
                                HttpStatus.NOT_FOUND,
                                new DetailBuilder(ErrorCode.ERR_404, "User", "Id", id.toString()
                        )
                ));
    }

    private void validateListOfDocuments(List<IcesiDocumentDTO> documentsDTO){
        List<IcesiErrorDetail> errors = validateTitles(documentsDTO);

        errors.addAll(validateUsersNotFound(documentsDTO));

        if (!errors.isEmpty()) {
            throw new IcesiException(
                    "There are errors to create documents",
                    IcesiError.builder()
                            .status(HttpStatus.BAD_REQUEST)
                            .details(errors)
                            .build()
            );
        }
    }

    //Validate that no document is created with a title that already exists
    private List<IcesiErrorDetail> validateTitles(List<IcesiDocumentDTO> documentsDTO) {
        return new ArrayList<>(documentsDTO.stream().filter(documentDTO -> documentRepository.findByTitle(documentDTO.getTitle()).isPresent())
                .map(documentDTO -> new IcesiErrorDetail(
                        "ERR_DUPLICATED",
                        "resource Document with field Title: %s, already exists".formatted(documentDTO.getTitle())
                ))
                .toList());
    }

    //Validate that the user of each document exists
    private List<IcesiErrorDetail> validateUsersNotFound(List<IcesiDocumentDTO> documentsDTO) {
        return new ArrayList<>(documentsDTO.stream().filter(documentDTO -> userRepository.findById(documentDTO.getUserId()).isEmpty())
                .map(documentDTO -> new IcesiErrorDetail(
                        "ERR_404",
                        "User with Id: %s not found".formatted(documentDTO.getUserId())
                ))
                .toList());
    }

    @Override
    public IcesiDocumentDTO updateDocument(String documentId, IcesiDocumentDTO icesiDocumentDTO) {
        validateDocumentStatus(documentId);
        validateDocumentTitle(icesiDocumentDTO.getTitle());
        var document = documentRepository.findById(UUID.fromString(documentId))
                .orElseThrow(
                        createIcesiException(
                                "Document not found",
                                HttpStatus.NOT_FOUND,
                                new DetailBuilder(ErrorCode.ERR_404, DOCUMENT, "Id", documentId)
                        )
                );
        validateDocumentUser(document.getIcesiUser().getIcesiUserId(), icesiDocumentDTO.getUserId());
        document.setTitle(icesiDocumentDTO.getTitle());
        document.setText(icesiDocumentDTO.getText());
        document.setStatus(icesiDocumentDTO.getStatus());
        return documentMapper.fromIcesiDocument(documentRepository.save(document));
    }

    //Validate if the document is on approved status
    private void validateDocumentStatus(String actualDocumentId) {
        IcesiDocumentDTO actualDocument = getDocumentById(actualDocumentId);
        if (Objects.equals(actualDocument.getStatus(), IcesiDocumentStatus.APPROVED)) {
            throw createIcesiException(
                    "The document is currently approved, it cannot be updated",
                    HttpStatus.BAD_REQUEST,
                    new DetailBuilder(ErrorCode.ERR_500)).get();
        }
    }

    //Validate if you are trying to change the user of the document
    private void validateDocumentUser(UUID actualDocumentId, UUID newUserId) {
        if (!Objects.equals(actualDocumentId, newUserId)) {
            throw createIcesiException(
                    "You are trying to change the user of the document",
                    HttpStatus.BAD_REQUEST,
                    new DetailBuilder(ErrorCode.ERR_400, DOCUMENT, "User", newUserId)
            ).get();
        }
    }

    //Validate the title of the document, it must be unique
    private void validateDocumentTitle(String title) {
        if (documentRepository.findByTitle(title).isPresent()) {
            throw createIcesiException(
                    "Document title already exists",
                    HttpStatus.BAD_REQUEST,
                    new DetailBuilder(ErrorCode.ERR_DUPLICATED, DOCUMENT, "Title", title)
            ).get();
        }
    }

    //Validate user id not null in a document dto
    private void validateUserIdNotNull(IcesiDocumentDTO icesiDocumentDTO) {
        if (Objects.isNull(icesiDocumentDTO.getUserId())) {
            throw createIcesiException(
                    "User id is null",
                    HttpStatus.BAD_REQUEST,
                    new DetailBuilder(ErrorCode.ERR_REQUIRED_FIELD, "userId", "User", null)
            ).get();
        }
    }

    @Override
    public IcesiDocumentDTO createDocument(IcesiDocumentDTO icesiDocumentDTO) {
        validateUserIdNotNull(icesiDocumentDTO);
        validateDocumentTitle(icesiDocumentDTO.getTitle());
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

