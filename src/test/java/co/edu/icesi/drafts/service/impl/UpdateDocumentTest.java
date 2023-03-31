package co.edu.icesi.drafts.service.impl;

import co.edu.icesi.drafts.dto.IcesiDocumentDTO;
import co.edu.icesi.drafts.error.exception.IcesiException;
import co.edu.icesi.drafts.mapper.IcesiDocumentMapper;
import co.edu.icesi.drafts.mapper.impl.IcesiDocumentMapperImpl;
import co.edu.icesi.drafts.model.IcesiDocument;
import co.edu.icesi.drafts.model.IcesiDocumentStatus;
import co.edu.icesi.drafts.model.IcesiUser;
import co.edu.icesi.drafts.repository.IcesiDocumentRepository;
import co.edu.icesi.drafts.repository.IcesiUserRepository;
import co.edu.icesi.drafts.service.IcesiDocumentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    @DisplayName("When try to update an approved document throws an error")
    public void TestUpdate_WhenDocumentIsOnApprovedCantBeModified() {
        var document = defaultDocument();
        document.setStatus(IcesiDocumentStatus.APPROVED);
        var documentDTO = defaultDocumentDTO();
        documentDTO.setTitle("Title Changed");
        when(documentRepository.findById(any())).thenReturn(Optional.of(document));

        // Act
        var exception = assertThrows(IcesiException.class, () -> documentService.updateDocument(document.getIcesiDocumentId().toString(), documentDTO), "No exception was thrown");

        // Assert
        var error = exception.getError();
        var details = error.getDetails();
        assertEquals(1, details.size());
        var detail = details.get(0);
        assertEquals("ERR_404", detail.getErrorCode(), "Code doesn't match");
        assertEquals("CAN'T MODIFY AN APPROVED DOCUMENT", detail.getErrorMessage(), "Error message doesn't match");
    }

    @Test
    @DisplayName("When try to update the user field throws an error")
    public void TestUpdate_WhenUserIsBeenModified_ShouldThrowException() {
        var document = defaultDocument();
        document.setStatus(IcesiDocumentStatus.DRAFT);
        var documentDTO = defaultDocumentDTO();
        documentDTO.setUserId(UUID.randomUUID());
        when(documentRepository.findById(any())).thenReturn(Optional.of(document));

        // Act
        var exception = assertThrows(IcesiException.class, () -> documentService.updateDocument(document.getIcesiDocumentId().toString(), documentDTO), "No exception was thrown");

        // Assert
        var error = exception.getError();
        var details = error.getDetails();
        assertEquals(1, details.size());
        var detail = details.get(0);
        assertEquals("ERR_404", detail.getErrorCode(), "Code doesn't match");
        assertEquals("THE USER CAN'T BE CHANGED", detail.getErrorMessage(), "Error message doesn't match");
    }

    @Test
    @DisplayName("When try to set a new title that is already in use throws an error")
    public void TestUpdate_WhenModifyToAnExistingTitle_ShouldThrowException() {
        var document = defaultDocument();
        document.setStatus(IcesiDocumentStatus.DRAFT);
        var documentDTO = defaultDocumentDTO();
        documentDTO.setTitle("Title DTO");
        when(documentRepository.findById(any())).thenReturn(Optional.of(document));
        when(documentRepository.findByTitle(documentDTO.getTitle())).thenReturn(Optional.of(defaultDocument()));

        // Act
        var exception = assertThrows(IcesiException.class, () -> documentService.updateDocument(document.getIcesiDocumentId().toString(), documentDTO), "No exception was thrown");

        // Assert
        var error = exception.getError();
        var details = error.getDetails();
        assertEquals(1, details.size());
        var detail = details.get(0);
        assertEquals("ERR_DUPLICATED", detail.getErrorCode(), "Code doesn't match");
        assertEquals("resource Document with field Title: " + documentDTO.getTitle() +", already exists", detail.getErrorMessage(), "Error message doesn't match");
    }

    private IcesiDocumentDTO defaultDocumentDTO() {
        return IcesiDocumentDTO.builder()
                .icesiDocumentId(UUID.fromString("2dc074a1-2100-4d49-9823-aa12de103e70"))
                .title("Some title")
                .text("loreipsum")
                .userId(UUID.fromString("08a4db02-6625-40ee-b782-088add3a494f"))
                .build();
    }

    private IcesiDocument defaultDocument() {
        return IcesiDocument.builder()
                .icesiDocumentId(UUID.fromString("2dc074a1-2100-4d49-9823-aa12de103e70"))
                .title("Some title")
                .text("loreipsum")
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
