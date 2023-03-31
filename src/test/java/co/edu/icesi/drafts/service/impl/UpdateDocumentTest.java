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
    public void testUpdate_HappyPath() {
        when(documentRepository.findById(any(UUID.class))).thenReturn(Optional.ofNullable(defaultDocument()));
        when(documentRepository.findByTitle(any(String.class))).thenReturn(Optional.empty());

        var updatedDocument = defaultDocumentDTO();
        updatedDocument.setTitle("New title");
        updatedDocument.setText("New text");

        documentService.updateDocument(defaultDocumentDTO().getIcesiDocumentId().toString(), updatedDocument);
        verify(documentRepository, times(1)).save(any(IcesiDocument.class));
        assertNotEquals(defaultDocumentDTO().getTitle(), updatedDocument.getTitle());
        assertNotEquals(defaultDocumentDTO().getText(), updatedDocument.getText());
    }

    @Test
    public void TestUpdate_WhenDocumentIsOnApprovedCantBeModified(){
        var documentApproved = defaultDocument();
        documentApproved.setStatus(IcesiDocumentStatus.APPROVED);
        when(documentRepository.findById(any())).thenReturn(Optional.of(documentApproved));

        var exception = assertThrows(IcesiException.class, () ->
                documentService.updateDocument(UUID.randomUUID().toString(), defaultDocumentDTO())
        );

        var error = exception.getError();
        var details = error.getDetails();
        assertEquals(1, details.size());
        var detail = details.get(0);
        assertEquals("ERR_500",detail.getErrorCode());
        assertEquals("Oops, we ran into an error",detail.getErrorMessage());
        verify(documentRepository, times(0)).save(any(IcesiDocument.class));
    }

    @Test
    public void testUpdate_WhenTitleAlreadyExists() {
        when(documentRepository.findById(any(UUID.class))).thenReturn(Optional.ofNullable(defaultDocument()));
        when(documentRepository.findByTitle(any(String.class))).thenReturn(Optional.of(defaultDocument()));

        var updatedDocument = defaultDocumentDTO();
        updatedDocument.setText("New text");

        var exception = assertThrows(IcesiException.class, () ->
                documentService.updateDocument(defaultDocumentDTO().getIcesiDocumentId().toString(), updatedDocument)
        );

        var error = exception.getError();
        var details = error.getDetails();
        assertEquals(1, details.size());
        var detail = details.get(0);
        assertEquals("ERR_DUPLICATED",detail.getErrorCode());
        assertEquals("resource Document with field Title: " + updatedDocument.getTitle() + ", already exists",detail.getErrorMessage());
        verify(documentRepository, times(0)).save(any(IcesiDocument.class));
    }

    private IcesiDocumentDTO defaultDocumentDTO() {
        return IcesiDocumentDTO.builder()
                .icesiDocumentId(UUID.fromString("2dc074a1-2100-4d49-9823-aa12de103e70"))
                .title("Some title")
                .text("loreipsum")
                .status(IcesiDocumentStatus.DRAFT)
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
