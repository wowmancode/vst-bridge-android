package dev.vstbridge.android;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

final class PeInspector {
    enum Architecture {
        X86_64("x86-64", true),
        X86_32("x86 (32-bit)", false),
        ARM64("Windows ARM64", false),
        UNKNOWN("Unknown PE architecture", false),
        NOT_PE("Not a Windows PE file", false);

        final String label;
        final boolean supported;

        Architecture(String label, boolean supported) {
            this.label = label;
            this.supported = supported;
        }
    }

    private static final int DOS_MAGIC = 0x5a4d;
    private static final long PE_SIGNATURE = 0x00004550L;
    private static final int IMAGE_FILE_MACHINE_I386 = 0x014c;
    private static final int IMAGE_FILE_MACHINE_AMD64 = 0x8664;
    private static final int IMAGE_FILE_MACHINE_ARM64 = 0xaa64;
    private static final long MAX_PE_OFFSET = 64L * 1024L * 1024L;

    private PeInspector() {}

    static Architecture inspect(File file) {
        try (RandomAccessFile input = new RandomAccessFile(file, "r")) {
            if (input.length() < 64 || readU16LE(input) != DOS_MAGIC) {
                return Architecture.NOT_PE;
            }
            input.seek(0x3c);
            long peOffset = readU32LE(input);
            if (peOffset < 64 || peOffset > MAX_PE_OFFSET || peOffset + 6 > input.length()) {
                return Architecture.NOT_PE;
            }
            input.seek(peOffset);
            if (readU32LE(input) != PE_SIGNATURE) return Architecture.NOT_PE;
            int machine = readU16LE(input);
            if (machine == IMAGE_FILE_MACHINE_AMD64) return Architecture.X86_64;
            if (machine == IMAGE_FILE_MACHINE_I386) return Architecture.X86_32;
            if (machine == IMAGE_FILE_MACHINE_ARM64) return Architecture.ARM64;
            return Architecture.UNKNOWN;
        } catch (IOException ignored) {
            return Architecture.NOT_PE;
        }
    }

    private static int readU16LE(RandomAccessFile input) throws IOException {
        int low = input.read();
        int high = input.read();
        if ((low | high) < 0) throw new IOException("Unexpected end of file");
        return low | (high << 8);
    }

    private static long readU32LE(RandomAccessFile input) throws IOException {
        long low = readU16LE(input);
        long high = readU16LE(input);
        return low | (high << 16);
    }
}
