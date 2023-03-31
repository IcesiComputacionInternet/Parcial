package co.edu.icesi.drafts.service.impl;

import co.edu.icesi.drafts.dto.IcesiDocumentDTO;
import co.edu.icesi.drafts.error.exception.IcesiErrorDetail;
import co.edu.icesi.drafts.error.exception.IcesiException;
import co.edu.icesi.drafts.mapper.IcesiDocumentMapper;
import co.edu.icesi.drafts.mapper.IcesiDocumentMapperImpl;
import co.edu.icesi.drafts.model.IcesiDocument;
import co.edu.icesi.drafts.model.IcesiUser;
import co.edu.icesi.drafts.repository.IcesiDocumentRepository;
import co.edu.icesi.drafts.repository.IcesiUserRepository;
import co.edu.icesi.drafts.service.IcesiDocumentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

public class DocumentServiceTest {

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
    @DisplayName("Create document success")
    public void createDocument_HappyPath() {
        // Arrange
        var documentDTO = defaultDocumentDTO();
        var user = defaultUser();
        var document = defaultDocument();
        when(userRepository.findById(any())).thenReturn(Optional.of(user));
        when(documentRepository.findByTitle(any())).thenReturn(Optional.empty());
        // Act
        documentService.createDocument(documentDTO);

        // Assert
        verify(documentRepository, times(1)).save(document);
        verify(documentMapper, times(1)).fromIcesiDocument(any());
        verify(documentMapper, times(1)).fromIcesiDocumentDTO(any());
    }

    @Test
    @DisplayName("When userId in DTO is null method throws an error")
    public void createDocument_WhenUserIdInDTOIsNull_ShouldThrowException() {
        // Arrange
        var documentDTO = defaultDocumentDTO();
        documentDTO.setUserId(null);
        var user = defaultUser();
        when(userRepository.findById(any())).thenReturn(Optional.of(user));
        // Act
        var exception = assertThrows(IcesiException.class, () -> documentService.createDocument(documentDTO), "No exception was thrown");

        // Assert
        var error = exception.getError();
        var details = error.getDetails();
        assertEquals(1, details.size());
        var detail = details.get(0);
        assertEquals("ERR_REQUIRED_FIELD", detail.getErrorCode(), "Code doesn't match");
        assertEquals("field userId is required", detail.getErrorMessage(), "Error message doesn't match");
    }

    @Test
    @DisplayName("When title in DTO already exits method throws an error")
    public void createDocument_WhenTitleIsNotUnique_ShouldThrowException() {

        // Arrange
        var documentDTO = defaultDocumentDTO();
        var document = defaultDocument();
        var user = defaultUser();
        when(documentRepository.findByTitle("Some title")).thenReturn(Optional.of(document));
        when(userRepository.findById(any())).thenReturn(Optional.of(user));

        // Act
        var exception = assertThrows(IcesiException.class, () -> documentService.createDocument(documentDTO), "No exception was thrown");

        // Assert
        var error = exception.getError();
        var details = error.getDetails();
        assertEquals(1, details.size());
        var detail = details.get(0);
        assertEquals("ERR_DUPLICATED", detail.getErrorCode(), "Code doesn't match");
        assertEquals("resource Document with field Title: Some title, already exists", detail.getErrorMessage(), "Error message doesn't match");
    }

    @Test
    @DisplayName("Create documents success")
    public void createDocuments_HappyPath() {

        var documentsDTO = defaultDocumentsDTO();
        var documents = defaultDocuments();
        var user = defaultUser();

        when(userRepository.findById(any())).thenReturn(Optional.of(user));
        when(documentRepository.findByTitle(any())).thenReturn(Optional.empty());

        documentService.createDocuments(documentsDTO);

        verify(documentRepository, times(1)).saveAll(documents);
        verify(documentMapper, times(2)).fromIcesiDocumentDTO(any());
        verify(documentMapper, times(2)).fromIcesiDocument(any());

    }

    @Test
    @DisplayName("When title already exists throw an exception with all repeated titles")
    public void createDocuments_WhenTitleIsNotUnique_ShouldThrowExceptionWithDetailsOfEachRepeatedTitle() {

        var documentsDTO = new ArrayList<>(defaultDocumentsDTO());
        documentsDTO.add(IcesiDocumentDTO.builder()
                .icesiDocumentId(UUID.randomUUID())
                .title("Some title2")
                .text("loreipsum1")
                .userId(UUID.fromString("08a4db02-6625-40ee-b782-088add3a494f"))
                .build());
        var user = defaultUser();

        when(userRepository.findById(any())).thenReturn(Optional.of(user));
        when(documentRepository.findByTitle("Some title")).thenReturn(Optional.of(defaultDocument()));
        when(documentRepository.findByTitle("Some title1")).thenReturn(Optional.of(defaultDocument()));

        var exception = assertThrows(IcesiException.class, () -> documentService.createDocuments(documentsDTO));

        var error = exception.getError();
        var details = error.getDetails().stream().sorted(Comparator.comparing(IcesiErrorDetail::getErrorMessage)).toList();
        assertEquals(2, details.size());
        var detail = details.get(0);
        assertEquals("ERR_DUPLICATED", detail.getErrorCode(), "Code doesn't match");
        assertEquals("resource Document with field Title: Some title, already exists", detail.getErrorMessage(), "Error message doesn't match");
        var detail1 = details.get(1);
        assertEquals("ERR_DUPLICATED", detail1.getErrorCode(), "Code doesn't match");
        assertEquals("resource Document with field Title: Some title1, already exists", detail1.getErrorMessage(), "Error message doesn't match");

    }

    @Test
    @DisplayName("When user doesn't exists throw an exception with all not found users")
    public void createDocuments_WhenUserNotFound_ShouldThrowExceptionWithDetailsOfEachNotFoundUser() {

        var documentsDTO = new ArrayList<>(defaultDocumentsDTO());
        documentsDTO.add(IcesiDocumentDTO.builder()
                .icesiDocumentId(UUID.randomUUID())
                .title("Some title2")
                .text("loreipsum1")
                .userId(UUID.fromString("d36dec17-5c40-461c-b168-9c6f59924db0"))
                .build());
        var user = defaultUser();

        when(userRepository.findById(UUID.fromString("08a4db02-6625-40ee-b782-088add3a494f"))).thenReturn(Optional.empty());
        when(userRepository.findById(UUID.fromString("d36dec17-5c40-461c-b168-9c6f59924db0"))).thenReturn(Optional.of(defaultUser()));
        when(documentRepository.findByTitle(any())).thenReturn(Optional.empty());

        var exception = assertThrows(IcesiException.class, () -> documentService.createDocuments(documentsDTO));

        var error = exception.getError();
        var details = error.getDetails().stream().sorted(Comparator.comparing(IcesiErrorDetail::getErrorMessage)).toList();
        assertEquals(2, details.size());
        var detail = details.get(0);
        assertEquals("ERR_404", detail.getErrorCode(), "Code doesn't match");
        assertEquals("User with Id: 08a4db02-6625-40ee-b782-088add3a494f not found", detail.getErrorMessage(), "Error message doesn't match");
        var detail1 = details.get(1);
        assertEquals("ERR_404", detail1.getErrorCode(), "Code doesn't match");
        assertEquals("User with Id: 08a4db02-6625-40ee-b782-088add3a494f not found", detail1.getErrorMessage(), "Error message doesn't match");

    }

    @Test
    @DisplayName("When user doesn't exists throw an exception with all not found users")
    public void createDocuments_WhenUserNotFoundAndTitleAlreadyExists_ShouldThrowExceptionWithDetailsOfEachNotFoundUserAndTitle() {

        var documentsDTO = new ArrayList<>(defaultDocumentsDTO());
        documentsDTO.add(IcesiDocumentDTO.builder()
                .icesiDocumentId(UUID.randomUUID())
                .title("Some title2")
                .text("loreipsum1")
                .userId(UUID.fromString("d36dec17-5c40-461c-b168-9c6f59924db0"))
                .build());
        var user = defaultUser();
        when(userRepository.findById(UUID.fromString("08a4db02-6625-40ee-b782-088add3a494f"))).thenReturn(Optional.of(user));
        when(userRepository.findById(UUID.fromString("d36dec17-5c40-461c-b168-9c6f59924db0"))).thenReturn(Optional.empty());
        when(documentRepository.findByTitle("Some title")).thenReturn(Optional.of(defaultDocument()));

        var exception = assertThrows(IcesiException.class, () -> documentService.createDocuments(documentsDTO));

        var error = exception.getError();
        var details = error.getDetails().stream().sorted(Comparator.comparing(IcesiErrorDetail::getErrorMessage)).toList();
        assertEquals(2, details.size());
        var detail = details.get(0);
        assertEquals("ERR_404", detail.getErrorCode(), "Code doesn't match");
        assertEquals("User with Id: d36dec17-5c40-461c-b168-9c6f59924db0 not found", detail.getErrorMessage(), "Error message doesn't match");
        var detail1 = details.get(1);
        assertEquals("ERR_DUPLICATED", detail1.getErrorCode(), "Code doesn't match");
        assertEquals("resource Document with field Title: Some title, already exists", detail1.getErrorMessage(), "Error message doesn't match");

    }

    private List<IcesiDocumentDTO> defaultDocumentsDTO() {
        return List.of(
                IcesiDocumentDTO.builder()
                        .icesiDocumentId(UUID.fromString("ba6f5f75-5140-417a-9560-8b6fb131e6cb"))
                        .title("Some title1")
                        .text("loreipsum1")
                        .userId(UUID.fromString("08a4db02-6625-40ee-b782-088add3a494f"))
                        .build(),
                IcesiDocumentDTO.builder()
                        .icesiDocumentId(UUID.fromString("2dc074a1-2100-4d49-9823-aa12de103e70"))
                        .title("Some title")
                        .text("loreipsum")
                        .userId(UUID.fromString("08a4db02-6625-40ee-b782-088add3a494f"))
                        .build()
        );
    }

    private List<IcesiDocument> defaultDocuments() {
        return List.of(
                IcesiDocument.builder()
                        .icesiDocumentId(UUID.fromString("ba6f5f75-5140-417a-9560-8b6fb131e6cb"))
                        .title("Some title1")
                        .text("loreipsum1")
                        .icesiUser(defaultUser())
                        .build(),
                IcesiDocument.builder()
                        .icesiDocumentId(UUID.fromString("2dc074a1-2100-4d49-9823-aa12de103e70"))
                        .title("Some title")
                        .text("loreipsum")
                        .icesiUser(defaultUser())
                        .build()
        );
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
