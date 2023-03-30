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

import static org.junit.jupiter.api.Assertions.fail;
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
        var documentDTO = defaultDocumentDTO();
        var user = defaultUser();
        var document = defaultDocument();
        documentDTO.setStatus(IcesiDocumentStatus.APPROVED);
        document.setStatus(IcesiDocumentStatus.APPROVED);

        when(userRepository.findById(any())).thenReturn(Optional.of(user));
        when(documentRepository.findByTitle(any())).thenReturn(Optional.empty());

        documentService.createDocument(documentDTO);

        verify(documentRepository,times(1)).save(document);

        fail();
    }

    private IcesiDocumentDTO defaultDocumentDTO(){
        return IcesiDocumentDTO.builder()
                .icesiDocumentId(UUID.fromString("2dc074a1-2100-4d49-9823-aa12de103e70"))
                .title("Some title")
                .text("loreipsum")
                .userId(UUID.fromString("08a4db02-6625-40ee-b782-088add3a494f"))
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
    private IcesiDocument defaultDocument() {
        return IcesiDocument.builder()
                .icesiDocumentId(UUID.fromString("2dc074a1-2100-4d49-9823-aa12de103e70"))
                .title("Some title")
                .text("loreipsum")
                .icesiUser(defaultUser())
                .build();
    }
}
