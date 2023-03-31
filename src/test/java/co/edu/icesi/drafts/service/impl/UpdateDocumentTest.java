package co.edu.icesi.drafts.service.impl;

import co.edu.icesi.drafts.dto.IcesiDocumentDTO;
import co.edu.icesi.drafts.error.exception.IcesiException;
import co.edu.icesi.drafts.mapper.IcesiDocumentMapper;
import co.edu.icesi.drafts.mapper.IcesiDocumentMapperImpl;
import co.edu.icesi.drafts.model.IcesiDocument;
import co.edu.icesi.drafts.model.IcesiDocumentStatus;
import co.edu.icesi.drafts.model.IcesiUser;
import co.edu.icesi.drafts.repository.IcesiDocumentRepository;
import co.edu.icesi.drafts.repository.IcesiUserRepository;
import co.edu.icesi.drafts.service.IcesiDocumentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class UpdateDocumentTest {

    private IcesiDocumentService documentService;

    private IcesiDocumentRepository documentRepository;

    private IcesiUserRepository userRepository;

    private IcesiDocumentMapper documentMapper;


    @BeforeEach
    public void init() {
        documentRepository = mock(IcesiDocumentRepository.class);
        documentMapper = spy(IcesiDocumentMapperImpl.class);
        userRepository = mock(IcesiUserRepository.class);
        documentService = new IcesiDocumentServiceImpl(userRepository, documentRepository, documentMapper);
    }

    @Test
    public void TestUpdate_WhenDocumentIsOnApprovedCantBeModified(){
        IcesiDocument doc = defaultDoc();
        when(documentRepository.findById(any())).thenReturn(Optional.of(doc));
        when(userRepository.findById(any())).thenReturn(Optional.of(doc.getIcesiUser()));
        var errors= assertThrows(IcesiException.class, ()->{
            documentService.updateDocument("any uuid",defaultDocDTO());
        });
        var error=errors.getError().getDetails().get(0);
        assertEquals("ERR_500",error.getErrorCode());
        assertEquals("Oops, we ran into an error",error.getErrorMessage());
    }

    private IcesiDocument defaultDoc(){
        return IcesiDocument.builder()
                .status(IcesiDocumentStatus.APPROVED)
                .title("test")
                .text("isohel")
                .icesiUser(IcesiUser.builder()
                        .icesiUserId(UUID.randomUUID())
                        .email("chlorine")
                        .code("u wa wa wa wa")
                        .firstName("Isaac")
                        .lastName("Clark")
                        .phoneNumber("shadow the hedge hog is a * *  * * *  * * * ")
                        .build()
                )
                .build();
    }
    private IcesiDocumentDTO defaultDocDTO(){
        return IcesiDocumentDTO.builder()
                .status(IcesiDocumentStatus.APPROVED)
                .title("test")
                .text("isohel")
                .userId(UUID.randomUUID())
                .build();
    }
}
