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

import java.util.*;

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


    @Override
    public List<IcesiDocumentDTO> createDocuments(List<IcesiDocumentDTO> documentsDTO) {

        Optional<IcesiError> error = Optional.ofNullable(validateErrors(documentsDTO));

        if(error.isPresent()){
            throw new IcesiException("Error Creating Documents",validateErrors(documentsDTO));
        }

        List<IcesiDocument> icesiDocuments = documentsDTO.stream()
                .map(documentDTO -> {
                    IcesiDocument document  = documentMapper.fromIcesiDocumentDTO(documentDTO);
                    document.setIcesiUser(findById(documentDTO.getUserId()));
                    return document;
                })
                .toList();

        documentRepository.saveAll(icesiDocuments);

        return icesiDocuments.stream()
                .map(documentMapper::fromIcesiDocument)
                .toList();
    }

    @Override
    public IcesiDocumentDTO updateDocument(String documentId, IcesiDocumentDTO icesiDocumentDTO) {

        var document = documentRepository.findById(icesiDocumentDTO.getIcesiDocumentId()).orElseThrow(createIcesiException(
                "Document not found",
                HttpStatus.NOT_FOUND,
                new DetailBuilder(ErrorCode.ERR_404, "Document", "Id", documentId)
        ));



        validateStatus(document.getStatus(),icesiDocumentDTO);
        validateTitle(icesiDocumentDTO);

        document.setText(icesiDocumentDTO.getText());
        document.setTitle(icesiDocumentDTO.getTitle());
        document.setStatus(icesiDocumentDTO.getStatus());

        return documentMapper.fromIcesiDocument(documentRepository.save(document));
    }

    private void validateStatus(IcesiDocumentStatus status,IcesiDocumentDTO icesiDocumentDTO){
        if(status == IcesiDocumentStatus.APPROVED ){
            throw createIcesiException(
                    "Try to update approved document, invalid action",
                    HttpStatus.BAD_REQUEST,
                    new DetailBuilder(ErrorCode.ERR_500, "Document", "Id", icesiDocumentDTO.getIcesiDocumentId())
            ).get();
        }
    }

    private void validateTitle(IcesiDocumentDTO icesiDocumentDTO){
        if(documentRepository.findByTitle(icesiDocumentDTO.getTitle()).isPresent()){
            throw createIcesiException(
                    "Title must be unique",
                    HttpStatus.NOT_ACCEPTABLE,
                    new DetailBuilder(ErrorCode.ERR_DUPLICATED, "Document", "Title", icesiDocumentDTO.getTitle())
            ).get();
        }
    }

    @Override
    public IcesiDocumentDTO createDocument(IcesiDocumentDTO icesiDocumentDTO) {
        validateTitle(icesiDocumentDTO);
        validateIcesiUserIdDtoIsNull(icesiDocumentDTO);

        IcesiUser user = findById(icesiDocumentDTO.getUserId());
        var icesiDocument = documentMapper.fromIcesiDocumentDTO(icesiDocumentDTO);
        icesiDocument.setIcesiUser(user);
        return documentMapper.fromIcesiDocument(documentRepository.save(icesiDocument));
    }

    private IcesiUser findById(UUID id){
        return userRepository.findById(id)
                .orElseThrow(
                        createIcesiException(
                                "User not found",
                                HttpStatus.NOT_FOUND,
                                new DetailBuilder(ErrorCode.ERR_404, "User", "Id", id)
                        )
                );
    }

    private void validateIcesiUserIdDtoIsNull(IcesiDocumentDTO icesiDocumentDTO){
        if(icesiDocumentDTO.getUserId() == null){
            throw createIcesiException(
                    "User not found",
                    HttpStatus.NOT_FOUND,
                    new DetailBuilder(ErrorCode.ERR_REQUIRED_FIELD, "userId", "Id", null)
            ).get();
        }
    }

    private IcesiError validateErrors(List<IcesiDocumentDTO> icesiDocumentDTO) {
        List<IcesiErrorDetail> errors = new ArrayList<>();
        icesiDocumentDTO.forEach(
                dto -> {
                    try {
                        validateTitle(dto);
                    } catch (IcesiException exception) {
                        errors.add(exception.getError().getDetails().get(0));
                    }
                    try {
                        findById(dto.getUserId());
                    } catch (IcesiException exception) {
                        errors.add(exception.getError().getDetails().get(0));
                    }
                }
        );

        if (errors.isEmpty()) {
            return null;
        }

        return new IcesiError(HttpStatus.BAD_REQUEST, errors);
    }
}
