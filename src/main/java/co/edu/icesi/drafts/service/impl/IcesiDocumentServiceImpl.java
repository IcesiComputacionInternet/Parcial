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
import org.mapstruct.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import java.util.UUID;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static co.edu.icesi.drafts.error.util.IcesiExceptionBuilder.createIcesiException;


@Component
class IcesiDocumentServiceImpl implements IcesiDocumentService {

    private final IcesiUserRepository userRepository;
    private final IcesiDocumentRepository documentRepository;
    private final IcesiDocumentMapper documentMapper;

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
        String documentTitle = icesiDocumentDTO.getTitle();
        IcesiDocument icesiDocument = documentRepository.findByTitle(documentTitle).orElseThrow(
                createIcesiException(
                        "Document not found",
                        HttpStatus.NOT_FOUND,
                        new DetailBuilder(ErrorCode.ERR_404, "Document", "Title", documentTitle)
                )
        );
        checkIfUsersAreDifferent(icesiDocument.getIcesiUser().getIcesiUserId(), icesiDocumentDTO.getUserId());
        checkIfTitleAndTextCouldBeUpdated(icesiDocument, icesiDocumentDTO);
        checkIfNewTitleExists(icesiDocumentDTO.getTitle());
        documentRepository.updateDocument(icesiDocumentDTO.getIcesiDocumentId().toString(), icesiDocumentDTO.getTitle(), icesiDocumentDTO.getText(), icesiDocumentDTO.getStatus());

        return getUpdatedDocument(documentTitle);
    }

    private void checkIfUsersAreDifferent(UUID icesiUserId1, UUID icesiUserId2){
        if(!icesiUserId1.equals(icesiUserId2)){
            createIcesiException("The user can not be updated",
                    HttpStatus.BAD_REQUEST,
                    new DetailBuilder(ErrorCode.ERR_400, "User", "can not be updated"));
        }
    }

    private IcesiDocumentDTO getUpdatedDocument(String documentTitle){
        IcesiDocument icesiDocument = documentRepository.findByTitle(documentTitle).orElseThrow(
                createIcesiException(
                        "The user that was just updated no longer exists.",
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        new DetailBuilder(ErrorCode.ERR_500, null)
                )
        );
        return documentMapper.fromIcesiDocument(icesiDocument);
    }

    private void checkIfTitleAndTextCouldBeUpdated(IcesiDocument icesiDocument, IcesiDocumentDTO icesiDocumentDTO){
        if(!icesiDocument.getStatus().equals(IcesiDocumentStatus.DRAFT) && !icesiDocument.getStatus().equals(IcesiDocumentStatus.REVISION)){
            if(!icesiDocument.getTitle().equals(icesiDocumentDTO.getTitle())){
                createIcesiException("The title can not be updated",
                        HttpStatus.BAD_REQUEST,
                        new DetailBuilder(ErrorCode.ERR_400, "Title", "can not be updated because the document status is "+icesiDocument.getStatus().toString()));
            }

            if(!icesiDocument.getText().equals(icesiDocumentDTO.getText())){
                createIcesiException("The text can not be updated",
                        HttpStatus.BAD_REQUEST,
                        new DetailBuilder(ErrorCode.ERR_400, "Text", "can not be updated because the document status is "+icesiDocument.getStatus().toString()));
            }
        }

    }

    private void checkIfNewTitleExists(String newTitle){
        if(Optional.ofNullable(documentRepository.findByTitle(newTitle)).isPresent()){
            createIcesiException("The title " + newTitle + " already existed",
                    HttpStatus.BAD_REQUEST,
                    new DetailBuilder(ErrorCode.ERR_DUPLICATED, "Document", "Title", newTitle));
        }
    }

    @Override
    public IcesiDocumentDTO createDocument(IcesiDocumentDTO icesiDocumentDTO) {
        checkIfUserIdInDTOIsNull(icesiDocumentDTO);
        checkIfTitleExists(icesiDocumentDTO.getTitle());
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

    private void checkIfUserIdInDTOIsNull(IcesiDocumentDTO icesiDocumentDTO){
        if (icesiDocumentDTO.getUserId() == null){
            throw createIcesiException(
                    "User id is null",
                    HttpStatus.BAD_REQUEST,
                    new DetailBuilder(ErrorCode.ERR_REQUIRED_FIELD, "userId")
            ).get();
        }
    }

    private void checkIfTitleExists(String title){
        if(documentRepository.findByTitle(title).isPresent()){
            throw createIcesiException(
                    "Title already exists",
                    HttpStatus.BAD_REQUEST,
                    new DetailBuilder(ErrorCode.ERR_DUPLICATED, "Document", "Title", title)
            ).get();
        }
    }
}
