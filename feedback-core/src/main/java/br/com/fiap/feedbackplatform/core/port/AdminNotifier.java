package br.com.fiap.feedbackplatform.core.port;

import br.com.fiap.feedbackplatform.core.notification.AdminNotification;

/**
 * Output port for delivering a message to the platform administrators. Who the administrators are is a
 * configuration concern of the adapter, not of the use cases.
 */
public interface AdminNotifier {

    /**
     * @throws RuntimeException when the message could not be delivered, so the caller's trigger can retry
     */
    void notify(AdminNotification notification);
}
