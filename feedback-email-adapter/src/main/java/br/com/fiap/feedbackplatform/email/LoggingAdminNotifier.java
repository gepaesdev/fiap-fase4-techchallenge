package br.com.fiap.feedbackplatform.email;

import br.com.fiap.feedbackplatform.core.notification.AdminNotification;
import br.com.fiap.feedbackplatform.core.port.AdminNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Local-development stand-in for e-mail delivery: there is no Azure Communication Services emulator, so the
 * message is written to the log instead. Never enabled in Azure, where logs must not contain student text.
 */
public class LoggingAdminNotifier implements AdminNotifier {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingAdminNotifier.class);

    private final List<String> recipients;

    public LoggingAdminNotifier(List<String> recipients) {
        this.recipients = List.copyOf(recipients);
    }

    @Override
    public void notify(AdminNotification notification) {
        LOGGER.info("[LOCAL E-MAIL] to={} subject=\"{}\"\n{}",
                recipients, notification.subject(), notification.plainTextBody());
    }
}
