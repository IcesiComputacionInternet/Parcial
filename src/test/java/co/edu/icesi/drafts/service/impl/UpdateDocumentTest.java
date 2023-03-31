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
import static org.mockito.Mockito.times;

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
                .status(IcesiDocumentStatus.DRAFT)
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

    @Test
    public void TestUpdate_WhenDocumentIsOnApprovedTheTitleCantBeModified(){
        // Arrange
        var user = defaultUser();
        var document = defaultDocument();
        document.setStatus(IcesiDocumentStatus.APPROVED);
        var documentUpdatedDTO = defaultDocumentDTO();
        documentUpdatedDTO.setTitle(document.getTitle()+"1");
        when(userRepository.findById(any())).thenReturn(Optional.of(user));
        when(documentRepository.findById(any())).thenReturn(Optional.of(document));

        // Act
        IcesiException exception = assertThrows(IcesiException.class, () -> documentService.updateDocument(document.getIcesiDocumentId().toString(), documentUpdatedDTO));

        // Assert
        var error = exception.getError();
        var details = error.getDetails();
        assertEquals(1, details.size());
        var detail = details.get(0);
        assertEquals("The title can not be updated", exception.getMessage());
        assertEquals("ERR_400", detail.getErrorCode(), "Code doesn't match");
        assertEquals("field Title can not be updated because the document status is "+IcesiDocumentStatus.APPROVED, detail.getErrorMessage(), "Error message doesn't match");
        verify(documentRepository, times(0)).updateDocument(any(), any(), any(), any());
        verify(documentMapper, times(0)).fromIcesiDocument(any());
    }

    @Test
    public void TestUpdate_WhenDocumentIsOnApprovedTheTextCantBeModified(){
        // Arrange
        var user = defaultUser();
        var document = defaultDocument();
        document.setStatus(IcesiDocumentStatus.APPROVED);
        var documentUpdatedDTO = defaultDocumentDTO();
        documentUpdatedDTO.setText(document.getText()+"1");
        when(userRepository.findById(any())).thenReturn(Optional.of(user));
        when(documentRepository.findById(any())).thenReturn(Optional.of(document));

        // Act
        IcesiException exception = assertThrows(IcesiException.class, () -> documentService.updateDocument(document.getIcesiDocumentId().toString(), documentUpdatedDTO));

        // Assert
        var error = exception.getError();
        var details = error.getDetails();
        assertEquals(1, details.size());
        var detail = details.get(0);
        assertEquals("The text can not be updated", exception.getMessage());
        assertEquals("ERR_400", detail.getErrorCode(), "Code doesn't match");
        assertEquals("field Text can not be updated because the document status is "+IcesiDocumentStatus.APPROVED, detail.getErrorMessage(), "Error message doesn't match");
        verify(documentRepository, times(0)).updateDocument(any(), any(), any(), any());
        verify(documentMapper, times(0)).fromIcesiDocument(any());
    }

    @Test
    public void TestUpdate_WhenThereIsNoDocumentWithTheProvidedId(){
        // Arrange
        var user = defaultUser();
        var documentUpdatedDTO = defaultDocumentDTO();
        var document = defaultDocument();
        when(userRepository.findById(any())).thenReturn(Optional.of(user));
        when(documentRepository.findById(any())).thenReturn(Optional.empty());

        // Act
        IcesiException exception = assertThrows(IcesiException.class, () -> documentService.updateDocument(document.getIcesiDocumentId().toString(), documentUpdatedDTO));

        // Assert
        var error = exception.getError();
        var details = error.getDetails();
        assertEquals(1, details.size());
        var detail = details.get(0);
        assertEquals("Document not found", exception.getMessage());
        assertEquals("ERR_404", detail.getErrorCode(), "Code doesn't match");
        assertEquals("Document with Document Id: "+document.getIcesiDocumentId() + " not found", detail.getErrorMessage(), "Error message doesn't match");
        verify(documentRepository, times(0)).updateDocument(any(), any(), any(), any());
        verify(documentMapper, times(0)).fromIcesiDocument(any());
    }

    @Test
    public void TestUpdate_WhenTryingToChangeTheUser(){
        // Arrange
        var user = defaultUser();
        var documentUpdatedDTO = defaultDocumentDTO();
        documentUpdatedDTO.setUserId(UUID.fromString("08a4db02-6625-1234-abcd-088add3a494f"));
        when(userRepository.findById(any())).thenReturn(Optional.of(user));
        when(documentRepository.findById(any())).thenReturn(Optional.of(defaultDocument()));

        // Act
        IcesiException exception = assertThrows(IcesiException.class, () -> documentService.updateDocument(documentUpdatedDTO.getIcesiDocumentId().toString(), documentUpdatedDTO));

        // Assert
        var error = exception.getError();
        var details = error.getDetails();
        assertEquals(1, details.size());
        var detail = details.get(0);
        assertEquals("The user can not be updated", exception.getMessage());
        assertEquals("ERR_400", detail.getErrorCode(), "Code doesn't match");
        assertEquals("field User can not be updated", detail.getErrorMessage(), "Error message doesn't match");
        verify(documentRepository, times(0)).updateDocument(any(), any(), any(), any());
        verify(documentMapper, times(0)).fromIcesiDocument(any());
    }

    @Test
    public void TestUpdate_WhenTryingToSetAnExistingTitle(){
        // Arrange
        var user = defaultUser();
        var documentUpdatedDTO = defaultDocumentDTO();
        var document = defaultDocument();
        when(userRepository.findById(any())).thenReturn(Optional.of(user));
        when(documentRepository.findById(any())).thenReturn(Optional.of(document));
        when(documentRepository.findByTitle(any())).thenReturn(Optional.of(document));

        // Act
        IcesiException exception = assertThrows(IcesiException.class, () -> documentService.updateDocument(documentUpdatedDTO.getIcesiDocumentId().toString(), documentUpdatedDTO));

        // Assert
        var error = exception.getError();
        var details = error.getDetails();
        assertEquals(1, details.size());
        var detail = details.get(0);
        assertEquals("The title " + document.getTitle() + " already exists", exception.getMessage());
        assertEquals("ERR_DUPLICATED", detail.getErrorCode(), "Code doesn't match");
        assertEquals("resource Document with field Title: " + document.getTitle() + ", already exists", detail.getErrorMessage(), "Error message doesn't match");
        verify(documentRepository, times(0)).updateDocument(any(), any(), any(), any());
        verify(documentMapper, times(0)).fromIcesiDocument(any());
    }

    @Test
    public void TestUpdate_WhenTheDocumentUpdatedIsNoLongerInTheDB(){
        // Arrange
        var user = defaultUser();
        var documentUpdatedDTO = defaultDocumentDTO();
        var document = defaultDocument();
        when(userRepository.findById(any())).thenReturn(Optional.of(user));
        when(documentRepository.findById(any())).thenReturn(Optional.of(document), Optional.empty());
        when(documentRepository.findByTitle(any())).thenReturn(Optional.empty());

        // Act
        IcesiException exception = assertThrows(IcesiException.class, () -> documentService.updateDocument(documentUpdatedDTO.getIcesiDocumentId().toString(), documentUpdatedDTO));

        // Assert
        var error = exception.getError();
        var details = error.getDetails();
        assertEquals(1, details.size());
        var detail = details.get(0);
        assertEquals("The document that was just updated no longer exists", exception.getMessage());
        assertEquals("ERR_500", detail.getErrorCode(), "Code doesn't match");
        assertEquals("Oops, we ran into an error", detail.getErrorMessage(), "Error message doesn't match");
        verify(documentRepository, times(1)).updateDocument(any(), any(), any(), any());
        verify(documentRepository, times(1)).findByTitle(any());
        verify(documentMapper, times(0)).fromIcesiDocument(any());
    }

    @Test
    public void TestUpdate_WhenTheNewDocumentIdAlreadyExists(){
        // Arrange
        var user = defaultUser();
        var documentUpdatedDTO = defaultDocumentDTO();
        documentUpdatedDTO.setIcesiDocumentId(UUID.fromString("08a4db02-6625-1234-abcd-088add3a494f"));
        var document = defaultDocument();
        when(userRepository.findById(any())).thenReturn(Optional.of(user));
        when(documentRepository.findById(any())).thenReturn(Optional.of(document), Optional.of(document));
        when(documentRepository.findByTitle(any())).thenReturn(Optional.empty());

        IcesiException exception = assertThrows(IcesiException.class, () -> documentService.updateDocument(document.getIcesiDocumentId().toString(), documentUpdatedDTO));

        // Assert
        var error = exception.getError();
        var details = error.getDetails();
        assertEquals(1, details.size());
        var detail = details.get(0);
        assertEquals("The document id " + documentUpdatedDTO.getIcesiDocumentId() + " already exists", exception.getMessage());
        assertEquals("ERR_DUPLICATED", detail.getErrorCode(), "Code doesn't match");
        assertEquals("resource Document with field Id: "+ documentUpdatedDTO.getIcesiDocumentId() +", already exists", detail.getErrorMessage(), "Error message doesn't match");
        verify(documentRepository, times(0)).updateDocument(any(), any(), any(), any());
        verify(documentRepository, times(1)).findByTitle(any());
        verify(documentRepository, times(2)).findById(any());
        verify(documentMapper, times(0)).fromIcesiDocument(any());
    }

    @Test
    public void TestUpdate_HappyPath(){
        // Arrange
        var user = defaultUser();
        var documentUpdatedDTO = defaultDocumentDTO();
        var document = defaultDocument();
        when(userRepository.findById(any())).thenReturn(Optional.of(user));
        when(documentRepository.findById(any())).thenReturn(Optional.of(document), Optional.of(document));
        when(documentRepository.findByTitle(any())).thenReturn(Optional.ofNullable(null));

        // Act
        documentService.updateDocument(documentUpdatedDTO.getIcesiDocumentId().toString(), documentUpdatedDTO);

        // Assert
        verify(documentRepository, times(1)).updateDocument(any(), any(), any(), any());
        verify(documentRepository, times(1)).findByTitle(any());
        verify(documentRepository, times(2)).findById(any());
        verify(documentMapper, times(1)).fromIcesiDocument(any());
    }
}
