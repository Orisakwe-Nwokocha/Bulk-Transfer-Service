package dev.orisha.bulk_transfer_service.constants;

import lombok.Getter;

@Getter
public enum ResponseCodes {

    SUCCESS("00", "Approved or completed successfully"),
    FAILED("99", "Failed to process request");

    private final String code;
    private final String description;

    ResponseCodes(final String code, final String description) {
        this.code = code;
        this.description = description;
    }

}
