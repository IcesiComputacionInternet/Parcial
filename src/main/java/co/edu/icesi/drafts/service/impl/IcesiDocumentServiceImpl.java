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
import org.springframework.http.HttpStatus;

import java.util.List;
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

    /*
    Es necesario implementar el create documents, en caso de que haya problemas con un usuario
     que no exista o un titulo repetido deben enviarse todos los errores de todos los documentos que se intentaron crear.
     Si al menos uno falla no se debe crear ninguno! puedes guiarte con las pruebas.
     */
    @Override
    public List<IcesiDocumentDTO> createDocuments(List<IcesiDocumentDTO> documentsDTO) {
        List<IcesiUser> users = documentsDTO.stream()
                .map(IcesiDocumentDTO::getIcesiDocumentId)
                .map(user-> userRepository.findById(user).get())
                .toList();

        List<IcesiDocument> documents = documentsDTO.stream().map(doc-> documentMapper.fromIcesiDocumentDTO(doc)).toList();

        //documents.stream().forEach(doc->doc.setIcesiUser());

        documentRepository.saveAll(documents);
        return documents.stream().map(doc->documentMapper.fromIcesiDocument(doc)).toList();
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

        if(icesiDocument.getStatus()== IcesiDocumentStatus.DRAFT || icesiDocument.getStatus()==IcesiDocumentStatus.REVISION){
            validateUniqueTitle(icesiDocumentDTO.getTitle());

            icesiDocument.setTitle(icesiDocumentDTO.getTitle());
            icesiDocument.setText(icesiDocumentDTO.getText());
        }
        icesiDocument.setStatus(icesiDocumentDTO.getStatus());

        documentRepository.save(icesiDocument);
        return documentMapper.fromIcesiDocument(icesiDocument);
    }

    public void validateUniqueTitle(String title){
        if(documentRepository.findByTitle(title).isPresent()){
            throw new RuntimeException("The title of the document already exists");
        }
    }

    public void validateUpdateDocumentUser(UUID expectedUser, UUID actualUser) {
        if(!expectedUser.equals(actualUser)){
            throw new RuntimeException("User can't be updated when updating document");
        }
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
