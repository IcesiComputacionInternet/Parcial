package co.edu.icesi.drafts.service.impl;

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
import org.mapstruct.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.*;

import static co.edu.icesi.drafts.error.util.IcesiExceptionBuilder.createIcesiError;
import static co.edu.icesi.drafts.error.util.IcesiExceptionBuilder.createIcesiException;
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


    @Override
    public List<IcesiDocumentDTO> createDocuments(List<IcesiDocumentDTO> documentsDTO) {
        List<IcesiErrorDetail> documentsErrors = verifyDocuments(documentsDTO);

        if (!documentsErrors.isEmpty()){
            createError(documentsErrors);
        }

        List<IcesiDocument> icesiDocuments = documentsDTO
                .stream()
                .map(documentMapper::fromIcesiDocumentDTO)
                .toList();

        icesiDocuments.forEach(document -> document.setIcesiUser(findUserById(document.getIcesiDocumentId())));
        documentRepository.saveAll(icesiDocuments);
        return icesiDocuments.stream().map(documentMapper::fromIcesiDocument).toList();
    }

    @Override
    public IcesiDocumentDTO updateDocument(String documentId, IcesiDocumentDTO icesiDocumentDTO) {
        var document = findDocumentById(UUID.fromString(documentId));
        verifyDocumentStatus(document);
        verifyTitleRepeat(icesiDocumentDTO.getTitle());
        document.setTitle(icesiDocumentDTO.getTitle());
        document.setText(icesiDocumentDTO.getText());
        return documentMapper.fromIcesiDocument(documentRepository.save(document));
    }

    private void verifyDocumentStatus(IcesiDocument document){
        if(document.getStatus() == IcesiDocumentStatus.APPROVED){
            throw new IcesiException(
                    "Document can't be updated because it has already been approved",
                    createIcesiError("", HttpStatus.CONFLICT,
                            new DetailBuilder(ErrorCode.ERR_400, "status is", document.getStatus()))
            );
        }
    }

    @Override
    public IcesiDocumentDTO createDocument(IcesiDocumentDTO icesiDocumentDTO) {
        verifyTitleRepeat(icesiDocumentDTO.getTitle());

        var userId = getUserId(icesiDocumentDTO.getUserId());
        var user = findUserById(userId);

        var icesiDocument = documentMapper.fromIcesiDocumentDTO(icesiDocumentDTO);
        icesiDocument.setIcesiUser(user);
        return documentMapper.fromIcesiDocument(documentRepository.save(icesiDocument));
    }

    private void verifyTitleRepeat(String title){
        var isTitleInUse = documentRepository.findByTitle(title).isPresent();

        if(isTitleInUse){
            throw new IcesiException(
                    "Title repeat",
                    createIcesiError("",
                            HttpStatus.CONFLICT,
                            new DetailBuilder(ErrorCode.ERR_DUPLICATED, "Document", "Title", title))
            );
        }
    }

    private void createError(List<IcesiErrorDetail> exceptionBuilders){
        throw new IcesiException(
                "Errors in the document creation process",
                IcesiError.builder().status(HttpStatus.BAD_REQUEST).details(exceptionBuilders).build()
        );
    }

    private List<IcesiErrorDetail> verifyDocuments(List<IcesiDocumentDTO> document){
        List<IcesiErrorDetail> errors = new ArrayList<>();

        document.forEach(documentInformation -> {
            try {
                verifyTitleRepeat(documentInformation.getTitle());
                findUserById(documentInformation.getUserId());
            }catch (IcesiException exception){
                errors.addAll(exception.getError().getDetails());
            }
        });
        return errors;
    }

    private UUID getUserId(UUID id){
        return Optional.ofNullable(id)
                .orElseThrow(
                        createIcesiException(
                                "User is null",
                                HttpStatus.NOT_FOUND,
                                new DetailBuilder(ErrorCode.ERR_REQUIRED_FIELD, "userId")
                        )
                );
    }

    private IcesiUser findUserById(UUID id){
        return userRepository.findById(id)
                .orElseThrow(
                        createIcesiException(
                                "User not found",
                                HttpStatus.NOT_FOUND,
                                new DetailBuilder(ErrorCode.ERR_404, "User", "Id", id)
                        )
                );
    }

    private IcesiDocument findDocumentById(UUID id){
        return documentRepository.findById(id)
                .orElseThrow(
                        createIcesiException(
                                "Document not found",
                                HttpStatus.NOT_FOUND,
                                new DetailBuilder(ErrorCode.ERR_404, "Document", "Id", id)
                        )
                );
    }
}
