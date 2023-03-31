package co.edu.icesi.drafts.service.impl;

import co.edu.icesi.drafts.controller.IcesiDocumentController;
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
import static org.mockito.ArgumentMatchers.any;
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
        var documentDTO = defaultDocumentDTO();
        var document = defaultDocument();
        var documentId = "5ecd75ae-312f-48d0-9046-f26beeb16ecf";
        when(documentRepository.findById(any())).thenReturn(Optional.of(document));

        var exception = assertThrows(IcesiException.class, () -> documentService.updateDocument(documentId,  documentDTO));

        var error = exception.getError();
        var details = error.getDetails();
        assertEquals(1, details.size());
        var detail = details.get(0);
        assertEquals("ERR_400", detail.getErrorCode(), "Error code doesn't match");
        assertEquals("field status is APPROVED", detail.getErrorMessage(), "Error message doesn't match");
    }

    private IcesiDocumentDTO defaultDocumentDTO() {
        return IcesiDocumentDTO.builder()
                .icesiDocumentId(UUID.fromString("5ecd75ae-312f-48d0-9046-f26beeb16ecf"))
                .title("Some title")
                .text("Loreno Insomnio, nunca supe como se decía")
                .userId(UUID.fromString("a77e71c1-99d9-4804-be9b-e26cf4380981"))
                .build();
    }

    private IcesiDocument defaultDocument(){
        return IcesiDocument.builder()
                .icesiDocumentId(UUID.fromString("5ecd75ae-312f-48d0-9046-f26beeb16ecf"))
                .title("Some title")
                .title("Loreno Insomnio, nunca supe como se decía")
                .status(IcesiDocumentStatus.APPROVED)
                .icesiUser(defaultUser())
                .build();
    }

    private IcesiUser defaultUser(){
        return IcesiUser.builder()
                .icesiUserId(UUID.fromString("a77e71c1-99d9-4804-be9b-e26cf4380981"))
                .email("johndoe@email.com")
                .code("A00369982")
                .firstName("John")
                .lastName("Doe")
                .phoneNumber("+57 00000000")
                .build();
    }
}
