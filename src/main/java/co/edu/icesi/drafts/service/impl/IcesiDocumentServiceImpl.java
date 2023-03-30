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
import org.springframework.context.annotation.Primary;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.*;

import static co.edu.icesi.drafts.error.util.IcesiExceptionBuilder.createIcesiException;
import static co.edu.icesi.drafts.error.util.IcesiExceptionBuilder.mapToIcesiErrorDetail;

@Service
public class IcesiDocumentServiceImpl implements IcesiDocumentService {


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
        return null;
    }

    @Override
    public IcesiDocumentDTO updateDocument(String documentId, IcesiDocumentDTO icesiDocumentDTO) {
        IcesiDocument icesiDocument = documentRepository.findById(UUID.fromString(documentId))
                .orElseThrow(() -> new RuntimeException("IcesiDocument not found with id " + documentId));

        if (!icesiDocument.getStatus().equals(IcesiDocumentStatus.DRAFT) && !icesiDocument.getStatus().equals(IcesiDocumentStatus.REVISION)) {
            throw new IllegalStateException("Cannot update IcesiDocument with id " + documentId + " because its state is " + icesiDocument.getStatus());
        }

        if (!icesiDocument.getTitle().equals(icesiDocumentDTO.getTitle())) {
            Optional<IcesiDocument> existingDocumentWithTitle = documentRepository.findByTitle(icesiDocumentDTO.getTitle());
            if (existingDocumentWithTitle.isPresent()) {
                throw new RuntimeException("Another IcesiDocument already exists with title " + icesiDocumentDTO.getTitle());
            }
        }

        icesiDocument.setTitle(icesiDocumentDTO.getTitle());
        icesiDocument.setText(icesiDocumentDTO.getText());
        icesiDocument.setStatus(icesiDocumentDTO.getStatus());

        documentRepository.save(icesiDocument);
        return icesiDocumentDTO;
    }

    @Override
    public IcesiDocumentDTO createDocument(IcesiDocumentDTO icesiDocumentDTO) {
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
