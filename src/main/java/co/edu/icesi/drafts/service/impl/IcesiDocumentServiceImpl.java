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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static co.edu.icesi.drafts.error.util.IcesiExceptionBuilder.createIcesiException;


@AllArgsConstructor
@Component
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
        return null;
    }

    @Override
    public IcesiDocumentDTO updateDocument(String documentId, IcesiDocumentDTO icesiDocumentDTO) {
        IcesiDocument icesiDocument = documentRepository.findByTitle(icesiDocumentDTO.getTitle()).orElseThrow();
        if(!icesiDocument.getIcesiUser().getIcesiUserId().equals(icesiDocumentDTO.getUserId())){
            createIcesiException("The user can not be updated",
                    HttpStatus.BAD_REQUEST,
                    new DetailBuilder(null, null));
        }
        checkIfTitleAndTextCouldBeUpdated(icesiDocument, icesiDocumentDTO);
        checkIfNewTitleExists(icesiDocumentDTO.getTitle());
        return documentMapper.fromIcesiDocument(documentRepository.updateDocument(icesiDocumentDTO.getIcesiDocumentId().toString(), icesiDocumentDTO.getTitle(), icesiDocumentDTO.getText(), icesiDocumentDTO.getStatus()));
    }

    private void checkIfTitleAndTextCouldBeUpdated(IcesiDocument icesiDocument, IcesiDocumentDTO icesiDocumentDTO){
        if(!icesiDocument.getStatus().equals(IcesiDocumentStatus.DRAFT) && !icesiDocument.getStatus().equals(IcesiDocumentStatus.REVISION)){
            if(!icesiDocument.getTitle().equals(icesiDocumentDTO.getTitle())){
                createIcesiException("The title can not be updated",
                        HttpStatus.BAD_REQUEST,
                        new DetailBuilder(null, null));
            }

            if(!icesiDocument.getText().equals(icesiDocumentDTO.getText())){
                createIcesiException("The text can not be updated",
                        HttpStatus.BAD_REQUEST,
                        new DetailBuilder(null, null));
            }
        }

    }

    private void checkIfNewTitleExists(String newTitle){
        if(Optional.ofNullable(documentRepository.findByTitle(newTitle)).isPresent()){
            createIcesiException("The title " + newTitle + " already existed",
                    HttpStatus.BAD_REQUEST,
                    new DetailBuilder(null, null));
        }
    }

    private void checkThatIcesiUserDoesNotChange(IcesiDocument icesiDocument){
        if(icesiDocument.getIcesiUser().getIcesiUserId().equals())
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
