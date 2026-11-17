package com.pnc.masters.customer.application;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class CompanyLogoBlobTest {

    private static final byte[] PNG = new byte[] {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x01
    };

    @Test
    void rawImageIsUsedAsStored() {
        CompanyLogoBlob.Image image = CompanyLogoBlob.decode(PNG).orElseThrow();

        assertThat(image.mediaType()).isEqualTo(MediaType.IMAGE_PNG);
        assertThat(image.bytes()).containsExactly(PNG);
    }

    @Test
    void accessPackageUnwrapsTheEmbeddedImage() {
        CompanyLogoBlob.Image image = CompanyLogoBlob.decode(accessPackage(PNG)).orElseThrow();

        assertThat(image.mediaType()).isEqualTo(MediaType.IMAGE_PNG);
        assertThat(image.bytes()).startsWith((byte) 0x89, (byte) 0x50, (byte) 0x4E, (byte) 0x47);
    }

    @Test
    void windows1252Utf8PackageDecodesTheEmbeddedDib() {
        byte[] dib = new byte[44];
        dib[0] = 40;
        dib[4] = 1;
        dib[8] = 1;
        dib[12] = 1;
        dib[14] = 24;
        byte[] packaged = accessPackage(dib);
        packaged[16] = (byte) 0xFF;

        CompanyLogoBlob.Image image = CompanyLogoBlob.decode(asUtf8Windows1252(packaged)).orElseThrow();

        assertThat(image.mediaType().toString()).isEqualTo("image/bmp");
        assertThat(image.bytes()).startsWith((byte) 'B', (byte) 'M');
    }

    @Test
    void blankOrUnknownBytesProduceNoPreview() {
        assertThat(CompanyLogoBlob.decode(null)).isEmpty();
        assertThat(CompanyLogoBlob.decode(new byte[0])).isEmpty();
        assertThat(CompanyLogoBlob.decode("not an image".getBytes(StandardCharsets.US_ASCII))).isEmpty();
    }

    private static byte[] accessPackage(byte[] payload) {
        byte[] packaged = new byte[20 + 12 + 8 + 4 + payload.length];
        packaged[0] = 0x15;
        packaged[1] = 0x1C;
        System.arraycopy(payload, 0, packaged, packaged.length - payload.length, payload.length);
        put32(packaged, 20 + 12 + 8, payload.length);
        return packaged;
    }

    private static byte[] asUtf8Windows1252(byte[] original) {
        StringBuilder text = new StringBuilder(original.length);
        for (byte value : original) {
            text.append((char) (value & 0xFF));
        }
        return text.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static void put32(byte[] data, int offset, int value) {
        data[offset] = (byte) value;
        data[offset + 1] = (byte) (value >>> 8);
        data[offset + 2] = (byte) (value >>> 16);
        data[offset + 3] = (byte) (value >>> 24);
    }
}
