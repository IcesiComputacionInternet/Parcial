package co.edu.icesi.drafts.error.exception;

import lombok.Builder;
import lombok.Getter;


@Getter
public class DetailBuilder {

    private final ErrorCode errorCode;
    private final Object[] fields;

    public DetailBuilder(ErrorCode errorCode, Object... fields){
        this.errorCode = errorCode;
        this.fields = fields;
    }


}
