package br.com.fiap.feedbackplatform.submitfeedback.handler;

import java.util.List;

/**
 * Body returned for client errors ({@code 400}).
 */
public record ErrorResponse(String error, List<String> details) {
}
