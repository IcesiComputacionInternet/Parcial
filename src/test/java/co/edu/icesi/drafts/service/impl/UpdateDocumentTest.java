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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

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
        // Arrange
        var documentDTO = defaultDocumentDTO();
        var document = defaultDocument();
        var user = defaultUser();

        when(documentRepository.findByIcesiDocumentId("2dc074a1-2100-4d49-9823-aa12de103e70")).thenReturn(Optional.of(document));
        when(documentRepository.findByTitle("Some title")).thenReturn(Optional.of(document));
        when(userRepository.findById(any())).thenReturn(Optional.of(user));

        // Act
        var exception = assertThrows(IcesiException.class, () -> documentService.updateDocument("2dc074a1-2100-4d49-9823-aa12de103e70",documentDTO), "No exception was thrown");

        // Assert
        var error = exception.getError();
        var details = error.getDetails();
        assertEquals(1, details.size());
        var detail = details.get(0);
        assertEquals("ERR_400", detail.getErrorCode(), "Code doesn't match");
        assertEquals("field status is APPROVED", detail.getErrorMessage(), "Error message doesn't match");
    }

    private IcesiDocumentDTO defaultDocumentDTO() {
        return IcesiDocumentDTO.builder()
                .icesiDocumentId(UUID.fromString("2dc074a1-2100-4d49-9823-aa12de103e70"))
                .title("Some title")
                .text("loreipsum")
                .status(IcesiDocumentStatus.APPROVED)
                .userId(UUID.fromString("08a4db02-6625-40ee-b782-088add3a494f"))
                .build();
    }

    private IcesiDocument defaultDocument() {
        return IcesiDocument.builder()
                .icesiDocumentId(UUID.fromString("2dc074a1-2100-4d49-9823-aa12de103e70"))
                .title("Some title")
                .text("loreipsum")
                .status(IcesiDocumentStatus.APPROVED)
                .icesiUser(defaultUser())
                .build();
    }

    private IcesiUser defaultUser() {
        return IcesiUser.builder()
                .icesiUserId(UUID.fromString("08a4db02-6625-40ee-b782-088add3a494f"))
                .email("johndoe@email.com")
                .code("A00232323")
                .firstName("John")
                .lastName("Doe")
                .phoneNumber("+57 00000000")
                .build();
    }

}
