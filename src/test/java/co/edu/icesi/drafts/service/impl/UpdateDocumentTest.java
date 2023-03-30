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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.UUID;

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
        IcesiDocumentDTO doc = defaultDocumentDTO();
        RuntimeException exception = assertThrows(RuntimeException.class, () -> documentService.updateDocument(null, doc))
    }

    private IcesiDocument defaultIcesiDocument() {
        return IcesiDocument.builder()
        .title("Dune")
        .text("default text")
        .status(IcesiDocumentStatus.APPROVED)
        .icesiUser(defaultUser())
        .build();
    }

    private IcesiDocumentDTO defaultDocumentDTO() {
        return IcesiDocumentDTO.builder()
        .title("Dune")
        .text("default text")
        .status(IcesiDocumentStatus.APPROVED)
        .userId(UUID.fromString("c0a80101-0000-0000-0000-000000000000"))
        .build();
    }

    private IcesiUser defaultUser() {
        return IcesiUser.builder()
        .firstName("John")
        .lastName("Doe")
        .code("123")
        .email("example@gmail")
        .phoneNumber("9999-9999")
        .build();
    }

}
