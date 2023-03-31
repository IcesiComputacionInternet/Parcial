package co.edu.icesi.drafts.dto;

import co.edu.icesi.drafts.model.IcesiDocument;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import javax.persistence.OneToMany;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IcesiUserDTO {

    private UUID icesiUserId;
    private String firstName;
    private String lastName;
    private String code;
    private String email;
    private String phoneNumber;

    private List<IcesiDocumentDTO> documents;

}
