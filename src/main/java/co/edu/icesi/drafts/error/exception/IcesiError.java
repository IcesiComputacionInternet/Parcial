package co.edu.icesi.drafts.error.exception;

import lombok.*;
import org.springframework.http.HttpStatus;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IcesiError {

    private HttpStatus status;
    private List<IcesiErrorDetail> details;

}
