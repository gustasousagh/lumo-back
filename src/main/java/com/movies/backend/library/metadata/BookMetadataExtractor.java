package com.movies.backend.library.metadata;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.util.Calendar;
import javax.imageio.ImageIO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Lê o dicionário de metadados de um PDF (título, autor, assunto, palavras-chave,
 * data) e renderiza a primeira página como capa.
 *
 * <p>PDF não tem campo de capa, então a primeira página é o melhor palpite —
 * acerta em livro diagramado e em digitalização, que é o caso normal. A imagem
 * sai em 96 DPI e é reduzida para no máximo 800px de altura: capa é miniatura de
 * grade, não precisa de resolução de leitura, e 300 DPI geraria arquivos de
 * vários MB por livro.
 *
 * <p>Como no áudio, nada aqui lança para fora: PDF protegido por senha, corrompido
 * ou com fontes exóticas devolve o que der e o livro entra assim mesmo.
 */
@Component
public class BookMetadataExtractor {

    private static final Logger log = LoggerFactory.getLogger(BookMetadataExtractor.class);

    /** Altura máxima da capa gerada, em pixels. */
    private static final int COVER_MAX_HEIGHT = 800;

    public BookMetadata extract(Path file) {
        try (PDDocument document = Loader.loadPDF(file.toFile())) {
            PDDocumentInformation info = document.getDocumentInformation();
            int pages = document.getNumberOfPages();

            return new BookMetadata(
                    clean(info.getTitle()),
                    clean(info.getAuthor()),
                    // PDF nao tem campo de editora. getProducer() e o software que
                    // gerou o arquivo ("Adobe PDF Library"), nao a editora -- usar
                    // isso encheria a ficha de todo livro com lixo. Fica vazio para
                    // o admin preencher no painel.
                    null,
                    clean(info.getSubject()),
                    clean(info.getKeywords()),
                    yearOf(info),
                    pages > 0 ? pages : null,
                    renderCover(document),
                    "image/jpeg");
        } catch (Exception ex) {
            log.debug("Não deu para ler o PDF {}: {}", file.getFileName(), ex.toString());
            return BookMetadata.empty();
        }
    }

    /** Primeira página como JPEG. Devolve null se o render falhar. */
    private byte[] renderCover(PDDocument document) {
        if (document.getNumberOfPages() == 0) {
            return null;
        }
        try {
            PDFRenderer renderer = new PDFRenderer(document);
            BufferedImage page = renderer.renderImageWithDPI(0, 96, ImageType.RGB);
            BufferedImage scaled = downscale(page);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(scaled, "jpg", out);
            return out.size() > 0 ? out.toByteArray() : null;
        } catch (Exception ex) {
            log.debug("Render da capa falhou: {}", ex.toString());
            return null;
        }
    }

    private BufferedImage downscale(BufferedImage source) {
        int height = source.getHeight();
        if (height <= COVER_MAX_HEIGHT) {
            return source;
        }
        double ratio = (double) COVER_MAX_HEIGHT / height;
        int targetWidth = Math.max(1, (int) Math.round(source.getWidth() * ratio));

        BufferedImage target = new BufferedImage(targetWidth, COVER_MAX_HEIGHT, BufferedImage.TYPE_INT_RGB);
        var graphics = target.createGraphics();
        try {
            graphics.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                    java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.drawImage(source, 0, 0, targetWidth, COVER_MAX_HEIGHT, null);
        } finally {
            graphics.dispose();
        }
        return target;
    }

    private Integer yearOf(PDDocumentInformation info) {
        Calendar created = info.getCreationDate();
        if (created == null) {
            return null;
        }
        int year = created.get(Calendar.YEAR);
        return (year > 1400 && year < 2200) ? year : null;
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.replace("\u0000", "").trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
