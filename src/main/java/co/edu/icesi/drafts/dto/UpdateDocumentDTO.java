package co.edu.icesi.drafts.dto;

import co.edu.icesi.drafts.model.IcesiDocumentStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UpdateDocumentDTO {
    private String ID;
    private String title;
    private String text;
    private IcesiDocumentStatus status;


}
