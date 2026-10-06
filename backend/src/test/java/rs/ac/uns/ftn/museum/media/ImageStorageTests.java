package rs.ac.uns.ftn.museum.media;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.nio.file.Path;
import static org.assertj.core.api.Assertions.*;
import rs.ac.uns.ftn.museum.config.LocalImageSeeder;

class ImageStorageTests {
    @TempDir Path root;
    @Test void validPngRoundTripAndSafeFilename(){
        var s=new ImageStorage(root.toString(),5242880);
        var bytes=s.validate(new MockMultipartFile("file","../../secret.png","image/png",LocalImageSeeder.illustration(1)));
        String key=s.privateKey();s.write(key,bytes);assertThat(s.read(key)).isEqualTo(bytes);
        assertThatThrownBy(()->s.read("../secret.png")).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(()->s.fromReference("private-image:exhibit-1.png")).isInstanceOf(ResponseStatusException.class);
    }
    @Test void rejectsMimeSpoofingSvgCorruptAndOversize(){
        var s=new ImageStorage(root.toString(),100000);
        for(var f:new MockMultipartFile[]{new MockMultipartFile("file","x.svg","image/svg+xml","<svg/>".getBytes()),new MockMultipartFile("file","x.png","image/png","invalid".getBytes()),new MockMultipartFile("file","x.jpg","image/jpeg",LocalImageSeeder.illustration(2))})
            assertThatThrownBy(()->s.validate(f)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(()->s.validate(new MockMultipartFile("file","x.png","image/png",new byte[100001])))
            .isInstanceOfSatisfying(ResponseStatusException.class,e->assertThat(e.getStatusCode().value()).isEqualTo(413));
    }
    @Test void acceptsJpegAndRejectsOversizedDimensions()throws Exception {
        var s=new ImageStorage(root.toString(),5242880);
        var out=new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(12,10,java.awt.image.BufferedImage.TYPE_INT_RGB),"jpeg",out);
        byte[] png=s.validate(new MockMultipartFile("file","image.jpg","image/jpeg",out.toByteArray()));
        assertThat(javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(png)).getWidth()).isEqualTo(12);
        out.reset();javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(6001,1,java.awt.image.BufferedImage.TYPE_INT_RGB),"png",out);
        assertThatThrownBy(()->s.validate(new MockMultipartFile("file","wide.png","image/png",out.toByteArray()))).isInstanceOf(ResponseStatusException.class);
    }
}
