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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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
        catchErrorsInCreateDocuments(documentsDTO);
        List<IcesiDocument> icesiDocuments = documentsDTO.stream()
                .map(icesiDocumentDTO -> {
                    IcesiDocument icesiDocument = documentMapper.fromIcesiDocumentDTO(icesiDocumentDTO);
                    icesiDocument.setIcesiUser(userRepository.findById(icesiDocumentDTO.getUserId()).get());
                    return icesiDocument;
                })
                .toList();
        return documentRepository.saveAll(icesiDocuments).stream().map(icesiDocument -> {
            return documentMapper.fromIcesiDocument(icesiDocument);
        }).collect(Collectors.toList());
    }

    @Override
    public IcesiDocumentDTO updateDocument(String documentId, IcesiDocumentDTO icesiDocumentDTO) {
        var document = catchExistsDocumentId(documentId);
        if(document.getStatus().equals(IcesiDocumentStatus.APPROVED)){
            throw createIcesiException(
                    "Documents is APPROVED",
                    HttpStatus.BAD_REQUEST,
                    new DetailBuilder(ErrorCode.ERR_400, "Document","Status", icesiDocumentDTO.getStatus())
            ).get();
        }
        if(!icesiDocumentDTO.getTitle().equals(document.getTitle())){
            catchExistsDocumentTitle(icesiDocumentDTO.getTitle());
            document.setTitle(icesiDocumentDTO.getTitle());
        }
        document.setText(icesiDocumentDTO.getText());
        return documentMapper.fromIcesiDocument(documentRepository.save(document));
    }

    @Override
    public IcesiDocumentDTO createDocument(IcesiDocumentDTO icesiDocumentDTO) {
        catchExistsDocumentTitle(icesiDocumentDTO.getTitle());
        if(icesiDocumentDTO.getUserId() == null){
            throw createIcesiException(
                    "User is null",
                    HttpStatus.BAD_REQUEST,
                    new DetailBuilder(ErrorCode.ERR_REQUIRED_FIELD, "userId")
            ).get();
        }

        var user = catchExistsUserId(icesiDocumentDTO.getUserId());

        var icesiDocument = documentMapper.fromIcesiDocumentDTO(icesiDocumentDTO);
        icesiDocument.setIcesiUser(user);
        return documentMapper.fromIcesiDocument(documentRepository.save(icesiDocument));
    }

    private void catchExistsDocumentTitle(String title){
        if(documentRepository.findByTitle(title).isPresent()){
            throw createIcesiException(
                    "Title already exists",
                    HttpStatus.BAD_REQUEST,
                    new DetailBuilder(ErrorCode.ERR_DUPLICATED,"Document","Title",title)
            ).get();
        }
    }
    private IcesiDocument catchExistsDocumentId(String documentId){
        return documentRepository.findById(documentId)
                .orElseThrow(
                        createIcesiException(
                                "Document not found",
                                HttpStatus.NOT_FOUND,
                                new DetailBuilder(ErrorCode.ERR_404,"Document","Id",documentId)
                        )
                );
    }
    private IcesiUser catchExistsUserId(UUID userId){
        return userRepository.findById(userId)
                .orElseThrow(
                        createIcesiException(
                                "User not found",
                                HttpStatus.NOT_FOUND,
                                new DetailBuilder(ErrorCode.ERR_404, "User", "Id", userId)
                        )
                );
    }
    private void catchErrorsInCreateDocuments(List<IcesiDocumentDTO> documentDTOS) {
        List<DetailBuilder> detailErrors = new ArrayList<>();
        detailErrors.addAll(documentDTOS.stream()
                .filter(documentDTO -> documentDTO.getUserId() == null)
                .map(documentDTO -> new DetailBuilder(ErrorCode.ERR_REQUIRED_FIELD,
                        "userId"))
                .collect(Collectors.toList()));
        detailErrors.addAll(documentDTOS.stream()
                .filter(documentDTO -> userRepository.findById(documentDTO.getUserId()).isEmpty())
                .map(documentDTO -> new DetailBuilder(ErrorCode.ERR_404,
                        "User",
                        "Id",
                        documentDTO.getUserId()))
                .collect(Collectors.toList()));
        detailErrors.addAll(documentDTOS.stream()
                .filter(documentDTO -> documentRepository.findByTitle(documentDTO.getTitle()).isPresent())
                .map(documentDTO -> new DetailBuilder(ErrorCode.ERR_DUPLICATED,
                        "Document",
                        "Title",
                        documentDTO.getTitle()))
                .collect(Collectors.toList()));
        if (!detailErrors.isEmpty()) {
            throw createIcesiException(
                    "Error",
                    HttpStatus.BAD_REQUEST,
                    detailErrors.stream().toArray(DetailBuilder[]::new)
            ).get();
        }
    }
}
