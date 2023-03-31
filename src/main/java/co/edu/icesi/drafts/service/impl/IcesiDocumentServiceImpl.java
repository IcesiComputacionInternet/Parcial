package co.edu.icesi.drafts.service.impl;

import co.edu.icesi.drafts.controller.IcesiDocumentController;
import co.edu.icesi.drafts.dto.IcesiDocumentDTO;
import co.edu.icesi.drafts.dto.UpdateDocumentDTO;
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
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        List<IcesiErrorDetail> errorDetails = new ArrayList<>();
        List<IcesiDocument> documentsToSave = new ArrayList<>();
        for (IcesiDocumentDTO documentDTO : documentsDTO) {
            Optional<IcesiDocument> documentFound = documentRepository.findByTitle(documentDTO.getTitle());
            if(documentFound.isPresent()){
                errorDetails.add(
                        createIcesiException(
                                "Document already exists",
                                HttpStatus.BAD_REQUEST,
                                new DetailBuilder(ErrorCode.ERR_DUPLICATED, "Document", "Title", documentDTO.getTitle())
                        ).get().getError().getDetails().get(0)
                );
            }
            Optional<IcesiUser> userFound = userRepository.findById(documentDTO.getUserId());
            if(userFound.isEmpty()){
                errorDetails.add(
                        createIcesiException(
                                "User not found",
                                HttpStatus.NOT_FOUND,
                                new DetailBuilder(ErrorCode.ERR_404, "User", "Id", documentDTO.getUserId())
                        ).get().getError().getDetails().get(0)
                );
            }
            var document = documentMapper.fromIcesiDocumentDTO(documentDTO);
            document.setIcesiUser(userFound.orElse(null));
            documentsToSave.add(document);
        }
        if(!errorDetails.isEmpty()){
            throw new IcesiException(
                    "Some documents could not be created",
                    IcesiError.builder()
                            .status(HttpStatus.BAD_REQUEST)
                            .details(errorDetails)
                            .build()
            );
        }

        return documentRepository.saveAll(documentsToSave).stream()
                .map(documentMapper::fromIcesiDocument)
                .toList();
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
        if(document.getStatus() == IcesiDocumentStatus.APPROVED){
            throw createIcesiException(
                    "Document already approved",
                    HttpStatus.BAD_REQUEST,
                    new DetailBuilder(ErrorCode.ERR_500, "Document", "Id", updateDocumentDTO.getID())
            ).get();
        }
        document.setTitle(updateDocumentDTO.getTitle());
        document.setText(updateDocumentDTO.getText());
        document.setStatus(updateDocumentDTO.getStatus());
        return documentMapper.fromIcesiDocument(documentRepository.save(document));
    }

    @Override
    public IcesiDocumentDTO createDocument(IcesiDocumentDTO icesiDocumentDTO) {
        if(icesiDocumentDTO.getUserId() == null){
            throw createIcesiException(
                    "User cant be null",
                    HttpStatus.NOT_FOUND,
                    new DetailBuilder(ErrorCode.ERR_REQUIRED_FIELD, "userId")
            ).get();
        }

        Optional<IcesiUser> userFound = userRepository.findById(icesiDocumentDTO.getUserId());
        if(userFound.isEmpty()){
            throw createIcesiException(
                    "User not found",
                    HttpStatus.NOT_FOUND,
                    new DetailBuilder(ErrorCode.ERR_404, "User", "Id", icesiDocumentDTO.getUserId())
            ).get();
        }
        Optional<IcesiDocument> documentFound = documentRepository.findByTitle(icesiDocumentDTO.getTitle());
        if(documentFound.isPresent()){
            throw createIcesiException(
                    "Document already exists",
                    HttpStatus.CONFLICT,
                    new DetailBuilder(ErrorCode.ERR_DUPLICATED, "Document", "Title", icesiDocumentDTO.getTitle())
            ).get();
        }
        var icesiDocument = documentMapper.fromIcesiDocumentDTO(icesiDocumentDTO);
        icesiDocument.setIcesiUser(userFound.get());
        return documentMapper.fromIcesiDocument(documentRepository.save(icesiDocument));
    }
}
