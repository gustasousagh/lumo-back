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
 * Monta e envia os emails (confirmação e reset) via Spring Mail.
 *
 * Boas práticas aplicadas para não "quebrar" nos clientes de email:
 * - Layout em TABELAS (o único jeito confiável; div/flex/grid falham no Outlook).
 * - Cores sólidas inline (nada de gradiente, que some ou fica estranho).
 * - Botão "bulletproof" (célula de tabela colorida com link dentro).
 * - Versão em TEXTO PURO junto do HTML (multipart) -> melhora entrega/anti-spam.
 *
 * Se o envio falhar, o link é sempre logado no console para você testar mesmo assim.
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
        String firstName = firstName(user.getName());
        String subject = "🎬 " + firstName + ", confirma teu email e vem pro cineclube";

        String html = layout(
                "Falta só um clique pra você entrar no Movies.",
                "E aí, " + escape(firstName) + "! 🍿",
                "Que bom te ver por aqui. O <strong style=\"color:#ffffff;\">Movies</strong> é o nosso "
                        + "cantinho de filmes — privado, só pra galera. Confirma teu email pra liberar o acesso:",
                "Confirmar meu email",
                link,
                "Este link expira em 24 horas. Se não foi você que criou a conta, pode ignorar. 😉");

        String text = plain(
                "E aí, " + firstName + "!",
                "O Movies é o nosso cantinho de filmes, só pra galera. Confirme seu email para liberar o acesso:",
                "Confirmar meu email",
                link,
                "Este link expira em 24 horas. Se não foi você, ignore este email.");

        send(user.getEmail(), subject, html, text, "CONFIRMAÇÃO", link);
    }

    public void sendPasswordResetEmail(User user, String link) {
        String firstName = firstName(user.getName());
        String subject = "🔑 Redefinir sua senha do Movies";

        String html = layout(
                "Um link rapidinho pra você criar uma senha nova.",
                "Bora criar uma senha nova, " + escape(firstName) + "?",
                "Recebemos um pedido pra redefinir sua senha. É só clicar no botão abaixo "
                        + "e escolher uma nova:",
                "Criar nova senha",
                link,
                "Este link vale por 1 hora. Se não foi você que pediu, pode ignorar — "
                        + "sua senha atual continua valendo.");

        String text = plain(
                "Bora criar uma senha nova, " + firstName + "?",
                "Recebemos um pedido para redefinir sua senha. Use o link abaixo para escolher uma nova:",
                "Criar nova senha",
                link,
                "Este link vale por 1 hora. Se não foi você, pode ignorar.");

        send(user.getEmail(), subject, html, text, "RESET DE SENHA", link);
    }

    // ------------------------------------------------------------------- ENVIO
    private void send(String to, String subject, String html, String text, String kind, String link) {
        // Sempre loga o link -> útil enquanto o email não está configurado.
        log.info("\n============ EMAIL [{}] ============\n  para: {}\n  link: {}\n====================================", kind, to, link);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(appProperties.getMailFrom());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text, html); // (texto puro, html) -> multipart/alternative
            mailSender.send(message);
            log.info("Email de {} enviado para {}", kind, to);
        } catch (Exception ex) {
            log.warn("Não consegui enviar o email de {} para {} ({}). "
                    + "Use o link logado acima para continuar o teste.", kind, to, ex.getMessage());
        }
    }

    // ---------------------------------------------------------------- TEMPLATE
    /**
     * Layout base em tabelas. Usa tokens {{...}} trocados por .replace() de propósito
     * (nada de String.format aqui, para não conflitar com os "%" do CSS).
     */
    private String layout(String preheader, String heading, String bodyHtml,
                          String buttonLabel, String link, String note) {
        return TEMPLATE
                .replace("{{PREHEADER}}", escape(preheader))
                .replace("{{HEADING}}", heading)
                .replace("{{BODY}}", bodyHtml)
                .replace("{{BUTTON}}", escape(buttonLabel))
                .replace("{{LINK}}", link)
                .replace("{{NOTE}}", escape(note));
    }

    private String plain(String heading, String body, String buttonLabel, String link, String note) {
        return heading + "\n\n"
                + body + "\n\n"
                + buttonLabel + ": " + link + "\n\n"
                + note + "\n\n"
                + "— Movies · o cineclube dos amigos";
    }

    private static String firstName(String name) {
        if (name == null || name.isBlank()) return "amigo(a)";
        return name.trim().split("\\s+")[0];
    }

    /** Escapa HTML para nomes/textos dinâmicos não quebrarem o layout. */
    private static String escape(String value) {
        if (value == null) return "";
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private static final String TEMPLATE = """
            <!DOCTYPE html>
            <html lang="pt-BR">
            <head>
              <meta charset="utf-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <meta name="color-scheme" content="dark light">
              <title>Movies</title>
            </head>
            <body style="margin:0;padding:0;background-color:#0a0a0f;">
              <!-- preheader: texto de preview escondido -->
              <div style="display:none;max-height:0;overflow:hidden;opacity:0;">{{PREHEADER}}</div>

              <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="background-color:#0a0a0f;">
                <tr>
                  <td align="center" style="padding:32px 16px;">

                    <table role="presentation" width="480" cellpadding="0" cellspacing="0" border="0" style="width:480px;max-width:480px;background-color:#14141d;border:1px solid #2a2a3c;border-radius:16px;overflow:hidden;">
                      <!-- header -->
                      <tr>
                        <td style="padding:24px 32px;border-bottom:1px solid #2a2a3c;" bgcolor="#14141d">
                          <span style="font-family:Arial,Helvetica,sans-serif;font-size:20px;font-weight:800;letter-spacing:1px;color:#e11d48;">🎬 MOVIES</span>
                          <span style="font-family:Arial,Helvetica,sans-serif;font-size:11px;color:#9a9ab4;"> &nbsp;• só para amigos</span>
                        </td>
                      </tr>

                      <!-- corpo -->
                      <tr>
                        <td style="padding:32px;" bgcolor="#14141d">
                          <h1 style="margin:0 0 14px;font-family:Arial,Helvetica,sans-serif;font-size:22px;line-height:1.3;color:#ffffff;">{{HEADING}}</h1>
                          <p style="margin:0 0 28px;font-family:Arial,Helvetica,sans-serif;font-size:15px;line-height:1.65;color:#c4c4d4;">{{BODY}}</p>

                          <!-- botão bulletproof -->
                          <table role="presentation" cellpadding="0" cellspacing="0" border="0" style="margin:0 0 28px;">
                            <tr>
                              <td align="center" bgcolor="#e11d48" style="border-radius:12px;">
                                <a href="{{LINK}}" target="_blank" style="display:inline-block;padding:14px 32px;font-family:Arial,Helvetica,sans-serif;font-size:16px;font-weight:bold;color:#ffffff;text-decoration:none;border-radius:12px;">{{BUTTON}}</a>
                              </td>
                            </tr>
                          </table>

                          <!-- aviso de expiração -->
                          <p style="margin:0 0 24px;font-family:Arial,Helvetica,sans-serif;font-size:13px;line-height:1.6;color:#9a9ab4;">{{NOTE}}</p>

                          <!-- link de fallback -->
                          <p style="margin:0;font-family:Arial,Helvetica,sans-serif;font-size:12px;line-height:1.6;color:#6b6b80;">
                            Se o botão não funcionar, copie e cole este link no navegador:<br>
                            <a href="{{LINK}}" target="_blank" style="color:#f43f5e;word-break:break-all;">{{LINK}}</a>
                          </p>
                        </td>
                      </tr>

                      <!-- footer -->
                      <tr>
                        <td style="padding:20px 32px;border-top:1px solid #2a2a3c;" bgcolor="#111119">
                          <p style="margin:0;font-family:Arial,Helvetica,sans-serif;font-size:12px;color:#6b6b80;">
                            Enviado com 🍿 pelo <strong style="color:#9a9ab4;">Movies</strong> — o cineclube dos amigos.
                          </p>
                        </td>
                      </tr>
                    </table>

                    <p style="margin:20px 0 0;font-family:Arial,Helvetica,sans-serif;font-size:11px;color:#4a4a5a;">
                      Você recebeu este email porque alguém usou seu endereço no Movies.
                    </p>

                  </td>
                </tr>
              </table>
            </body>
            </html>
            """;
}
