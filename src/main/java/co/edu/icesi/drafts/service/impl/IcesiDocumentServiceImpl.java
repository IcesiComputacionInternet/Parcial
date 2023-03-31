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
        List<IcesiDocument> documents = documentsDTO.stream()
                .map(x -> documentMapper.fromIcesiDocumentDTO(x)).toList();

        assignUsersToDocuments(documentsDTO, documents);

        return documentRepository.saveAll(documents).stream()
                .map(x -> documentMapper.fromIcesiDocument(x)).toList();
    }

    private List<DetailBuilder> assignUsersToDocuments(List<IcesiDocumentDTO> documentsDTO, List<IcesiDocument> documents){
        List<DetailBuilder> details = new ArrayList<>();
        for(int i = 0; i < documentsDTO.size(); i++){
            Optional<IcesiUser> optionalIcesiUser = userRepository.findById(documentsDTO.get(i).getUserId());
            if (optionalIcesiUser.isPresent()){
                documents.get(i).setIcesiUser(optionalIcesiUser.get());
            }else {
                details.add(new DetailBuilder(ErrorCode.ERR_404, "User", "Id", documentsDTO.get(i).getUserId()));
            }
        }
        return details;
    }

    private void checkThatAllUsersExist(List<IcesiDocumentDTO> documentsDTO){
        List<DetailBuilder> details = new ArrayList<>();
        for(IcesiDocumentDTO icesiDocumentDTO: documentsDTO){
            if(!userRepository.findById(icesiDocumentDTO.getUserId()).isPresent()){
                details.add(new DetailBuilder(ErrorCode.ERR_404, "User", "Id", icesiDocumentDTO.getUserId()));
            };
        }
        if(!details.isEmpty()){
            throw createIcesiException(
                    "Some users do not exist",
                    HttpStatus.NOT_FOUND,
                    details.stream().toArray(DetailBuilder[]::new)
            ).get();
        }
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
            throw createIcesiException("The user can not be updated",
                    HttpStatus.BAD_REQUEST,
                    new DetailBuilder(ErrorCode.ERR_400, "User", "can not be updated")).get();
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
                throw createIcesiException("The title can not be updated",
                        HttpStatus.BAD_REQUEST,
                        new DetailBuilder(ErrorCode.ERR_400, "Title", "can not be updated because the document status is "+icesiDocument.getStatus().toString())).get();
            }

            if(!icesiDocument.getText().equals(icesiDocumentDTO.getText())){
                throw createIcesiException("The text can not be updated",
                        HttpStatus.BAD_REQUEST,
                        new DetailBuilder(ErrorCode.ERR_400, "Text", "can not be updated because the document status is "+icesiDocument.getStatus().toString())).get();
            }
        }

    }

    private void checkIfNewTitleExists(String newTitle){
        if(Optional.ofNullable(documentRepository.findByTitle(newTitle)).isPresent()){
            throw createIcesiException("The title " + newTitle + " already existed",
                    HttpStatus.BAD_REQUEST,
                    new DetailBuilder(ErrorCode.ERR_DUPLICATED, "Document", "Title", newTitle)).get();
        }
    }

    @Override
    public IcesiDocumentDTO createDocument(IcesiDocumentDTO icesiDocumentDTO) {
        checkIfUserIdInDTOIsNull(icesiDocumentDTO);
        checkIfTitleExists(icesiDocumentDTO.getTitle());
        var user = checkIfUserExists(icesiDocumentDTO.getUserId());
        var icesiDocument = documentMapper.fromIcesiDocumentDTO(icesiDocumentDTO);

        icesiDocument.setIcesiUser(user);
        return documentMapper.fromIcesiDocument(documentRepository.save(icesiDocument));
    }

    private IcesiUser checkIfUserExists(UUID userId){
        return userRepository.findById(userId)
                .orElseThrow(
                    createIcesiException(
                        "User not found",
                        HttpStatus.NOT_FOUND,
                        new DetailBuilder(ErrorCode.ERR_404, "User", "Id", userId)
                    )
                );
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
