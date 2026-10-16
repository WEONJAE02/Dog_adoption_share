package org.likelion.hsu.db_project.Service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ImageStorageTest {

    @TempDir
    Path tmp;

    private Path publicDir;
    private Path privateDir;
    private ImageStorage storage;

    @BeforeEach
    void setUp() {
        publicDir = tmp.resolve("uploads");
        privateDir = tmp.resolve("private-uploads");
        storage = new ImageStorage();
        ReflectionTestUtils.setField(storage, "uploadDir", publicDir.toString());
        ReflectionTestUtils.setField(storage, "privateDir", privateDir.toString());
    }

    private static MockMultipartFile jpg() {
        return new MockMultipartFile("f", "a.jpg", "image/jpeg", new byte[]{1, 2, 3});
    }

    @Test
    void 비공개_사진은_공개_폴더에_남지_않는다() throws Exception {
        String name = storage.storePrivate(jpg());

        assertTrue(Files.exists(privateDir.resolve(name)));
        assertFalse(Files.exists(publicDir.resolve(name)));
        assertArrayEquals(new byte[]{1, 2, 3}, storage.readPrivate(name));
        assertTrue(storage.privateFile(name).isPresent());

        storage.deletePrivate(name);
        assertFalse(Files.exists(privateDir.resolve(name)));
    }

    @Test
    void 폴더_밖을_가리키는_경로는_거부한다() throws Exception {
        Files.createDirectories(publicDir);
        Files.write(publicDir.resolve("public.jpg"), new byte[]{9});

        assertTrue(storage.privateFile("../uploads/public.jpg").isEmpty());
        assertTrue(storage.privateFile("").isEmpty());
        assertTrue(storage.privateFile(null).isEmpty());
        assertThrows(java.io.IOException.class, () -> storage.readPrivate("../uploads/public.jpg"));
    }

    @Test
    void 공개_폴더의_기존_사진을_비공개_폴더로_옮긴다() throws Exception {
        String name = storage.store(jpg());

        assertTrue(storage.moveToPrivate(name));
        assertFalse(Files.exists(publicDir.resolve(name)));
        assertTrue(Files.exists(privateDir.resolve(name)));
        assertFalse(storage.moveToPrivate(name));      // 이미 옮겼으면 건너뜀
        assertFalse(storage.moveToPrivate("없는파일.jpg"));
    }
}
