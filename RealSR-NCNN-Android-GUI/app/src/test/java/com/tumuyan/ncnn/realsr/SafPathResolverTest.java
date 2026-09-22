package com.tumuyan.ncnn.realsr;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link SafPathResolver} — the pure-Java twin of
 * {@link SafPathHelper}'s string logic.  Mirrors {@link SafPathHelperTest}
 * but runs without any Android classes, so it can be compiled with javac and
 * executed by test.sh on machines without an Android SDK.
 */
public class SafPathResolverTest {

    private static final String STORAGE_BASE = "/storage/emulated/0";

    // ---------------------------------------------------------------
    // resolveTreeDocIdToPath — primary storage
    // ---------------------------------------------------------------

    @Test
    public void resolveTreeDocIdToPath_primary_root() {
        assertEquals(STORAGE_BASE + "/",
                SafPathResolver.resolveTreeDocIdToPath("primary:", STORAGE_BASE));
    }

    @Test
    public void resolveTreeDocIdToPath_primary_subdir() {
        assertEquals(STORAGE_BASE + "/DCIM",
                SafPathResolver.resolveTreeDocIdToPath("primary:DCIM", STORAGE_BASE));
    }

    @Test
    public void resolveTreeDocIdToPath_primary_nested() {
        assertEquals(STORAGE_BASE + "/DCIM/Camera",
                SafPathResolver.resolveTreeDocIdToPath("primary:DCIM/Camera", STORAGE_BASE));
    }

    // ---------------------------------------------------------------
    // resolveTreeDocIdToPath — non-primary (SD card / USB-OTG)
    // ---------------------------------------------------------------

    @Test
    public void resolveTreeDocIdToPath_sdCard_subdir() {
        assertEquals("/storage/ABCD-1234/Photos",
                SafPathResolver.resolveTreeDocIdToPath("ABCD-1234:Photos", STORAGE_BASE));
    }

    @Test
    public void resolveTreeDocIdToPath_sdCard_root() {
        assertEquals("/storage/ABCD-1234/",
                SafPathResolver.resolveTreeDocIdToPath("ABCD-1234:", STORAGE_BASE));
    }

    @Test
    public void resolveTreeDocIdToPath_nonPrimary_doesNotUseStorageBase() {
        assertEquals("/storage/XYZ/path",
                SafPathResolver.resolveTreeDocIdToPath("XYZ:path", "/some/other/base"));
    }

    // ---------------------------------------------------------------
    // resolveTreeDocIdToPath — edge cases
    // ---------------------------------------------------------------

    @Test
    public void resolveTreeDocIdToPath_nullDocId_returnsEmpty() {
        assertEquals("", SafPathResolver.resolveTreeDocIdToPath(null, STORAGE_BASE));
    }

    @Test
    public void resolveTreeDocIdToPath_noColon_fallsBack() {
        assertEquals(STORAGE_BASE + "/some-doc-id",
                SafPathResolver.resolveTreeDocIdToPath("some-doc-id", STORAGE_BASE));
    }

    @Test
    public void resolveTreeDocIdToPath_primaryCaseSensitive() {
        assertEquals("/storage/Primary/DCIM",
                SafPathResolver.resolveTreeDocIdToPath("Primary:DCIM", STORAGE_BASE));
    }

    // ---------------------------------------------------------------
    // extractTreeDisplayName
    // ---------------------------------------------------------------

    @Test
    public void extractTreeDisplayName_primary_singleDir() {
        assertEquals("DCIM", SafPathResolver.extractTreeDisplayName("primary:DCIM"));
    }

    @Test
    public void extractTreeDisplayName_primary_nestedPath() {
        assertEquals("Camera", SafPathResolver.extractTreeDisplayName("primary:DCIM/Camera"));
    }

    @Test
    public void extractTreeDisplayName_sdCard() {
        assertEquals("Photos", SafPathResolver.extractTreeDisplayName("ABCD-1234:Photos"));
    }

    @Test
    public void extractTreeDisplayName_sdCard_nested() {
        assertEquals("Vacation", SafPathResolver.extractTreeDisplayName("ABCD-1234:Photos/Vacation"));
    }

    @Test
    public void extractTreeDisplayName_noColon() {
        assertEquals("some-doc", SafPathResolver.extractTreeDisplayName("some-doc"));
    }

    @Test
    public void extractTreeDisplayName_nullOrEmpty_returnsNull() {
        assertNull(SafPathResolver.extractTreeDisplayName(null));
        assertNull(SafPathResolver.extractTreeDisplayName(""));
    }

    // ---------------------------------------------------------------
    // isSafUriString
    // ---------------------------------------------------------------

    @Test
    public void isSafUriString_contentUri_isTrue() {
        assertTrue(SafPathResolver.isSafUriString(
                "content://com.android.externalstorage.documents/tree/primary%3ADCIM"));
    }

    @Test
    public void isSafUriString_filePath_isFalse() {
        assertFalse(SafPathResolver.isSafUriString("/storage/emulated/0/DCIM"));
        assertFalse(SafPathResolver.isSafUriString("file:///storage/emulated/0/DCIM"));
    }

    @Test
    public void isSafUriString_nullOrEmpty_isFalse() {
        assertFalse(SafPathResolver.isSafUriString(null));
        assertFalse(SafPathResolver.isSafUriString(""));
    }

    // ---------------------------------------------------------------
    // Regression scenario: SD card that used to fail with File.exists()
    // ---------------------------------------------------------------

    @Test
    public void regression_sdCard_pathAndDisplayName() {
        String path = SafPathResolver.resolveTreeDocIdToPath("ABCD-1234:DCIM", STORAGE_BASE);
        assertEquals("/storage/ABCD-1234/DCIM", path);
        assertEquals("DCIM", SafPathResolver.extractTreeDisplayName("ABCD-1234:DCIM"));
    }

    @Test
    public void regression_vendorProvider_nonStandardDocId() {
        String path = SafPathResolver.resolveTreeDocIdToPath("my-doc-folder", STORAGE_BASE);
        assertEquals(STORAGE_BASE + "/my-doc-folder", path);
    }
}
