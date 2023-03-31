package co.edu.icesi.drafts.service.impl;

import co.edu.icesi.drafts.dto.IcesiDocumentDTO;
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

    @Test
    public void TestUpdate_WhenDocumentIsOnApprovedCanBeModified(){
        var documentDTO = defaultDocumentDTO();
        var user = defaultUser();
        var document = defaultDocument();
        when(userRepository.findById(any())).thenReturn(Optional.of(user));
        when(documentRepository.getTypeofDocument(any())).thenReturn(Optional.of(document));

        documentService.createDocument(documentDTO);
        documentDTO.setText("new text");
        documentService.updateDocument(documentDTO.getIcesiDocumentId().toString(), documentDTO);

        // Assert
        verify(documentRepository, times(1)).getTypeofDocument(any());
        assertEquals("new text", documentDTO.getText());
    }

    @Test
    public void TestUpdate_WhenDocumentIsOnApprovedCannotBeModified(){
        var documentDTO = defaultDocumentDTO();
        var user = defaultUser();
        var document = defaultDocument();
        document.setStatus(IcesiDocumentStatus.APPROVED);
        var oldText = document.getText();

        when(userRepository.findById(any())).thenReturn(Optional.of(user));

        try {
            documentService.updateDocument(documentDTO.getIcesiDocumentId().toString(), documentDTO);
            documentDTO.setText("new text");
            fail();
        } catch (RuntimeException exception) {
            String message = exception.getMessage();
            assertEquals("Invalid Type of Document", message);
            assertEquals(oldText, documentDTO.getText());
        }
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
