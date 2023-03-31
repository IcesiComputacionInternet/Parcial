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

        // Arrange
        var document = createApprovedDocument();
        var user = defaultUser();
        when(userRepository.findById(any())).thenReturn(Optional.of(user));
        when(documentRepository.findById(any())).thenReturn(Optional.of(defaultDocument()));
        // Act
        documentService.createDocument(document);

        try {
            documentService.updateDocument(document.getIcesiDocumentId().toString(), defaultDocumentDTO());
            fail();
        }catch (RuntimeException exception){
            assertEquals("Status can't be modified", exception.getMessage());
        }
    }

    @Test
    public void TestUpdate_WhenDocumentNotExists(){
        // Arrange
        var documentDTO = defaultDocumentDTO();
        documentDTO.setTitle(null);
        var user = defaultUser();
        when(documentRepository.findByTitle(any())).thenReturn(Optional.of(defaultDocument()));
        // Act
        var exception = assertThrows(IcesiException.class, () -> documentService.updateDocument(documentDTO.getTitle(), documentDTO), "No exception was thrown");

        // Assert
        var error = exception.getError();
        var details = error.getDetails();
        assertEquals(1, details.size());
        var detail = details.get(0);
        assertEquals("ERR_404", detail.getErrorCode(), "Code doesn't match");
        assertEquals("document with title: null not found", detail.getErrorMessage(), "Error message doesn't match");
    }

    @Test
    public void TestUpdate_UpdateTitleAndText(){
        // Arrange
        var document = defaultDocumentDTO();
        var user = defaultUser();
        when(userRepository.findById(any())).thenReturn(Optional.of(user));
        when(documentRepository.findById(any())).thenReturn(Optional.of(defaultDocument1()));
        // Act
        documentService.createDocument(document);

        IcesiDocumentDTO newDocument = documentService.updateDocument(document.getIcesiDocumentId().toString(), defaultDocumentDTO1());

        assertEquals("Another title", newDocument.getTitle());
    }

    public IcesiDocumentDTO createApprovedDocument(){
        return IcesiDocumentDTO.builder()
                .icesiDocumentId(UUID.randomUUID())
                .title("Criterios de evaluacion")
                .text("Este documento contiene criterios de evaluacion")
                .status(IcesiDocumentStatus.APPROVED)
                .userId(UUID.fromString("08a4db02-6625-40ee-b782-088add3a494f"))
                .build();
    }

    private IcesiDocumentDTO defaultDocumentDTO() {
        return IcesiDocumentDTO.builder()
                .icesiDocumentId(UUID.fromString("2dc074a1-2100-4d49-9823-aa12de103e70"))
                .title("Some title")
                .text("loreipsum")
                .status(IcesiDocumentStatus.REVISION)
                .userId(UUID.fromString("08a4db02-6625-40ee-b782-088add3a494f"))
                .build();
    }

    private IcesiDocumentDTO defaultDocumentDTO1() {
        return IcesiDocumentDTO.builder()
                .icesiDocumentId(UUID.fromString("4dc074a1-2100-4d49-9823-aa12de103e70"))
                .title("Another title")
                .text("loreipsum")
                .status(IcesiDocumentStatus.REVISION)
                .userId(UUID.fromString("08a4db02-6625-40ee-b782-088add3a494f"))
                .build();
    }

    private IcesiDocument defaultDocument1() {
        return IcesiDocument.builder()
                .icesiDocumentId(UUID.fromString("2dc074a1-2100-4d49-9823-aa12de103e70"))
                .title("Some title")
                .text("loreipsum")
                .status(IcesiDocumentStatus.REVISION)
                .icesiUser(defaultUser())
                .build();
    }

    private IcesiDocument defaultDocument() {
        return IcesiDocument.builder()
                .icesiDocumentId(UUID.fromString("2dc074a1-2100-4d49-9823-aa12de103e70"))
                .title("Some title")
                .text("loreipsum")
                .status(IcesiDocumentStatus.DRAFT)
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
