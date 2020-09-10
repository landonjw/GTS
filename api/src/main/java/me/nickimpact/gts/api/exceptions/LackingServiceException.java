package me.nickimpact.gts.api.exceptions;

public class LackingServiceException extends RuntimeException {

    public LackingServiceException(Class<?> service) {
        super(service.getCanonicalName());
    }

}
