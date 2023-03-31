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
import net.bytebuddy.implementation.bytecode.Throw;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.*;

import static co.edu.icesi.drafts.error.util.IcesiExceptionBuilder.createIcesiException;
import static java.util.UUID.fromString;



@Service
public class IcesiDocumentServiceImpl implements IcesiDocumentService {


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


    //TODO implementar
    @Override
    public List<IcesiDocumentDTO> createDocuments(List<IcesiDocumentDTO> documentsDTO) {
        List<IcesiErrorDetail> errorDetails = new ArrayList<>(documentsDTO.stream().filter(
                doc -> documentRepository.findByTitle(doc.getTitle()).isPresent()
        ).map(
                doc -> new IcesiErrorDetail(
                        "ERR_DUPLICATED",
                        "resource %s with field Title: %s, already exists".formatted("Document", doc.getTitle())
                )
        ).toList());
        errorDetails.addAll(documentsDTO.stream().filter(
                doc->userRepository.findById(doc.getUserId()).isEmpty()
        ).map(
                doc-> new IcesiErrorDetail(
                        "ERR_404",
                        "%s with %s: %s not found".formatted("User","Id",doc.getUserId())
                )
        ).toList());

        if(!errorDetails.isEmpty()){
            throw new IcesiException(
                    "Errors",
                    IcesiError.builder()
                            .status(HttpStatus.BAD_REQUEST)
                            .details(errorDetails)
                            .build()
            );
        }

        List<IcesiDocument> documents = documentsDTO.stream()
                .map( (doc) ->{
                            var document =documentMapper.fromIcesiDocumentDTO(doc);
                            document.setIcesiUser(userRepository.findById(doc.getUserId()).orElseThrow(
                                    createIcesiException(
                                            "Document not found",
                                            HttpStatus.NOT_FOUND,
                                            new DetailBuilder(ErrorCode.ERR_404, "userId","id",doc.getUserId())
                                    ))
                            );
                            return document;
                        }
                ).toList();
        documentRepository.saveAll(documents);
        return documents.stream().map((doc)->documentMapper.fromIcesiDocument(doc)).toList();
    }

    @Override
    public IcesiDocumentDTO updateDocument(String documentId, IcesiDocumentDTO icesiDocumentDTO) {
        var document = documentRepository.findById(icesiDocumentDTO.getIcesiDocumentId())
                .orElseThrow(
                        createIcesiException(
                        "Document not found",
                        HttpStatus.NOT_FOUND,
                        new DetailBuilder(ErrorCode.ERR_404, "document","id",documentId)
                    )
                );

        var user = userRepository.findById(icesiDocumentDTO.getUserId())
                .orElseThrow(
                        createIcesiException(
                                "User not found",
                                HttpStatus.NOT_FOUND,
                                new DetailBuilder(ErrorCode.ERR_404, "User", "Id", icesiDocumentDTO.getUserId())
                        )
                );

        if(icesiDocumentDTO.getStatus()== IcesiDocumentStatus.APPROVED || icesiDocumentDTO.getStatus() == IcesiDocumentStatus.DRAFT){
            throw createIcesiException(
                    "You cannot change the text",
                    HttpStatus.BAD_REQUEST,
                    new DetailBuilder(ErrorCode.ERR_500)

            ).get();
        }

        if(document.getIcesiUser()!=user){
            throw createIcesiException(
                    "You cannot change the user",
                    HttpStatus.BAD_REQUEST,
                    new DetailBuilder(ErrorCode.ERR_500)

            ).get();
        }

        documentRepository.findByTitle(icesiDocumentDTO.getTitle()).ifPresent(
                (doc)->{
                    throw createIcesiException(
                           "There's a document with this title already present",
                           HttpStatus.BAD_REQUEST,
                           new DetailBuilder(ErrorCode.ERR_REQUIRED_FIELD, "document","title",icesiDocumentDTO.getTitle())
                    ).get();
                });



        document.setTitle(icesiDocumentDTO.getTitle());
        document.setText(icesiDocumentDTO.getText());
        return documentMapper.fromIcesiDocument(documentRepository.save(document));
    }


    //TODO aca hay un bug
    @Override
    public IcesiDocumentDTO createDocument(IcesiDocumentDTO icesiDocumentDTO) {


        Optional.ofNullable(icesiDocumentDTO.getUserId()).orElseThrow(
                createIcesiException(
                        "User not found",
                        HttpStatus.NOT_FOUND,
                        new DetailBuilder(ErrorCode.ERR_REQUIRED_FIELD, "userId",  icesiDocumentDTO.getUserId())
                )
        );

        var user = userRepository.findById(icesiDocumentDTO.getUserId())
                .orElseThrow(
                        createIcesiException(
                                "User not found",
                                HttpStatus.NOT_FOUND,
                                new DetailBuilder(ErrorCode.ERR_404, "User", "Id", icesiDocumentDTO.getUserId())
                        )
                );



        documentRepository.findByTitle(icesiDocumentDTO.getTitle())
                .ifPresent(doc ->{ throw
                        createIcesiException(
                                "User not found",
                                HttpStatus.NOT_FOUND,
                                new DetailBuilder(ErrorCode.ERR_DUPLICATED, "Document", "Title", icesiDocumentDTO.getTitle())
                        ).get();
                });


        var icesiDocument = documentMapper.fromIcesiDocumentDTO(icesiDocumentDTO);
        icesiDocument.setIcesiUser(user);
        return documentMapper.fromIcesiDocument(documentRepository.save(icesiDocument));
    }
}
