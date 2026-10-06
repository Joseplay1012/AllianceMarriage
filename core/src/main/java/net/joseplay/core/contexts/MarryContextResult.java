package net.joseplay.core.contexts;

import java.util.UUID;

public class MarryContextResult {
    private boolean divorced = false;
    private boolean marred = false;
    private boolean error = false;
    private String errorMessage = null;
    private MarryErrorType errorType;


    private final UUID partner1UUID;
    private final UUID partner2UUID;

    public MarryContextResult(UUID partner1UUID, UUID partner2UUID) {
        this.partner1UUID = partner1UUID;
        this.partner2UUID = partner2UUID;
    }

    public boolean isMarred() {
        return marred;
    }

    public void setMarred(boolean marred) {
        this.marred = marred;
    }

    public boolean isError() {
        return error;
    }

    public void setError(boolean error) {
        this.error = error;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public UUID getPartner1UUID() {
        return partner1UUID;
    }

    public UUID getPartner2UUID() {
        return partner2UUID;
    }


    public MarryErrorType getErrorType() {
        return errorType;
    }

    public void setErrorType(MarryErrorType errorType) {
        this.errorType = errorType;
    }



    public boolean isDivorced() {
        return divorced;
    }

    public void setDivorced(boolean divorced) {
        this.divorced = divorced;
    }

    public static MarryContextResult error(
            UUID playerUUID,
            UUID partnerUUID,
            MarryContextResult.MarryErrorType type,
            String message
    ) {
        MarryContextResult result = new MarryContextResult(
                playerUUID,
                partnerUUID
        );

        result.setError(true);
        result.setErrorMessage(message);
        result.setErrorType(type);

        return result;
    }

    public static MarryContextResult marry(UUID playerUUID, UUID partnerUUID) {
        MarryContextResult result = new MarryContextResult(playerUUID, partnerUUID);
        result.setMarred(true);

        return result;
    }

    public static MarryContextResult divorce(UUID playerUUID, UUID partnerUUID) {
        MarryContextResult result = new MarryContextResult(playerUUID, partnerUUID);
        result.setDivorced(true);

        return result;
    }

    public enum MarryErrorType {
        ALREADY_MARRIED,
        PARTNER_ALREADY_MARRIED,
        NOT_MARRIED

    }
}
