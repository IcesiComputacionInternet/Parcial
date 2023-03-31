package co.edu.icesi.drafts.service;

import co.edu.icesi.drafts.dto.IcesiDocumentDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import java.util.List;

@Service
@Qualifier
public interface IcesiDocumentService {
    List<IcesiDocumentDTO> getAllDocuments();

    List<IcesiDocumentDTO> createDocuments(List<IcesiDocumentDTO> documentsDTO);

    IcesiDocumentDTO updateDocument(String documentId, IcesiDocumentDTO icesiDocumentDTO);

    IcesiDocumentDTO createDocument(IcesiDocumentDTO icesiDocumentDTO);
}
