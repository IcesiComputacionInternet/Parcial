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


import java.util.*;

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
        return null;
    }

    @Override
    public IcesiDocumentDTO updateDocument(String documentId, IcesiDocumentDTO icesiDocumentDTO) {
    checkDocumentStatus(icesiDocumentDTO);
    IcesiDocument toUpdateDocument = documentRepository.findById(icesiDocumentDTO.getIcesiDocumentId()).
            orElseThrow(
                    createIcesiException(
                            "Document not found",
                            HttpStatus.NOT_FOUND,
                            new DetailBuilder(ErrorCode.ERR_404, "Document", "Id", documentId)
                    )

            );
    toUpdateDocument.setTitle(icesiDocumentDTO.getTitle());
    checkTitle(icesiDocumentDTO.getTitle());
    toUpdateDocument.setText(icesiDocumentDTO.getText());
    toUpdateDocument.setStatus(icesiDocumentDTO.getStatus());
    return documentMapper.fromIcesiDocument(documentRepository.save(toUpdateDocument));
    }

    private void checkTitle(String title) {
        documentRepository.findByTitle(title).ifPresent(x->{
            throw createIcesiException(
                    "Title exists, please change",
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    new DetailBuilder(ErrorCode.ERR_DUPLICATED, "Document","Title",title)
            ).get();
        });
    }

    private void checkDocumentStatus(IcesiDocumentDTO icesiDocumentDTO) {
        if(icesiDocumentDTO.getStatus() == IcesiDocumentStatus.APPROVED){
            throw createIcesiException(
                    "Document status can be APPROVED, change that for continue",
                    HttpStatus.BAD_REQUEST,
                    new DetailBuilder(ErrorCode.ERR_500)).get();


        }
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
