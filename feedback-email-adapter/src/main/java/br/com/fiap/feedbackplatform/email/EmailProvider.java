package br.com.fiap.feedbackplatform.email;

/**
 * Where administrator notifications are delivered.
 */
public enum EmailProvider {

    /** Real delivery through Azure Communication Services Email. */
    AZURE,

    /** Writes the message to the application log instead of sending it — local development only. */
    LOG
}
