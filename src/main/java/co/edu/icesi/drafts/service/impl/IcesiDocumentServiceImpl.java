package co.edu.icesi.drafts.service.impl;

import co.edu.icesi.drafts.controller.IcesiDocumentController;
import co.edu.icesi.drafts.dto.IcesiDocumentDTO;
import co.edu.icesi.drafts.error.exception.*;
import co.edu.icesi.drafts.error.util.IcesiExceptionBuilder;
import co.edu.icesi.drafts.mapper.IcesiDocumentMapper;
import co.edu.icesi.drafts.model.IcesiDocument;
import co.edu.icesi.drafts.model.IcesiDocumentStatus;
import co.edu.icesi.drafts.repository.IcesiDocumentRepository;
import co.edu.icesi.drafts.repository.IcesiUserRepository;
import co.edu.icesi.drafts.service.IcesiDocumentService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
        documentsDTO.stream().forEach((doc)->{
            createDocument(doc);
        });
        return null;
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
        var doc = documentRepository.findByTitle(icesiDocumentDTO.getTitle()).orElseThrow(
                createIcesiException(
                        "Document not found",
                        HttpStatus.NOT_FOUND,
                        new DetailBuilder(ErrorCode.ERR_DUPLICATED, "Document", "Title", icesiDocumentDTO.getTitle())
                )
        );

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
