package com.pnc.masters.customer.application;

import org.springframework.http.MediaType;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

/**
 * Turns a stored company_blob_logo into image bytes a browser can show.
 * Legacy rows are Access OLE packages, not raw image files. This never writes the column.
 */
public final class CompanyLogoBlob {

    private CompanyLogoBlob() {
    }

    public record Image(byte[] bytes, MediaType mediaType) {
    }

    private static final Map<Integer, Integer> WINDOWS_1252_BYTES = Map.ofEntries(
            Map.entry(0x20AC, 0x80), Map.entry(0x201A, 0x82), Map.entry(0x0192, 0x83), Map.entry(0x201E, 0x84),
            Map.entry(0x2026, 0x85), Map.entry(0x2020, 0x86), Map.entry(0x2021, 0x87), Map.entry(0x02C6, 0x88),
            Map.entry(0x2030, 0x89), Map.entry(0x0160, 0x8A), Map.entry(0x2039, 0x8B), Map.entry(0x0152, 0x8C),
            Map.entry(0x017D, 0x8E), Map.entry(0x2018, 0x91), Map.entry(0x2019, 0x92), Map.entry(0x201C, 0x93),
            Map.entry(0x201D, 0x94), Map.entry(0x2022, 0x95), Map.entry(0x2013, 0x96), Map.entry(0x2014, 0x97),
            Map.entry(0x02DC, 0x98), Map.entry(0x2122, 0x99), Map.entry(0x0161, 0x9A), Map.entry(0x203A, 0x9B),
            Map.entry(0x0153, 0x9C), Map.entry(0x017E, 0x9E), Map.entry(0x0178, 0x9F)
    );

    static Optional<Image> decode(byte[] stored) {
        if (stored == null || stored.length == 0) {
            return Optional.empty();
        }
        Optional<Image> direct = decodeBytes(stored);
        if (direct.isPresent()) {
            return direct;
        }
        byte[] restored = legacyWindowsBytes(stored);
        if (restored == null) {
            return Optional.empty();
        }
        return decodeBytes(restored);
    }

    private static Optional<Image> decodeBytes(byte[] stored) {
        byte[] payload = accessPackagePayload(stored).orElse(stored);
        return sniff(payload).or(() -> sniff(stored));
    }

    /**
     * Some imported blobs are the original bytes read as Windows-1252 and stored as UTF-8.
     * Raw image bytes are not valid UTF-8, so this leaves those rows unchanged.
     */
    private static byte[] legacyWindowsBytes(byte[] stored) {
        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        String text;
        try {
            text = decoder.decode(ByteBuffer.wrap(stored)).toString();
        } catch (CharacterCodingException exception) {
            return null;
        }
        byte[] restored = new byte[text.length()];
        int length = 0;
        for (int index = 0; index < text.length(); ) {
            int codePoint = text.codePointAt(index);
            index += Character.charCount(codePoint);
            if (codePoint <= 0xFF) {
                restored[length++] = (byte) codePoint;
                continue;
            }
            Integer original = WINDOWS_1252_BYTES.get(codePoint);
            if (original == null) {
                return null;
            }
            restored[length++] = original.byteValue();
        }
        if (length == stored.length) {
            return null;
        }
        byte[] bytes = new byte[length];
        System.arraycopy(restored, 0, bytes, 0, length);
        return bytes;
    }

    private static Optional<byte[]> accessPackagePayload(byte[] stored) {
        if (stored.length < 20 || u16(stored, 0) != 0x1C15) {
            return Optional.empty();
        }
        int friendlyLength = u16(stored, 8);
        int classLength = u16(stored, 10);
        int cursor = 20;
        if (!fits(stored, cursor, friendlyLength + classLength)) {
            return Optional.empty();
        }
        cursor += friendlyLength + classLength;
        if (!fits(stored, cursor, 12)) {
            return Optional.empty();
        }
        int typeNameLength = s32(stored, cursor + 8);
        cursor += 12;
        if (typeNameLength < 0 || !fits(stored, cursor, typeNameLength + 12)) {
            return Optional.empty();
        }
        cursor += typeNameLength + 8;
        int dataLength = s32(stored, cursor);
        cursor += 4;
        if (dataLength <= 0 || !fits(stored, cursor, dataLength)) {
            return Optional.empty();
        }
        byte[] payload = new byte[dataLength];
        System.arraycopy(stored, cursor, payload, 0, dataLength);
        return Optional.of(payload);
    }

    private static Optional<Image> sniff(byte[] data) {
        int jpeg = indexOf(data, new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});
        if (jpeg >= 0) {
            return image(data, jpeg, data.length - jpeg, MediaType.IMAGE_JPEG);
        }
        int png = indexOf(data, new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A});
        if (png >= 0) {
            return image(data, png, data.length - png, MediaType.IMAGE_PNG);
        }
        int gif = indexOf(data, new byte[] {'G', 'I', 'F', '8'});
        if (gif >= 0) {
            return image(data, gif, data.length - gif, MediaType.IMAGE_GIF);
        }
        int webp = indexOf(data, new byte[] {'R', 'I', 'F', 'F'});
        if (webp >= 0 && webp + 12 <= data.length
                && data[webp + 8] == 'W' && data[webp + 9] == 'E' && data[webp + 10] == 'B' && data[webp + 11] == 'P') {
            return image(data, webp, data.length - webp, MediaType.parseMediaType("image/webp"));
        }
        int bmp = indexOf(data, new byte[] {'B', 'M'});
        if (bmp >= 0 && bmp + 6 <= data.length) {
            int size = s32(data, bmp + 2);
            if (size >= 54 && bmp + size <= data.length) {
                return image(data, bmp, size, MediaType.parseMediaType("image/bmp"));
            }
        }
        return wrapDib(data).map(bytes -> new Image(bytes, MediaType.parseMediaType("image/bmp")));
    }

    private static Optional<byte[]> wrapDib(byte[] dib) {
        if (dib.length < 40) {
            return Optional.empty();
        }
        int headerSize = s32(dib, 0);
        if (headerSize != 40 && headerSize != 108 && headerSize != 124) {
            return Optional.empty();
        }
        int width = s32(dib, 4);
        int height = s32(dib, 8);
        if (width <= 0 || width > 20000 || height == 0 || Math.abs(height) > 20000) {
            return Optional.empty();
        }
        int bitCount = u16(dib, 14);
        if (bitCount != 1 && bitCount != 4 && bitCount != 8 && bitCount != 16 && bitCount != 24 && bitCount != 32) {
            return Optional.empty();
        }
        int compression = s32(dib, 16);
        int colorsUsed = s32(dib, 32);
        if (colorsUsed < 0 || colorsUsed > 256) {
            return Optional.empty();
        }
        int colors = colorsUsed > 0 ? colorsUsed : (bitCount <= 8 ? 1 << bitCount : 0);
        int masks = compression == 3 ? 12 : (compression == 6 ? 16 : 0);
        int pixelOffset = 14 + headerSize + masks + colors * 4;
        if (pixelOffset > 14 + dib.length) {
            return Optional.empty();
        }
        byte[] file = new byte[14 + dib.length];
        file[0] = 'B';
        file[1] = 'M';
        put32(file, 2, file.length);
        put32(file, 10, pixelOffset);
        System.arraycopy(dib, 0, file, 14, dib.length);
        return Optional.of(file);
    }

    private static Optional<Image> image(byte[] data, int offset, int length, MediaType mediaType) {
        byte[] bytes = new byte[length];
        System.arraycopy(data, offset, bytes, 0, length);
        return Optional.of(new Image(bytes, mediaType));
    }

    private static int indexOf(byte[] data, byte[] needle) {
        int last = data.length - needle.length;
        for (int i = 0; i <= last; i++) {
            boolean match = true;
            for (int j = 0; j < needle.length; j++) {
                if (data[i + j] != needle[j]) {
                    match = false;
                    break;
                }
            }
            if (match) {
                return i;
            }
        }
        return -1;
    }

    private static boolean fits(byte[] data, int offset, int length) {
        return length >= 0 && offset >= 0 && offset <= data.length - length;
    }

    private static int u16(byte[] data, int offset) {
        return (data[offset] & 0xFF) | ((data[offset + 1] & 0xFF) << 8);
    }

    private static int s32(byte[] data, int offset) {
        return (data[offset] & 0xFF)
                | ((data[offset + 1] & 0xFF) << 8)
                | ((data[offset + 2] & 0xFF) << 16)
                | ((data[offset + 3] & 0xFF) << 24);
    }

    private static void put32(byte[] data, int offset, int value) {
        data[offset] = (byte) value;
        data[offset + 1] = (byte) (value >>> 8);
        data[offset + 2] = (byte) (value >>> 16);
        data[offset + 3] = (byte) (value >>> 24);
    }
}
