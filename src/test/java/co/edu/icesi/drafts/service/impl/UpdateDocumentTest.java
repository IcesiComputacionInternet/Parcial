package co.edu.icesi.drafts.service.impl;

import co.edu.icesi.drafts.dto.UpdateDocumentDTO;
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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
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
        //TODO implement test!
        var defaultDocument = defaultDocument();
        defaultDocument.setStatus(IcesiDocumentStatus.APPROVED);
        when(documentRepository.findById(anyString())).thenReturn(Optional.of(defaultDocument));
        assertThrows(IcesiException.class, () -> documentService.updateDocument(defaultUpdateDocumentDTO()));
    }
    @Test
    public void TestUpdate_HappyPath(){
        //TODO implement test!
        var defaultDocument = defaultDocument();
        defaultDocument.setStatus(IcesiDocumentStatus.DRAFT);
        when(documentRepository.findById(anyString())).thenReturn(Optional.of(defaultDocument));
        documentService.updateDocument(defaultUpdateDocumentDTO());
        verify(documentRepository, times(1)).save(defaultDocument);
    }

    private UpdateDocumentDTO defaultUpdateDocumentDTO() {
        return UpdateDocumentDTO.builder()
                .ID("2dc074a1-2100-4d49-9823-aa12de103e70")
                .title("Some new title")
                .text("loreipsum is sus")
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
