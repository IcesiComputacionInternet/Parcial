package co.edu.icesi.drafts.service.impl;

import co.edu.icesi.drafts.controller.IcesiDocumentController;
import co.edu.icesi.drafts.dto.IcesiDocumentDTO;
import co.edu.icesi.drafts.error.exception.*;
import co.edu.icesi.drafts.error.util.IcesiExceptionBuilder;
import co.edu.icesi.drafts.mapper.IcesiDocumentMapper;
import co.edu.icesi.drafts.model.IcesiDocument;
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
        boolean areTitlesInUse = findRepeatTitle(documentsDTO);

        if(areTitlesInUse){
            throw new RuntimeException("There are some title that are in use");
        }

        List<IcesiDocument> icesiDocuments = documentsDTO
                .stream()
                .map(documentMapper::fromIcesiDocumentDTO)
                .map(documentRepository::save)
                .toList();
        icesiDocuments.forEach(document -> document.setIcesiUser(findUser(document.getIcesiDocumentId())));
        icesiDocuments.forEach(documentRepository::save);
        return icesiDocuments.stream().map(documentMapper::fromIcesiDocument).toList();
    }

    private boolean findRepeatTitle(List<IcesiDocumentDTO> documentDTOS){
        Optional<IcesiDocumentDTO> icesiDocuments = documentDTOS
                .stream()
                .filter(document -> documentRepository.findByTitle(document.getTitle())
                        .isPresent())
                .findAny();
        return icesiDocuments.isPresent();
    }

    private IcesiUser findUser(UUID userId){
        return userRepository.findById(userId).orElseThrow(() -> new RuntimeException("There are some users that doesn't exists"));
    }

    @Override
    public IcesiDocumentDTO updateDocument(String documentId, IcesiDocumentDTO icesiDocumentDTO) {
        return null;
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
