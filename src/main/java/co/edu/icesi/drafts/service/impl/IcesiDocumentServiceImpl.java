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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
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
        List<IcesiErrorDetail> errorDetails = validateErrors(documentsDTO);
        if (!errorDetails.isEmpty()){
            throw new IcesiException("Error creating documents",
                    IcesiError.builder()
                            .status(HttpStatus.BAD_REQUEST)
                            .details(errorDetails)
                            .build()

            );
        }
        List<IcesiDocument> documents = documentsDTO.stream().collect(
                ArrayList::new,
                (list, doc) ->{
                    IcesiDocument document = documentMapper.fromIcesiDocumentDTO(doc);
                    IcesiUser user = getUser(doc);
                    document.setIcesiUser(user);
                    list.add(document);

                },
                ArrayList::addAll
        );

        documentRepository.saveAll(documents);

        return documents.stream().map(documentMapper::fromIcesiDocument).collect(Collectors.toList());
    }

    @Override
    public IcesiDocumentDTO updateDocument(String documentId, IcesiDocumentDTO icesiDocumentDTO) {
        documentRepository.findById(UUID.fromString(documentId)).orElseThrow(
                createIcesiException("Document not found",
                        HttpStatus.NOT_FOUND,
                        new DetailBuilder(ErrorCode.ERR_404, "Document","Id", documentId)
                )
        );
        validateTitle(icesiDocumentDTO.getTitle());
        IcesiDocument upatedDocument = documentMapper.fromIcesiDocumentDTO(icesiDocumentDTO);
        validateStatus(upatedDocument);
        return documentMapper.fromIcesiDocument(documentRepository.save(upatedDocument));
    }

    @Override
    public IcesiDocumentDTO createDocument(IcesiDocumentDTO icesiDocumentDTO) {
        validateUserIdUser(icesiDocumentDTO.getUserId());
        validateTitle(icesiDocumentDTO.getTitle());
        IcesiUser user = getUser(icesiDocumentDTO);
        var icesiDocument = documentMapper.fromIcesiDocumentDTO(icesiDocumentDTO);
        icesiDocument.setIcesiUser(user);

        return documentMapper.fromIcesiDocument(documentRepository.save(icesiDocument));
    }

    private IcesiUser getUser(IcesiDocumentDTO icesiDocumentDTO) {
        return userRepository.findById(icesiDocumentDTO.getUserId())
                .orElseThrow(
                        createIcesiException(
                                "User not found",
                                HttpStatus.NOT_FOUND,
                                new DetailBuilder(ErrorCode.ERR_404, "User", "Id", icesiDocumentDTO.getUserId())
                        )
                );
    }

    private  void  validateTitle(String title){
     documentRepository.findByTitle(title).ifPresent(e -> {
       throw createIcesiException(
                 "Title already exist",
                 HttpStatus.INTERNAL_SERVER_ERROR,
                 new DetailBuilder(ErrorCode.ERR_DUPLICATED, "Document","Title",title)
         ).get();
    });

    }


    private void validateStatus(IcesiDocument document){
        if(!(document.getStatus().equals(IcesiDocumentStatus.DRAFT) || document.getStatus().equals(IcesiDocumentStatus.REVISION))){
            throw  new RuntimeException("The document: "+document.getIcesiDocumentId()+" can not be updated, it must have status DRAFT or REVISION");
        }
    }

    private void validateUserIdUser(UUID idUser){
        if(idUser == null){
           throw  createIcesiException(
                    "User not found",
                    HttpStatus.NOT_FOUND,
                    new DetailBuilder(ErrorCode.ERR_REQUIRED_FIELD, "userId")
            ).get();
        }
    }

    private List<IcesiErrorDetail> validateErrors(List<IcesiDocumentDTO> documentsDTO){
        List<IcesiErrorDetail> errors = new ArrayList<>();
       documentsDTO.forEach(
               icesiDocumentDTO -> {
                   try {
                       validateTitle(icesiDocumentDTO.getTitle());
                   }catch (IcesiException exception){
                       errors.add(exception.getError().getDetails().get(0));
                   }

                   try {
                       userRepository.findById(icesiDocumentDTO.getUserId())
                               .orElseThrow(
                                       createIcesiException(
                                               "User not found",
                                               HttpStatus.NOT_FOUND,
                                               new DetailBuilder(ErrorCode.ERR_404, "User", "Id", icesiDocumentDTO.getUserId())
                                       )
                               );
                   }catch (IcesiException exception){
                       errors.add(exception.getError().getDetails().get(0));
                   }

                   try {
                     validateUserIdUser(icesiDocumentDTO.getUserId());
                   }catch (IcesiException exception){
                       errors.add(exception.getError().getDetails().get(0));
                   }
               }
       );

        return errors;
    }


}
