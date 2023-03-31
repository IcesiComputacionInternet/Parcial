package co.edu.icesi.drafts.error.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class IcesiError {

    private HttpStatus status;
    private List<IcesiErrorDetail> details;

}
