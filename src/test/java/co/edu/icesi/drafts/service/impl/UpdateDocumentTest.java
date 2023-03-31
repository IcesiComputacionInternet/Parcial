package co.edu.icesi.drafts.service.impl;

import co.edu.icesi.drafts.dto.IcesiDocumentDTO;
import co.edu.icesi.drafts.dto.IcesiUserDTO;
import co.edu.icesi.drafts.error.exception.IcesiException;
import co.edu.icesi.drafts.mapper.IcesiDocumentMapper;
import co.edu.icesi.drafts.mapper.IcesiUserMapper;
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
import static org.mockito.Mockito.*;

public class UpdateDocumentTest {

    private static final UUID DOCUMENT_UUID = UUID.fromString("5b666754-e217-4775-9c95-352ebb0673cb");

    private static final UUID USER_UUID = UUID.fromString("be01f5ea-3b10-4f1a-b9d3-710b403f7f4c");
    private IcesiDocumentService documentService;

    private IcesiDocumentRepository documentRepository;

    private IcesiUserRepository userRepository;

    private IcesiDocumentMapper documentMapper;

    private IcesiUserMapper icesiUserMapper;


    @BeforeEach
    public void init() {
        documentRepository = mock(IcesiDocumentRepository.class);
        documentMapper = spy(IcesiDocumentMapper.class);
        userRepository = mock(IcesiUserRepository.class);
        icesiUserMapper = spy(IcesiUserMapper.class);
        documentService = new IcesiDocumentServiceImpl(userRepository, documentRepository, documentMapper);
    }

    @Test
    public void TestUpdate_WhenDocumentIsOnApprovedCantBeModified(){
        //TODO implement test!
        //kill me now pls
        when(documentRepository.findById(any())).thenReturn(Optional.empty());
        try{
            IcesiUser icesiUser = icesiUserMapper.fromIcesiUserDTO(defaultIcesiUser());
            userRepository.save(icesiUser);

            documentService.updateDocument(DOCUMENT_UUID.toString(), defaultDocumentDTO());
        }catch (IcesiException e){

        }
        fail();
    }

    private IcesiUserDTO defaultIcesiUser(){
       return IcesiUserDTO.builder()
               .documents(null)
               .email("email")
               .code("code")
               .phoneNumber("phone")
               .firstName("Juan")
               .lastName("Felipe")
               .icesiUserId(USER_UUID)
               .build();
    }

    private IcesiDocumentDTO defaultDocumentDTO() {
        return IcesiDocumentDTO.builder()
                .text("lorena impsumi")
                .title("inspiring title")
                .status(IcesiDocumentStatus.APPROVED)
                .icesiDocumentId(DOCUMENT_UUID)
                .userId(USER_UUID)
                .build();
    }
}
