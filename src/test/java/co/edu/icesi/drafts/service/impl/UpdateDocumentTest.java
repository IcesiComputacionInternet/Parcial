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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
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
    @DisplayName("When document is on approved cant be modified")
    public void TestUpdate_WhenDocumentIsOnApprovedCantBeModified(){
        var document = defaultDocument();
        var documentDTO = defaultDocumentDTO();
        var documentId = "2dc074a1-2100-4d49-9823-aa12de103e70";
        documentDTO.setStatus(IcesiDocumentStatus.APPROVED);
        document.setStatus(IcesiDocumentStatus.APPROVED);

        when(documentRepository.findByTitle(any())).thenReturn(Optional.empty());
        when(documentRepository.findById((String) any())).thenReturn(Optional.of(document));

        var exception = assertThrows(IcesiException.class, () -> documentService.updateDocument(documentId,documentDTO));

        var error = exception.getError();
        var details = error.getDetails();
        assertEquals(1,details.size());
        var detail = details.get(0);
        assertEquals("ERR_400",detail.getErrorCode(), "Code doesn't match");
        assertEquals("field Document Status", detail.getErrorMessage(), "Error message doesn't match");
    }
    @Test
    @DisplayName("When document Id doesn't exists it throws an exception 404")
    public void TestUpdate_WhenDocumentIdDoesntExists(){
        var document = defaultDocument();
        var documentDTO = defaultDocumentDTO();
        var documentId = "2dc074a1-2100-4d49-9823-aa12de103e70";

        when(documentRepository.findById((String) any())).thenReturn(Optional.empty());

        var exception = assertThrows(IcesiException.class, () -> documentService.updateDocument(documentId,documentDTO));

        var error = exception.getError();
        var details = error.getDetails();
        assertEquals(1,details.size());
        var detail = details.get(0);
        assertEquals("ERR_404",detail.getErrorCode(), "Code doesn't match");
        assertEquals("Document with Id: 2dc074a1-2100-4d49-9823-aa12de103e70 not found", detail.getErrorMessage(), "Error message doesn't match");
    }

    @Test
    @DisplayName("When document title is different, but the new title already exists throws an ERR_DUPLICATED")
    public void TestUpdate_WhenDocumentTitleIsDiferentAndNewAlreadyExists(){
        var document = defaultDocument();
        var documentDTO = defaultDocumentDTO();
        var document2 = IcesiDocument.builder()
                .icesiDocumentId(UUID.fromString("2dc074a1-5600-4d49-9829-aa12de103e70"))
                .title("Some title 1")
                .text("loreipsum")
                .icesiUser(defaultUser())
                .status(IcesiDocumentStatus.DRAFT)
                .build();
        documentDTO.setTitle("Some Title 1");
        document.setStatus(IcesiDocumentStatus.DRAFT);
        documentDTO.setStatus(IcesiDocumentStatus.DRAFT);
        var documentId = "2dc074a1-2100-4d49-9823-aa12de103e70";

        when(documentRepository.findById((String) any())).thenReturn(Optional.of(document));
        when(documentRepository.findByTitle(any())).thenReturn(Optional.of(document2));

        var exception = assertThrows(IcesiException.class, () -> documentService.updateDocument(documentId,documentDTO));

        var error = exception.getError();
        var details = error.getDetails();
        assertEquals(1,details.size());
        var detail = details.get(0);
        assertEquals("ERR_DUPLICATED",detail.getErrorCode(), "Code doesn't match");
        assertEquals("resource Document with field Title: Some title 1, already exists", detail.getErrorMessage(), "Error message doesn't match");
    }
    @Test
    @DisplayName("Update document success")
    public void updateDocument_HappyPath() {
        // Arrange
        var document = defaultDocument();
        var documentDTO = defaultDocumentDTO();
        var documentId = "2dc074a1-2100-4d49-9823-aa12de103e70";
        documentDTO.setTitle("Some TITLE 1");
        document.setStatus(IcesiDocumentStatus.DRAFT);
        documentDTO.setStatus(IcesiDocumentStatus.DRAFT);

        when(documentRepository.findById((String) any())).thenReturn(Optional.of(document));
        when(documentRepository.findByTitle(any())).thenReturn(Optional.empty());
        // Act
        documentService.updateDocument(documentId, documentDTO);

        // Assert
        verify(documentRepository, times(1)).findById(documentId);
        verify(documentRepository,times(1)).findByTitle(documentDTO.getTitle());
        verify(documentMapper, times(1)).fromIcesiDocument(any());
        verify(documentRepository,times(1)).save(any());
    }
    private IcesiDocumentDTO defaultDocumentDTO(){
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
