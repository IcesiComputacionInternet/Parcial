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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import static co.edu.icesi.drafts.error.util.IcesiExceptionBuilder.createIcesiException;


@Service
public class IcesiDocumentServiceImpl implements IcesiDocumentService {

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

        List<IcesiErrorDetail> errors = new ArrayList<>(documentsDTO.stream()
                .filter(documentDTO -> documentRepository.findByTitle(documentDTO.getTitle()).isPresent())
                .map(documentDTO -> new IcesiErrorDetail(
                        "ERR_DUPLICATED",
                        "resource Document with field Title: %s, already exists".formatted(documentDTO.getTitle())
                ))
                .toList());

        errors.addAll(documentsDTO.stream()
                .filter(documentDTO -> userRepository.findById(documentDTO.getUserId()).isEmpty())
                .map(documentDTO -> new IcesiErrorDetail(
                        "ERR_404",
                        "User with Id: %s not found".formatted(documentDTO.getUserId())
                ))
                .toList());

        if (!errors.isEmpty()) {
            throw new IcesiException(
                    "Errores al añadir",
                    IcesiError.builder()
                        .status(HttpStatus.BAD_REQUEST)
                        .details(errors)
                        .build()
            );
        }

        List<IcesiDocument> icesiDocuments = documentsDTO.stream()
                .collect(
                        ArrayList::new,
                        (l, d) -> {
                            IcesiDocument document = documentMapper.fromIcesiDocumentDTO(d);
                            document.setIcesiUser(getUserById(d.getUserId()));
                            l.add(document);
                        },
                        ArrayList::addAll
                );

        documentRepository.saveAll(icesiDocuments);

        return icesiDocuments.stream()
                .map(documentMapper::fromIcesiDocument)
                .toList();
    }

    @Override
    public IcesiDocumentDTO updateDocument(String documentId, IcesiDocumentDTO icesiDocumentDTO) {
        validateStatusDocument(icesiDocumentDTO);
        documentExistsByTitle(icesiDocumentDTO.getTitle());

        var document = documentRepository.findById(icesiDocumentDTO.getIcesiDocumentId()).orElseThrow(
                createIcesiException(
                        "Document not found",
                        HttpStatus.NOT_FOUND,
                        new DetailBuilder(ErrorCode.ERR_404, "Document", "Id", documentId)
                )
        );

        document.setTitle(icesiDocumentDTO.getTitle());
        document.setText(icesiDocumentDTO.getText());
        document.setStatus(icesiDocumentDTO.getStatus());
        return documentMapper.fromIcesiDocument(documentRepository.save(document));
    }

    private void validateStatusDocument(IcesiDocumentDTO icesiDocumentDTO){
        if (icesiDocumentDTO.getStatus() == IcesiDocumentStatus.APPROVED) {
            throw createIcesiException(
                    "The status of the document is not valid to update",
                    HttpStatus.BAD_REQUEST,
                    new DetailBuilder(ErrorCode.ERR_500)).get();
        }
    }

    @Override
    public IcesiDocumentDTO createDocument(IcesiDocumentDTO icesiDocumentDTO) {
        documentExistsByTitle(icesiDocumentDTO.getTitle());

        var user = getUserById(icesiDocumentDTO.getUserId());

        var icesiDocument = documentMapper.fromIcesiDocumentDTO(icesiDocumentDTO);
        icesiDocument.setIcesiUser(user);
        return documentMapper.fromIcesiDocument(documentRepository.save(icesiDocument));
    }

    public void documentExistsByTitle(String title){
        documentRepository.findByTitle(title).ifPresent(
                document -> {
                    throw createIcesiException(
                            "Document already exists",
                            HttpStatus.BAD_REQUEST,
                            new DetailBuilder(ErrorCode.ERR_DUPLICATED, "Document", "Title", title)
                    ).get();
                }
        );
    }

    public IcesiUser getUserById(UUID userId){
        if (userId == null)
            throw createIcesiException(
                    "User not found",
                    HttpStatus.NOT_FOUND,
                    new DetailBuilder(ErrorCode.ERR_REQUIRED_FIELD, "userId", "Id", null)
            ).get();

        return userRepository.findById(userId)
                .orElseThrow(
                        createIcesiException(
                                "User not found",
                                HttpStatus.NOT_FOUND,
                                new DetailBuilder(ErrorCode.ERR_REQUIRED_FIELD, "userId", "Id", userId)
                        )
                );
    }

}
