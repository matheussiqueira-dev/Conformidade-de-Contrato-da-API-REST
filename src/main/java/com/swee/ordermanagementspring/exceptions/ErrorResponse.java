package com.swee.ordermanagementspring.exceptions;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(name = "ApiErrorResponse", description = "Erro retornado pela API")
public class ErrorResponse {

    @Schema(type = "string", format = "local-date-time", requiredMode = Schema.RequiredMode.REQUIRED,
            description = "Data/hora local do backend, sem offset; nao representa um instante UTC")
    private LocalDateTime timestamp;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "400", maximum = "599")
    private int status;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String error;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String message;
    @ArraySchema(arraySchema = @Schema(types = {"array", "null"}), schema = @Schema(type = "string"))
    private List<String> details;

    public ErrorResponse(int status, String error, String message ){
        this.timestamp = LocalDateTime.now();
        this.status = status;
        this.error = error;
        this.message = message;
    }

    public ErrorResponse(int status, String error, String message, List<String> details) {
        this(status, error, message);
        this.details = details;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public int getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }

    public String getMessage() {
        return message;
    }

    public List<String> getDetails() {
        return details;
    }
}
