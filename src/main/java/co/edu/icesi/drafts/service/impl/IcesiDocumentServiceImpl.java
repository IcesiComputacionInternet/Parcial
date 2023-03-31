package co.edu.icesi.drafts.service.impl;

import co.edu.icesi.drafts.controller.IcesiDocumentController;
import co.edu.icesi.drafts.dto.IcesiDocumentDTO;
import co.edu.icesi.drafts.dto.IcesiUserDTO;
import co.edu.icesi.drafts.error.exception.*;
import co.edu.icesi.drafts.error.util.IcesiExceptionBuilder;
import co.edu.icesi.drafts.mapper.IcesiDocumentMapper;
import co.edu.icesi.drafts.model.IcesiDocument;
import co.edu.icesi.drafts.model.IcesiDocumentStatus;
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

        //List<IcesiErrorDetail> errors = documentsDTO.stream(()->).toList();
        return null;
    }

    @Override
    public IcesiDocumentDTO updateDocument(String documentId, IcesiDocumentDTO icesiDocumentDTO) {

        var document = documentRepository.findById(icesiDocumentDTO.getIcesiDocumentId()).orElseThrow(createIcesiException(
                "Document not found",
                HttpStatus.NOT_FOUND,
                new DetailBuilder(ErrorCode.ERR_404, "Document", "Id", documentId)
        ));



        validateStatus(document.getStatus(),icesiDocumentDTO);
        validateTitle(icesiDocumentDTO);

        document.setText(icesiDocumentDTO.getText());
        document.setTitle(icesiDocumentDTO.getTitle());
        document.setStatus(icesiDocumentDTO.getStatus());

        return documentMapper.fromIcesiDocument(documentRepository.save(document));
    }

    private void validateStatus(IcesiDocumentStatus status,IcesiDocumentDTO icesiDocumentDTO){
        if(status == IcesiDocumentStatus.APPROVED ){
            throw createIcesiException(
                    "Try to update approved document, invalid action",
                    HttpStatus.BAD_REQUEST,
                    new DetailBuilder(ErrorCode.ERR_500, "Document", "Id", icesiDocumentDTO.getIcesiDocumentId())
            ).get();
        }
    }

    private void validateTitle(IcesiDocumentDTO icesiDocumentDTO){
        if(documentRepository.findByTitle(icesiDocumentDTO.getTitle()).isPresent()){
            throw createIcesiException(
                    "Title must be unique",
                    HttpStatus.NOT_ACCEPTABLE,
                    new DetailBuilder(ErrorCode.ERR_404, "Document", "Id", icesiDocumentDTO.getIcesiDocumentId())
            ).get();
        }
    }

    @Override
    public IcesiDocumentDTO createDocument(IcesiDocumentDTO icesiDocumentDTO) {
        validateTitle(icesiDocumentDTO);

        IcesiDocumentDTO opDto = Optional.of(icesiDocumentDTO).orElseThrow(
                createIcesiException(
                        "IcesiDocumentDTO null",
                        HttpStatus.NOT_FOUND,
                        new DetailBuilder(ErrorCode.ERR_404, "DTO", "null", icesiDocumentDTO.getUserId())
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
