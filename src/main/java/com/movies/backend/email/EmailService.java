package com.movies.backend.email;

import com.movies.backend.config.AppProperties;
import com.movies.backend.user.entity.User;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Responsável por montar e enviar os emails (confirmação e reset) via Spring Mail.
 * Se o envio falhar (ex.: credenciais ainda não configuradas), o link é sempre
 * logado no console para você conseguir testar o fluxo mesmo assim.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final AppProperties appProperties;

    public EmailService(JavaMailSender mailSender, AppProperties appProperties) {
        this.mailSender = mailSender;
        this.appProperties = appProperties;
    }

    public void sendVerificationEmail(User user, String link) {
        String subject = "Confirme seu email • Movies";
        String html = buildTemplate(
                "Quase lá, " + user.getName() + "!",
                "Confirme seu email para liberar seu acesso ao catálogo de filmes dos amigos.",
                "Confirmar meu email",
                link);
        send(user.getEmail(), subject, html, "CONFIRMAÇÃO", link);
    }

    public void sendPasswordResetEmail(User user, String link) {
        String subject = "Redefinição de senha • Movies";
        String html = buildTemplate(
                "Redefinir senha",
                "Recebemos um pedido para redefinir sua senha. O link vale por 1 hora. "
                        + "Se não foi você, pode ignorar este email.",
                "Criar nova senha",
                link);
        send(user.getEmail(), subject, html, "RESET DE SENHA", link);
    }

    private void send(String to, String subject, String html, String kind, String link) {
        // Sempre loga o link -> útil enquanto o email não está configurado.
        log.info("\n============ EMAIL [{}] ============\n  para: {}\n  link: {}\n====================================", kind, to, link);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(appProperties.getMailFrom());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
            log.info("Email de {} enviado para {}", kind, to);
        } catch (Exception ex) {
            log.warn("Não consegui enviar o email de {} para {} ({}). "
                    + "Use o link logado acima para continuar o teste.", kind, to, ex.getMessage());
        }
    }

    private String buildTemplate(String title, String message, String buttonLabel, String link) {
        return """
                <div style="background:#0b0b0f;padding:40px 0;font-family:Arial,Helvetica,sans-serif;">
                  <div style="max-width:480px;margin:0 auto;background:#15151d;border:1px solid #26263a;border-radius:16px;overflow:hidden;">
                    <div style="padding:28px 32px;border-bottom:1px solid #26263a;">
                      <span style="color:#e11d48;font-size:20px;font-weight:800;letter-spacing:.5px;">MOVIES</span>
                    </div>
                    <div style="padding:32px;">
                      <h1 style="color:#fafafa;font-size:22px;margin:0 0 12px;">%s</h1>
                      <p style="color:#a1a1b5;font-size:15px;line-height:1.6;margin:0 0 28px;">%s</p>
                      <a href="%s" style="display:inline-block;background:#e11d48;color:#fff;text-decoration:none;font-weight:700;font-size:15px;padding:13px 26px;border-radius:10px;">%s</a>
                      <p style="color:#6b6b80;font-size:12px;line-height:1.6;margin:28px 0 0;">Se o botão não funcionar, copie e cole este link no navegador:<br><span style="color:#8b8ba0;">%s</span></p>
                    </div>
                  </div>
                </div>
                """.formatted(title, message, link, buttonLabel, link);
    }
}
