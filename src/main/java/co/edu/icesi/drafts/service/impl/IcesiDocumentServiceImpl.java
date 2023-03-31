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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Stream;

import static co.edu.icesi.drafts.error.util.IcesiExceptionBuilder.createIcesiException;


@AllArgsConstructor
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
        List<IcesiErrorDetail> list = addErrorsOnAList(documentsDTO);
        if (list.isEmpty()){
            throw new IcesiException(
                    "Error to create a document",
                    IcesiError.builder()
                            .status(HttpStatus.BAD_REQUEST)
                            .details(list)
                            .build()
            );
        }

        List<IcesiDocument> documents = documentsDTO.stream()
                .map(d -> {
                    IcesiDocument document = documentMapper.fromIcesiDocumentDTO(d);
                    document.setIcesiUser(getUser(d));
                    return document;
                })
                .toList();

        documentRepository.saveAll(documents);

        return documents.stream()
                .map(documentMapper::fromIcesiDocument)
                .toList();
    }

    private List<IcesiErrorDetail> addErrorsOnAList(List<IcesiDocumentDTO> documentsDTO){
        List<IcesiErrorDetail> errorsList = new ArrayList<>(documentsDTO.stream()
                .filter(d -> documentRepository.findByTitle(d.getTitle()).isPresent())
                .map(d -> new IcesiErrorDetail(
                        "ERR_DUPLICATED",
                        String.format("resource Document with field Title: %s, already exists", d.getTitle())
                ))
                .toList());

        errorsList.addAll(documentsDTO.stream()
                .filter(d -> documentRepository.findById(d.getUserId()).isEmpty())
                .map(d -> new IcesiErrorDetail(
                        "ERR_404",
                        String.format("%s with %s: %s not found", "User", "Id", d.getUserId())
                ))
                .toList());

        return errorsList;
    }

    @Override
    public IcesiDocumentDTO updateDocument(String documentId, IcesiDocumentDTO icesiDocumentDTO) {
        var document = documentRepository.findById(icesiDocumentDTO.getIcesiDocumentId())
                .orElseThrow(
                        createIcesiException(
                                "Document not found to be update",
                                HttpStatus.NOT_FOUND,
                                new DetailBuilder(ErrorCode.ERR_404, "Document", "Id", documentId)
                        )
                );
        validateStatus(icesiDocumentDTO);
        validateExistTitle(icesiDocumentDTO.getTitle());
        IcesiDocument upatedDocument = documentMapper.fromIcesiDocumentDTO(icesiDocumentDTO);
        return documentMapper.fromIcesiDocument(documentRepository.save(upatedDocument));
    }

    private void validateStatus(IcesiDocumentDTO documentDTO){
        if(documentDTO.getStatus().equals(IcesiDocumentStatus.APPROVED)){
            throw createIcesiException(
                    "The document status isn't valid to be update",
                    HttpStatus.BAD_REQUEST,
                    new DetailBuilder(ErrorCode.ERR_500)).get();
        }
    }

    public void validateExistTitle(String documentTitle){
        documentRepository.findByTitle(documentTitle).ifPresent(err -> {
            throw createIcesiException(
                    "The title of this document already exists",
                    HttpStatus.BAD_REQUEST,
                    new DetailBuilder(ErrorCode.ERR_DUPLICATED, "Document", "Title", documentTitle)
            ).get();
        }
        );
    }

    private IcesiUser getUser(IcesiDocumentDTO userDTO) {
        return userRepository.findById(userDTO.getUserId())
                .orElseThrow(
                        createIcesiException(
                                "User not found",
                                HttpStatus.NOT_FOUND,
                                new DetailBuilder(ErrorCode.ERR_404, "User", "Id", userDTO.getUserId())
                        )
                );
    }

    @Override
    public IcesiDocumentDTO createDocument (IcesiDocumentDTO icesiDocumentDTO){
        validateExistTitle(icesiDocumentDTO.getTitle());
        findUserId(icesiDocumentDTO.getUserId());
        IcesiUser user = getUser(icesiDocumentDTO);
        var icesiDocument = documentMapper.fromIcesiDocumentDTO(icesiDocumentDTO);
        icesiDocument.setIcesiUser(user);
        return documentMapper.fromIcesiDocument(documentRepository.save(icesiDocument));
    }

    private void findUserId(UUID id){
        if(id==null){
            throw  createIcesiException(
                    "User not found",
                    HttpStatus.NOT_FOUND,
                    new DetailBuilder(ErrorCode.ERR_REQUIRED_FIELD, "userId")
            ).get();
        }
    }
}
