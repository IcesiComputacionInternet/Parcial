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
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.*;

import static co.edu.icesi.drafts.error.util.IcesiExceptionBuilder.createIcesiException;

@Service
@RequiredArgsConstructor
class IcesiDocumentServiceImpl implements IcesiDocumentService {


    private final IcesiUserRepository userRepository;
    private final IcesiDocumentRepository documentRepository;
    private final IcesiDocumentMapper documentMapper;


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

        //
        Optional<IcesiDocument> icesiDocumentOld = documentRepository.findById(UUID.fromString(documentId));
        //Ifs eliminated
        executeDocumentUpdateValidation(icesiDocumentOld, documentMapper.fromIcesiDocumentDTO(icesiDocumentDTO));
        //update
        IcesiDocument icesiDocumentNew = documentMapper.fromIcesiDocumentDTO(icesiDocumentDTO);
        return documentMapper.fromIcesiDocument(documentRepository.save(icesiDocumentNew));
        // Optional<IcesiDocument> icesiDocument = documentRepository.findById(UUID.fromString(documentId));
    }

    private void executeDocumentUpdateValidation(Optional<IcesiDocument> savedIcesiDocument, IcesiDocument newIcesiDocument){
        isDocumentPresent(savedIcesiDocument);
        verifyDocumentStatusIsUpdatable(savedIcesiDocument.get().getStatus());
        verifyDocumentsUserStillsTheSame(savedIcesiDocument.get(), newIcesiDocument);
        isDocumentTitleUnique(newIcesiDocument);
    }

    private void isDocumentPresent(Optional<IcesiDocument> icesiDocument){
        if(icesiDocument.isEmpty()){
            throw new RuntimeException("Requested document doesn't exists");
        }
    }

    private void verifyDocumentStatusIsUpdatable(IcesiDocumentStatus documentStatus){
        if(documentStatus != IcesiDocumentStatus.DRAFT && documentStatus != IcesiDocumentStatus.REVISION){
            throw new RuntimeException("Status approved. Can´t update");
        }
    }

    private void verifyDocumentsUserStillsTheSame(IcesiDocument savedIcesiDocument, IcesiDocument newIcesiDocument){
        if(!savedIcesiDocument.getIcesiUser().equals(newIcesiDocument.getIcesiUser())){
            throw new RuntimeException("Document's user has been changed");
        }
    }

    private void isDocumentTitleUnique(IcesiDocument newIcesiDocument){
        Optional<IcesiDocument> previousDocument = documentRepository.findByTitle(newIcesiDocument.getTitle());
        if(previousDocument.isPresent()){
            throw new RuntimeException("Already exists a document with the same title");
        }
    }

    @Override
    public IcesiDocumentDTO createDocument(IcesiDocumentDTO icesiDocumentDTO) {


        IcesiUser user = userRepository.findById(icesiDocumentDTO.getUserId())
                .orElseThrow(
                        createIcesiException(
                                "User not found",
                                HttpStatus.NOT_FOUND,
                                new DetailBuilder(ErrorCode.ERR_404, "User", "Id", icesiDocumentDTO.getUserId())
                        )
                );
        //the service works with the model classes
        IcesiDocument icesiDocument = documentMapper.fromIcesiDocumentDTO(icesiDocumentDTO);
        icesiDocument.setIcesiUser(user);
        return documentMapper.fromIcesiDocument(documentRepository.save(icesiDocument));
    }
}
