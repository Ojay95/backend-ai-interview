package com.ai_interview.common.exception;


import lombok.Getter;
import org.springframework.http.HttpStatus;

import static io.lettuce.core.pubsub.PubSubOutput.Type.message;

@Getter
public class InterviewException extends RuntimeException {

    private final HttpStatus status;

    public InterviewException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    // Helper for common 400 errors
    public static InterviewException badRequest(String message) {
        return new InterviewException(message, HttpStatus.BAD_REQUEST);
    }

    // Helper for common 404 errors
    public static InterviewException notFound(String message) {
        return new InterviewException(message, HttpStatus.NOT_FOUND);
    }


    public static InterviewException internalError(String message) {
        return new InterviewException(message, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
