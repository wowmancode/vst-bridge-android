package dev.vstbridge.android;

import static org.junit.Assert.assertEquals;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public final class PeInspectorTest {
    @Rule public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void detectsX8664() throws IOException {
        assertEquals(PeInspector.Architecture.X86_64, PeInspector.inspect(peFile(0x8664)));
    }

    @Test
    public void detectsX8632() throws IOException {
        assertEquals(PeInspector.Architecture.X86_32, PeInspector.inspect(peFile(0x014c)));
    }

    @Test
    public void detectsArm64() throws IOException {
        assertEquals(PeInspector.Architecture.ARM64, PeInspector.inspect(peFile(0xaa64)));
    }

    @Test
    public void rejectsNonPeFile() throws IOException {
        File file = temporaryFolder.newFile("not-a-plugin.dll");
        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(new byte[] {1, 2, 3, 4});
        }
        assertEquals(PeInspector.Architecture.NOT_PE, PeInspector.inspect(file));
    }

    @Test
    public void rejectsOutOfBoundsPeOffset() throws IOException {
        byte[] bytes = new byte[64];
        bytes[0] = 'M';
        bytes[1] = 'Z';
        bytes[0x3c] = (byte) 0xff;
        bytes[0x3d] = (byte) 0xff;
        File file = temporaryFolder.newFile("broken.dll");
        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(bytes);
        }
        assertEquals(PeInspector.Architecture.NOT_PE, PeInspector.inspect(file));
    }

    private File peFile(int machine) throws IOException {
        byte[] bytes = new byte[256];
        bytes[0] = 'M';
        bytes[1] = 'Z';
        bytes[0x3c] = (byte) 0x80;
        bytes[0x80] = 'P';
        bytes[0x81] = 'E';
        bytes[0x84] = (byte) (machine & 0xff);
        bytes[0x85] = (byte) ((machine >>> 8) & 0xff);
        File file = temporaryFolder.newFile(machine + ".dll");
        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(bytes);
        }
        return file;
    }
}
