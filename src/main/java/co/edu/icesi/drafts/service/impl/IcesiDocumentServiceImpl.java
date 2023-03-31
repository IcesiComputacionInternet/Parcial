package co.edu.icesi.drafts.service.impl;

import co.edu.icesi.drafts.dto.IcesiDocumentDTO;
import co.edu.icesi.drafts.error.exception.*;
import co.edu.icesi.drafts.mapper.IcesiDocumentMapper;
import co.edu.icesi.drafts.model.IcesiDocument;
import co.edu.icesi.drafts.repository.IcesiDocumentRepository;
import co.edu.icesi.drafts.repository.IcesiUserRepository;
import co.edu.icesi.drafts.service.IcesiDocumentService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Supplier;

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

        List<IcesiErrorDetail> errors = new ArrayList<>(documentsDTO.stream()
                .filter(documentDTO -> documentRepository.findByTitle(documentDTO.getTitle()).isPresent())
                .map(documentDTO -> new IcesiErrorDetail
                        ("ERR_DUPLICATED",
                                "resource Document with field Title: "+documentDTO.getTitle()+", already exists"))
                .toList());

        errors.addAll(documentsDTO.stream()
                .filter(documentDTO -> userRepository.findById(documentDTO.getUserId()).isEmpty())
                .map(documentDTO-> new IcesiErrorDetail(
                        "ERR_404", "User with Id: " +documentDTO.getUserId()+ " not found"))
                .toList());

        if(!errors.isEmpty()){
            throw new IcesiException(
                    "Bad Request",
                    IcesiError.builder().status(HttpStatus.BAD_REQUEST).details(errors).build()
            );
        }

        List<IcesiDocument> documents = documentsDTO.stream()
                .map(documentDTO -> {
                    var user = userRepository.findById(documentDTO.getUserId());
                    var document = documentMapper.fromIcesiDocumentDTO(documentDTO);
                    document.setIcesiUser(user.get());
                    return document;
                })
                .toList();

        documentRepository.saveAll(documents);

        return documents.stream()
                .map(documentMapper::fromIcesiDocument)
                .toList();
    }

    @Override
    public IcesiDocumentDTO updateDocument(String documentId, IcesiDocumentDTO icesiDocumentDTO) {
        documentRepository.getTypeofDocument(documentId)
                .orElseThrow(()-> new RuntimeException("Invalid Type of Document"));
        return createDocument(icesiDocumentDTO);
    }

    @Override
    public IcesiDocumentDTO createDocument(IcesiDocumentDTO icesiDocumentDTO) {

        validateTitle(icesiDocumentDTO.getTitle());
        UUID userId = validateUserIDNotNull(icesiDocumentDTO);

        var user = userRepository.findById(icesiDocumentDTO.getUserId())
                .orElseThrow(
                        createIcesiException(
                                "User not found",
                                HttpStatus.NOT_FOUND,
                                new DetailBuilder(ErrorCode.ERR_404, "User", "Id", userId)
                        )
                );
        var icesiDocument = documentMapper.fromIcesiDocumentDTO(icesiDocumentDTO);
        icesiDocument.setIcesiUser(user);
        return documentMapper.fromIcesiDocument(documentRepository.save(icesiDocument));
    }


    private void validateTitle(String title){
        documentRepository.findByTitle(title)
                .ifPresent(document -> {
                    throw createIcesiException(
                            "resource Document with field Title: "+title+", already exists",
                            HttpStatus.NOT_FOUND,
                            new DetailBuilder(ErrorCode.ERR_DUPLICATED, "Document", "Title", title)).get();
                });
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