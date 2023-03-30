package co.edu.icesi.drafts.service.impl;

import co.edu.icesi.drafts.controller.IcesiDocumentController;
import co.edu.icesi.drafts.dto.IcesiDocumentDTO;
import co.edu.icesi.drafts.dto.UpdateDocumentDTO;
import co.edu.icesi.drafts.error.exception.*;
import co.edu.icesi.drafts.error.util.IcesiExceptionBuilder;
import co.edu.icesi.drafts.mapper.IcesiDocumentMapper;
import co.edu.icesi.drafts.model.IcesiDocument;
import co.edu.icesi.drafts.model.IcesiUser;
import co.edu.icesi.drafts.repository.IcesiDocumentRepository;
import co.edu.icesi.drafts.repository.IcesiUserRepository;
import co.edu.icesi.drafts.service.IcesiDocumentService;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.*;

import static co.edu.icesi.drafts.error.util.IcesiExceptionBuilder.createIcesiException;

@Service
@AllArgsConstructor
public class IcesiDocumentServiceImpl implements IcesiDocumentService {

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
        List<IcesiDocumentDTO> documentsCreated = new ArrayList<>();
        List<IcesiError> errors = new ArrayList<>();
        List<String> noFoundUsers = new ArrayList<>();
        for (IcesiDocumentDTO documentDTO : documentsDTO) {
            try {
                documentsCreated.add(createDocument(documentDTO));
            } catch (IcesiException e) {
                errors.add(e.getError());
            }
        }
        if(errors.size() != 0){
            throw createIcesiException(
                    "Some documents could not be created",
                    HttpStatus.BAD_REQUEST,
                    new DetailBuilder(ErrorCode.ERR_400, "documents", "could not be created")
            ).get();
        }
        return documentsCreated;
    }

    @Override
    public IcesiDocumentDTO updateDocument(UpdateDocumentDTO updateDocumentDTO) {
        Optional<IcesiDocument> documentFound = documentRepository.findById(updateDocumentDTO.getID());
        if(documentFound.isEmpty()){
            throw createIcesiException(
                    "Document not found",
                    HttpStatus.NOT_FOUND,
                    new DetailBuilder(ErrorCode.ERR_404, "Document", "Id", updateDocumentDTO.getID())
            ).get();
        }

        IcesiDocument document = documentFound.get();
        document.setTitle(updateDocumentDTO.getTitle());
        document.setText(updateDocumentDTO.getText());


        return null;
    }

    @Override
    public IcesiDocumentDTO createDocument(IcesiDocumentDTO icesiDocumentDTO) {
        if(icesiDocumentDTO.getUserId() == null){
            throw createIcesiException(
                    "User not found",
                    HttpStatus.NOT_FOUND,
                    new DetailBuilder(ErrorCode.ERR_REQUIRED_FIELD, "userId", icesiDocumentDTO.getUserId())
            ).get();
        }
        var user = userRepository.findById(icesiDocumentDTO.getUserId())
                .orElseThrow(
                        createIcesiException(
                                "User not found",
                                HttpStatus.NOT_FOUND,
                                new DetailBuilder(ErrorCode.ERR_404, "User", "Id", icesiDocumentDTO.getUserId())
                        )
                );
        Optional<IcesiDocument> documentFound = documentRepository.findByTitle(icesiDocumentDTO.getTitle());
        if(documentFound.isPresent()){
            throw createIcesiException(
                    "Document already exists",
                    HttpStatus.CONFLICT,
                    new DetailBuilder(ErrorCode.ERR_DUPLICATED, "Document", "Title", icesiDocumentDTO.getTitle())
            ).get();
        }


        var icesiDocument = documentMapper.fromIcesiDocumentDTO(icesiDocumentDTO);
        icesiDocument.setIcesiUser(user);
        return documentMapper.fromIcesiDocument(documentRepository.save(icesiDocument));
    }
}
