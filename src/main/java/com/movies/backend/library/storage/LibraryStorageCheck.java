package com.movies.backend.library.storage;

import com.movies.backend.library.repository.BookRepository;
import com.movies.backend.library.repository.TrackRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Confere, no arranque, se o diretório da biblioteca sobreviveu ao deploy.
 *
 * <p>Por que isto existe: o banco é um serviço separado e persiste sozinho, mas
 * os arquivos moram no disco do container. Se esse disco não estiver num volume
 * de verdade, o resultado é o pior tipo de falha — silenciosa e enganosa. O
 * catálogo continua listando as faixas (os registros estão no MySQL), a capa
 * aparece, e o play simplesmente não toca. Ninguém liga isso a "deploy".
 *
 * <p>Detectar é simples: gravamos um arquivo com um identificador na primeira
 * vez. Se ele ainda estiver lá no arranque seguinte, o armazenamento é
 * persistente. Se sumiu e o banco tem registros, o volume é descartável e o
 * acervo já se perdeu — e aí o log grita, em vez de deixar você descobrir pelo
 * usuário reclamando.
 *
 * <p>Só observa e avisa: nada é apagado. Um volume desmontado por engano é
 * temporário, e limpar os registros "órfãos" transformaria um susto em perda
 * definitiva.
 */
@Component
// Depois do AdminBootstrap: mensagem de acervo perdido é o que deve ficar por
// último no log, não no meio das linhas de inicialização.
@Order(100)
public class LibraryStorageCheck implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(LibraryStorageCheck.class);

    /** Fica na raiz da biblioteca, junto das pastas de mídia. */
    private static final String MARKER = ".storage-id";

    private final LibraryStorage storage;
    private final TrackRepository trackRepository;
    private final BookRepository bookRepository;

    public LibraryStorageCheck(LibraryStorage storage,
                               TrackRepository trackRepository,
                               BookRepository bookRepository) {
        this.storage = storage;
        this.trackRepository = trackRepository;
        this.bookRepository = bookRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        Path root = storage.root();
        Path marker = root.resolve(MARKER);

        try {
            Files.createDirectories(root);

            if (Files.exists(marker)) {
                log.info("Biblioteca: armazenamento persistente confirmado em {} (id {})",
                        root, Files.readString(marker).trim());
                return;
            }

            String id = UUID.randomUUID().toString();
            Files.writeString(marker, id);

            long files = trackRepository.count() + bookRepository.count();
            if (files == 0) {
                log.info("Biblioteca: primeira inicialização em {} (id {})", root, id);
                return;
            }

            // Banco com registros + diretório zerado = os arquivos se foram.
            log.error("""

                    ===========================================================================
                     ACERVO PERDIDO — o armazenamento da biblioteca NÃO é persistente.

                     O banco tem {} registro(s) de música/livro, mas {} está vazio.
                     Os arquivos foram embora no último deploy; os registros ficaram, então o
                     catálogo vai listar faixas que não tocam e livros que não abrem.

                     Causa quase certa: nenhum volume montado nesse caminho. Em Docker Swarm,
                     um volume anônimo é recriado a cada task — ou seja, a cada deploy.

                     Como resolver: monte um volume NOMEADO em /app/data no painel de deploy
                     e faça o deploy de novo. Depois disso, esta mensagem não volta.
                    ===========================================================================
                    """, files, root);

        } catch (Exception ex) {
            // Não conseguir nem escrever o marcador é o outro modo de falha: o
            // ponto de montagem existe mas pertence a outro usuário (bind mount
            // criado pelo root, com o app rodando como uid 1001). Sem este aviso,
            // o sintoma só apareceria no primeiro upload, como erro genérico.
            log.error("Biblioteca: não consigo escrever em {} — todo upload vai falhar. "
                    + "Se for bind mount, rode: chown -R 1001:1001 <pasta-do-host>. Causa: {}",
                    root, ex.toString());
        }
    }
}
