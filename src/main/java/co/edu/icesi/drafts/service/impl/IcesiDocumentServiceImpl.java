package co.edu.icesi.drafts.service.impl;

import co.edu.icesi.drafts.controller.IcesiDocumentController;
import co.edu.icesi.drafts.dto.IcesiDocumentDTO;
import co.edu.icesi.drafts.error.exception.*;
import co.edu.icesi.drafts.error.util.IcesiExceptionBuilder;
import co.edu.icesi.drafts.mapper.IcesiDocumentMapper;
import co.edu.icesi.drafts.model.IcesiDocument;
import co.edu.icesi.drafts.repository.IcesiDocumentRepository;
import co.edu.icesi.drafts.repository.IcesiUserRepository;
import co.edu.icesi.drafts.service.IcesiDocumentService;
import lombok.AllArgsConstructor;
import net.bytebuddy.implementation.bytecode.Throw;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static co.edu.icesi.drafts.error.util.IcesiExceptionBuilder.createIcesiException;


@AllArgsConstructor
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


    //TODO implementar
    @Override
    public List<IcesiDocumentDTO> createDocuments(List<IcesiDocumentDTO> documentsDTO) {
        List<String> titulosRepetidos= new ArrayList<>();
        documentsDTO.forEach((IcesiDocumentDTO doc)-> documentRepository.findByTitle(doc.getTitle())
                .ifPresent(
                        icesiDocument -> {
                            titulosRepetidos.add(icesiDocument.getTitle());
                            }
                        )
                );


        documentsDTO.forEach((IcesiDocumentDTO doc)-> userRepository.findById(doc.getUserId())
                .orElseThrow(
                        createIcesiException(
                                "User not found",
                                HttpStatus.NOT_FOUND,
                                new DetailBuilder(ErrorCode.ERR_404, "User", "Id", doc.getUserId())
                        )
                )
        );
        List<IcesiDocument> icesiDocuments = new ArrayList<>();
        documentsDTO.forEach((IcesiDocumentDTO doc)-> icesiDocuments.add(
                documentMapper.fromIcesiDocumentDTO(doc)));
        List<IcesiDocumentDTO> icesiDocumentDTOS = new ArrayList<>();
        icesiDocuments.forEach((IcesiDocument doc)-> icesiDocumentDTOS.add(
                documentMapper.fromIcesiDocument(documentRepository.save(doc))));
        return icesiDocumentDTOS;
    }

    @Override
    public IcesiDocumentDTO updateDocument(String documentId, IcesiDocumentDTO icesiDocumentDTO) {
        var document = documentRepository.findById(UUID.fromString(documentId)).orElseThrow(createIcesiException(
                "Document not found",
                HttpStatus.NOT_FOUND,
                new DetailBuilder(ErrorCode.ERR_404, "id",documentId)
        ));
        var user = userRepository.findById(icesiDocumentDTO.getUserId())
                .orElseThrow(
                        createIcesiException(
                                "User not found",
                                HttpStatus.NOT_FOUND,
                                new DetailBuilder(ErrorCode.ERR_404, "User", "Id", icesiDocumentDTO.getUserId())
                        )
                );
        if(!document.getText().equals(icesiDocumentDTO.getText())){
            throw new RuntimeException("No se editar el texto");
        }

        if(document.getIcesiUser().getIcesiUserId().equals(user.getIcesiUserId())){
            throw new RuntimeException("No se editar el usuario");
        }

        if(!document.getStatus().equals("DRAFT")||!document.getStatus().equals("REVISION")){
            throw new RuntimeException("No se puede modificar titulo");
        }
        documentRepository.delete(document);
        var icesiDocument = documentMapper.fromIcesiDocumentDTO(icesiDocumentDTO);
        icesiDocument.setIcesiUser(user);
        return documentMapper.fromIcesiDocument(documentRepository.save(icesiDocument));
    }


    //TODO aca hay un bug
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

        documentRepository.findByTitle(icesiDocumentDTO.getTitle())
                .ifPresent(doc ->{ throw new RuntimeException("ERR_DUPLICATED");
                });


        var icesiDocument = documentMapper.fromIcesiDocumentDTO(icesiDocumentDTO);
        icesiDocument.setIcesiUser(user);
        return documentMapper.fromIcesiDocument(documentRepository.save(icesiDocument));
    }
}
