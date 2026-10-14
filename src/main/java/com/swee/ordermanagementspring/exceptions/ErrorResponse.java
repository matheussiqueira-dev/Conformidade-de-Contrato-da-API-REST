package com.swee.ordermanagementspring.exceptions;

import com.swee.ordermanagementspring.entities.StoreTime;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;

/** Formato unico de erro (A5). Contrato v4: nome ErrorResponse, timestamp com offset e code obrigatorio (REC-d, C-22). */
@Schema(name = "ErrorResponse", description = "Erro retornado pela API")
public class ErrorResponse {

    @Schema(type = "string", format = "date-time", requiredMode = Schema.RequiredMode.REQUIRED,
            description = "Instante RFC 3339 com offset")
    private final OffsetDateTime timestamp;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "400", maximum = "599")
    private final int status;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private final String error;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private final ErrorCode code;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private final String message;
    @ArraySchema(arraySchema = @Schema(types = {"array", "null"}, requiredMode = Schema.RequiredMode.REQUIRED),
            schema = @Schema(type = "string"))
    private final List<String> details;

    public ErrorResponse(int status, String error, ErrorCode code, String message) {
        this(status, error, code, message, null);
    }

    public ErrorResponse(int status, String error, ErrorCode code, String message, List<String> details) {
        this.timestamp = OffsetDateTime.now(StoreTime.clock());
        this.status = status;
        this.error = error;
        this.code = code;
        this.message = message;
        this.details = details == null ? null : List.copyOf(details);
    }

    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    public int getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }

    public ErrorCode getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public List<String> getDetails() {
        return details;
    }
}
