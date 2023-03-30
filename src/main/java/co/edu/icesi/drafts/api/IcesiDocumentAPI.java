package co.edu.icesi.drafts.api;

import co.edu.icesi.drafts.dto.IcesiDocumentDTO;
import co.edu.icesi.drafts.dto.UpdateDocumentDTO;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RequestMapping("/documents")
public interface IcesiDocumentAPI {

    @GetMapping
    List<IcesiDocumentDTO> getAllDocuments();

    @PostMapping
    IcesiDocumentDTO createDocument(@Valid @RequestBody IcesiDocumentDTO documentDTO);

    @PostMapping("/all")
    List<IcesiDocumentDTO> createDocuments(@Valid @RequestBody List<IcesiDocumentDTO> documentDTOS);

    @PutMapping("/{documentId}")
    IcesiDocumentDTO updateDocument(@Valid @RequestBody UpdateDocumentDTO updateDocumentDTO);



}
