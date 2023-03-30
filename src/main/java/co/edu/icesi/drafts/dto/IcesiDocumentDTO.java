package co.edu.icesi.drafts.dto;

import co.edu.icesi.drafts.model.IcesiDocumentStatus;
import co.edu.icesi.drafts.model.IcesiUser;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import org.springframework.lang.NonNull;

import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.UUID;


@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IcesiDocumentDTO {

    private UUID icesiDocumentId;
    @NotBlank(message = "can't be blank")
    @NotNull(message = "can't be null")
    private String title;
    private String text;
    @NotNull(message = "can't be null")
    private IcesiDocumentStatus status;
    private UUID userId;

}
