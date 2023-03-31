package co.edu.icesi.drafts.service.impl;

import co.edu.icesi.drafts.controller.IcesiDocumentController;
import co.edu.icesi.drafts.dto.IcesiDocumentDTO;
import co.edu.icesi.drafts.error.exception.*;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

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
        List<IcesiErrorDetail> icesiErrorList = new ArrayList<>();
        IcesiError icesiError = new IcesiError();
        documentsDTO.stream()
                .peek(icesiDocumentDTO -> saveDocumentErrors(icesiDocumentDTO, icesiErrorList))
                .peek(icesiDocumentDTO -> saveUserErrors(icesiDocumentDTO.getUserId(), icesiErrorList))
                .toList();

        List<IcesiDocument> icesiDocuments;

        icesiDocuments = documentsDTO.stream().collect(
                ArrayList::new,
                (list, document) -> {
                        Optional<IcesiUser> user = userRepository.findById(document.getUserId());
                        user.ifPresent(icesiUser -> {
                            IcesiDocument icesiDocument = documentMapper.fromIcesiDocumentDTO(document);
                            icesiDocument.setIcesiUser(icesiUser);
                            list.add(icesiDocument);
                    });
                },ArrayList::addAll
        );
        if(!icesiErrorList.isEmpty()){
            icesiError.setDetails(icesiErrorList);
            throw new IcesiException("",icesiError);
        }

        documentRepository.saveAll(icesiDocuments);

        return icesiDocuments.stream()
                .map(documentMapper::fromIcesiDocument)
                .toList();
    }

    private void saveDocumentErrors(IcesiDocumentDTO icesiDocumentDTO, List<IcesiErrorDetail> icesiError){
        documentRepository.findByTitle(icesiDocumentDTO.getTitle()).ifPresent(document -> {
            icesiError.add(
                    new IcesiErrorDetail(
                            "ERR_DUPLICATED",
                            "resource Document with field Title: "+icesiDocumentDTO.getTitle()+", already exists"
                    ));
        });

    }

    private void saveUserErrors(UUID userId, List<IcesiErrorDetail> icesiError){
        userRepository.findById(userId).or(() ->{
            icesiError.add(
                    new IcesiErrorDetail(
                            "ERR_404",
                            "User with Id: "+userId+" not found"
                    ));
            return Optional.empty();
        });
    }
    private void validateUniqueDocumentTitle(IcesiDocumentDTO icesiDocumentDTO, List<IcesiErrorDetail> icesiError){

        documentRepository.findByTitle(icesiDocumentDTO.getTitle())
                .ifPresent(document -> {

                    throw createIcesiException(
                            "resource Document with field Title: "+icesiDocumentDTO.getTitle()+", already exists",
                            HttpStatus.NOT_FOUND,
                            new DetailBuilder(ErrorCode.ERR_DUPLICATED, "Document", "Title", icesiDocumentDTO.getTitle())).get();
                });
    }
    private void validateUserExists(UUID userId, List<IcesiErrorDetail> icesiError){
        userRepository.findById(userId).orElseThrow(
                createIcesiException(
                        "field User Id is required",
                        HttpStatus.BAD_REQUEST,
                        new DetailBuilder(ErrorCode.ERR_REQUIRED_FIELD, "userId")
                )
        );
    }

    @Override
    public IcesiDocumentDTO updateDocument(String documentId, IcesiDocumentDTO icesiDocumentDTO) {
        List<IcesiErrorDetail> icesiError = new ArrayList<>();
        IcesiDocument documentWithNewInfo = getDocumentById(documentId);
        IcesiDocument documentToUpdate = documentMapper.fromIcesiDocumentDTO(icesiDocumentDTO);

        validateDocumentState(icesiDocumentDTO);
        validateUniqueDocumentTitle(icesiDocumentDTO, icesiError);

        changeDocumentArguments(documentToUpdate, documentWithNewInfo);

        documentRepository.save(documentToUpdate);
        return documentMapper.fromIcesiDocument(documentToUpdate);
    }

    private void changeDocumentArguments(IcesiDocument documentToUpdate, IcesiDocument documentWithNewInfo){
        documentToUpdate.setText(documentWithNewInfo.getText());
        documentToUpdate.setTitle(documentWithNewInfo.getTitle());
    }

    private void validateDocumentState(IcesiDocumentDTO icesiDocumentDTO){
        if(icesiDocumentDTO.getStatus().equals(IcesiDocumentStatus.APPROVED)){
            throw new IcesiException("Document is already approved"
                    , IcesiError.builder()
                    .status(HttpStatus.BAD_REQUEST)
                    .build());
        }
    }

    private IcesiDocument getDocumentById(String documentId){
        return documentRepository.findById(UUID.fromString(documentId))
                .orElseThrow(
                        createIcesiException(
                                "Document not found",
                                HttpStatus.NOT_FOUND,
                                new DetailBuilder(ErrorCode.ERR_404, "Document", "Id", documentId)
                        )
                );
    }

    @Override
    public IcesiDocumentDTO createDocument(IcesiDocumentDTO icesiDocumentDTO) {

        IcesiUser user = userRepository.findById(validateUserIDNotNull(icesiDocumentDTO)).orElseThrow(
                createIcesiException(
                "User not found",
                HttpStatus.BAD_REQUEST,
                new DetailBuilder(ErrorCode.ERR_404, "User", "Id", icesiDocumentDTO.getUserId()))
        );

        validateUniqueDocumentTitle(icesiDocumentDTO, new ArrayList<>());

        var icesiDocument = documentMapper.fromIcesiDocumentDTO(icesiDocumentDTO);
        icesiDocument.setIcesiUser(user);
        return documentMapper.fromIcesiDocument(documentRepository.save(icesiDocument));
    }
    private UUID validateUserIDNotNull(IcesiDocumentDTO icesiDocumentDTO){
        return Optional.ofNullable(icesiDocumentDTO.getUserId()).orElseThrow(
                createIcesiException(
                        "field User Id is required",
                        HttpStatus.BAD_REQUEST,
                        new DetailBuilder(ErrorCode.ERR_REQUIRED_FIELD, "userId")
                )
        );
    }
}
