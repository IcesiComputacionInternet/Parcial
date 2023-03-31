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
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.*;

import static co.edu.icesi.drafts.error.util.IcesiExceptionBuilder.createIcesiException;

@Service
@RequiredArgsConstructor
class IcesiDocumentServiceImpl implements IcesiDocumentService {


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
        return null;
    }

    @Override
    public IcesiDocumentDTO updateDocument(String documentId, IcesiDocumentDTO icesiDocumentDTO) {

        Optional<IcesiDocument> icesiDocument = documentRepository.findById(UUID.fromString(documentId));
        //document exists
        if(icesiDocument.isPresent()){

            //Status allows update
            if(icesiDocumentDTO.getStatus()== IcesiDocumentStatus.DRAFT || icesiDocumentDTO.getStatus()==IcesiDocumentStatus.REVISION){

                //User not changed
                IcesiDocumentDTO icesiDocumentDTOFound = documentMapper.fromIcesiDocument(icesiDocument.get());
                if(icesiDocumentDTOFound.getUserId()==icesiDocumentDTO.getUserId()){

                    //Unique title
                    Optional<IcesiDocument> icesiDocumentTitle = documentRepository.findByTitle(icesiDocumentDTO.getTitle());
                    if(icesiDocumentTitle.isPresent()){
                        //throw new
                    }else{
                        IcesiDocument icesiDocumentNew = documentMapper.fromIcesiDocumentDTO(icesiDocumentDTO);
                        return documentMapper.fromIcesiDocument(documentRepository.save(icesiDocumentNew));
                    }
                }else{
                    //throw new RuntimeException("Status approved. Can´t update");
                }
               // throw new IcesiException("ERROR", new IcesiError(HttpStatus.BAD_REQUEST, new IcesiErrorDetail("CODE-01", "AYUDA")));
            }else{
                //throw new RuntimeException("Status approved. Can´t update");
            }
        }else{
            //throw new RuntimeException("Status approved. Can´t update");
        }
        //
        // Optional<IcesiDocument> icesiDocument = documentRepository.findById(UUID.fromString(documentId));

        return null;
    }

    @Override
    public IcesiDocumentDTO createDocument(IcesiDocumentDTO icesiDocumentDTO) {


        IcesiUser user = userRepository.findById(icesiDocumentDTO.getUserId())
                .orElseThrow(
                        createIcesiException(
                                "User not found",
                                HttpStatus.NOT_FOUND,
                                new DetailBuilder(ErrorCode.ERR_404, "User", "Id", icesiDocumentDTO.getUserId())
                        )
                );
        //the service works with the model classes
        IcesiDocument icesiDocument = documentMapper.fromIcesiDocumentDTO(icesiDocumentDTO);
        icesiDocument.setIcesiUser(user);
        return documentMapper.fromIcesiDocument(documentRepository.save(icesiDocument));
    }
}
