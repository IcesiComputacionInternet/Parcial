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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static co.edu.icesi.drafts.error.util.IcesiExceptionBuilder.createIcesiException;
import static java.util.stream.Collectors.toList;


@Service
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

    /*
    Es necesario implementar el create documents, en caso de que haya problemas con un usuario
     que no exista o un titulo repetido deben enviarse todos los errores de todos los documentos que se intentaron crear.
     Si al menos uno falla no se debe crear ninguno! puedes guiarte con las pruebas.
     */
    @Override
    public List<IcesiDocumentDTO> createDocuments(List<IcesiDocumentDTO> documentsDTO) {
        List<DetailBuilder> details= new ArrayList<>();


        List<IcesiUser> users = documentsDTO.stream()
                .map(IcesiDocumentDTO::getUserId)
                .map(user-> userRepository.findById(user).orElseGet(
                        ()->{details.add(
                                new DetailBuilder(ErrorCode.ERR_404, "User", "Id",user )
                        );
                            return null;
                        }
                )).toList();

        documentsDTO.stream()
                 .map(IcesiDocumentDTO::getTitle)
                .forEach(title-> documentRepository.findByTitle(title).ifPresent(
                        doc->details.add(
                                new DetailBuilder(ErrorCode.ERR_DUPLICATED, "Document", "Title",title)
                        )
                ));

         if(!details.isEmpty()){
             throw createIcesiException(
                     "Can't create documents",
                     HttpStatus.NOT_FOUND,
                     details.toArray(DetailBuilder[]::new)
             ).get();
         }

        List<IcesiDocument> documents = documentsDTO.stream()
                .map(documentMapper::fromIcesiDocumentDTO)
                .toList();



        users.forEach(user-> documents
                .forEach(doc-> documentsDTO.forEach(
                        docDTO-> {
                            if(docDTO.getUserId().equals(user.getIcesiUserId()) && docDTO.getIcesiDocumentId().equals(doc.getIcesiDocumentId())) doc.setIcesiUser(user);
                        }
                ))
        );

        documentRepository.saveAll(documents);
        return documents.stream().map(documentMapper::fromIcesiDocument).toList();
    }



    /*
    Es necesario implementar la funcion y prueba de update,
    hay que tener en cuenta que no se puede actualizar el usuario y el texto y titulo solo se pueden modificar
    si el documento se encuentra en estado de DRAFT o REVISION. Recuerda que el titulo debe ser unico!
     */
    @Override
    public IcesiDocumentDTO updateDocument(String documentId, IcesiDocumentDTO icesiDocumentDTO) {
        IcesiDocument icesiDocument= documentRepository.findById(UUID.fromString(documentId)).orElseThrow(()->new RuntimeException("Document not found"));
        IcesiUser icesiUser= icesiDocument.getIcesiUser();
        validateUpdateDocumentUser(icesiUser.getIcesiUserId(),icesiDocumentDTO.getUserId());
        if(icesiDocument.getStatus()== IcesiDocumentStatus.APPROVED){
            throw new RuntimeException("Document can't be updated. Is on approved");
        }

        validateUniqueTitle(icesiDocumentDTO.getTitle());

        icesiDocument.setTitle(icesiDocumentDTO.getTitle());
        icesiDocument.setText(icesiDocumentDTO.getText());

        icesiDocument.setStatus(icesiDocumentDTO.getStatus());

        documentRepository.save(icesiDocument);
        return documentMapper.fromIcesiDocument(icesiDocument);
    }

    public void validateUniqueTitle(String title){
        if(documentRepository.findByTitle(title).isPresent()){
            throw createIcesiException(
                    "Title already exists",
                    HttpStatus.NOT_FOUND,
                    new DetailBuilder(ErrorCode.ERR_DUPLICATED, "Document", "Title",title)
            ).get();
        }
    }

    public void validateUpdateDocumentUser(UUID expectedUser, UUID actualUser) {
        if(!expectedUser.equals(actualUser)){
            throw new RuntimeException("User can't be updated when updating document");
        }
    }
    @Override
    public IcesiDocumentDTO createDocument(IcesiDocumentDTO icesiDocumentDTO) {

        var user = userRepository.findById( userIdNotNull(icesiDocumentDTO.getUserId()))

                .orElseThrow(
                        createIcesiException(
                                "User not found",
                                HttpStatus.NOT_FOUND,
                                new DetailBuilder(ErrorCode.ERR_404, "User", "Id", icesiDocumentDTO.getUserId())
                        )
                );
        validateUniqueTitle(icesiDocumentDTO.getTitle());
        var icesiDocument = documentMapper.fromIcesiDocumentDTO(icesiDocumentDTO);

        icesiDocument.setIcesiUser(user);
        return documentMapper.fromIcesiDocument(documentRepository.save(icesiDocument));
    }

    public UUID userIdNotNull(UUID userId){
        return   Optional.ofNullable(userId).orElseThrow(
                createIcesiException(
                        "User not given",
                        HttpStatus.NOT_FOUND,
                        new DetailBuilder(ErrorCode.ERR_REQUIRED_FIELD, "userId")
                )
        );
    }
}
