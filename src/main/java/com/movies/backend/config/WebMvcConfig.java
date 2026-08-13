package com.movies.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuração MVC extra: serve arquivos de disco como estáticos.
 *
 * <p>/uploads/** são as imagens de perfil; /library/covers/** são as capas
 * extraídas dos MP3 e dos PDFs. As capas ficam abertas de propósito: elas vão
 * dentro de {@code <img src>} espalhado pela grade toda, e um {@code <img>} não
 * manda header Authorization. O áudio e o PDF em si continuam protegidos — são
 * eles que valem alguma coisa, não a miniatura da capa.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final String libraryDir;

    public WebMvcConfig(@Value("${app.library.dir:./data/library}") String libraryDir) {
        this.libraryDir = libraryDir.endsWith("/") ? libraryDir : libraryDir + "/";
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:./data/uploads/");

        registry.addResourceHandler("/library/covers/**")
                .addResourceLocations("file:" + libraryDir + "covers/")
                // O nome do arquivo é um UUID, então o conteúdo nunca muda.
                .setCachePeriod(60 * 60 * 24 * 365);
    }
}
