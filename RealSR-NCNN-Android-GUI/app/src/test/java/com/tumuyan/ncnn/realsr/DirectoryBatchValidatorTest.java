package com.tumuyan.ncnn.realsr;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Regression tests for {@link DirectoryBatchValidator}.
 * <p>
 * Background: DirectoryProcessActivity used to wrap the displayed path string
 * in {@code new File(path)} and check exists()/isDirectory() to decide whether
 * the Start button is enabled and whether the batch may launch.  For SAF-picked
 * directories on non-primary storage (SD card, USB-OTG) and several vendor
 * document providers, File.exists() returns false even though the app holds a
 * valid URI grant — the button stayed disabled or the launch was blocked.
 * <p>
 * These tests pin the SAF-first validation rules using a fake PathProbe, so
 * they run on a plain JVM without the Android framework.
 */
public class DirectoryBatchValidatorTest {

    /** Probe simulating a path that exists on the filesystem as a directory. */
    private static final DirectoryBatchValidator.PathProbe EXISTING_DIR_PROBE =
            new DirectoryBatchValidator.PathProbe() {
                @Override
                public boolean exists(String path) {
                    return true;
                }

                @Override
                public boolean isDirectory(String path) {
                    return true;
                }
            };

    /**
     * Probe simulating the regression scenario: the filesystem path derived
     * from a SAF tree URI is NOT visible to java.io.File (SD card, vendor
     * provider, URI-only provider).
     */
    private static final DirectoryBatchValidator.PathProbe INACCESSIBLE_PROBE =
            new DirectoryBatchValidator.PathProbe() {
                @Override
                public boolean exists(String path) {
                    return false;
                }

                @Override
                public boolean isDirectory(String path) {
                    return false;
                }
            };

    // ---------------------------------------------------------------
    // Input validation — SAF path (the regression)
    // ---------------------------------------------------------------

    @Test
    public void input_safUriValid_fileInaccessible_isValid() {
        // Core regression: SAF grant is valid but File.exists() would fail.
        // The old code blocked this; it must now be accepted.
        assertTrue(DirectoryBatchValidator.isInputValid(
                true, true, "/storage/ABCD-1234/DCIM", INACCESSIBLE_PROBE));
    }

    @Test
    public void input_safUriValid_uriOnlyPath_isValid() {
        // Provider returned no mappable filesystem path — the field shows the
        // raw content:// URI.  File probing must not be consulted at all.
        assertTrue(DirectoryBatchValidator.isInputValid(
                true, true,
                "content://com.android.externalstorage.documents/tree/ABCD-1234%3ADCIM",
                INACCESSIBLE_PROBE));
    }

    @Test
    public void input_safUriInvalid_isRejected() {
        // Grant revoked or provider gone → must be rejected even if a File
        // happens to exist at the derived path.
        assertFalse(DirectoryBatchValidator.isInputValid(
                true, false, "/storage/emulated/0/DCIM", EXISTING_DIR_PROBE));
    }

    // ---------------------------------------------------------------
    // Input validation — manual path fallback
    // ---------------------------------------------------------------

    @Test
    public void input_manualPath_existingDirectory_isValid() {
        assertTrue(DirectoryBatchValidator.isInputValid(
                false, false, "/storage/emulated/0/Pictures", EXISTING_DIR_PROBE));
    }

    @Test
    public void input_manualPath_notExisting_isRejected() {
        assertFalse(DirectoryBatchValidator.isInputValid(
                false, false, "/storage/emulated/0/Nope", INACCESSIBLE_PROBE));
    }

    @Test
    public void input_manualPath_empty_isRejected() {
        assertFalse(DirectoryBatchValidator.isInputValid(
                false, false, "", EXISTING_DIR_PROBE));
        assertFalse(DirectoryBatchValidator.isInputValid(
                false, false, "   ", EXISTING_DIR_PROBE));
        assertFalse(DirectoryBatchValidator.isInputValid(
                false, false, null, EXISTING_DIR_PROBE));
    }

    @Test
    public void input_manualPath_existsButNotDirectory_isRejected() {
        DirectoryBatchValidator.PathProbe fileNotDir =
                new DirectoryBatchValidator.PathProbe() {
                    @Override
                    public boolean exists(String path) {
                        return true;
                    }

                    @Override
                    public boolean isDirectory(String path) {
                        return false;
                    }
                };
        assertFalse(DirectoryBatchValidator.isInputValid(
                false, false, "/storage/emulated/0/photo.png", fileNotDir));
    }

    // ---------------------------------------------------------------
    // Output validation
    // ---------------------------------------------------------------

    @Test
    public void output_safUriValid_isValid() {
        assertTrue(DirectoryBatchValidator.isOutputValid(
                true, true, "/storage/ABCD-1234/Output"));
    }

    @Test
    public void output_safUriInvalid_isRejected() {
        // Regression: startBatchProcess previously never validated the output
        // SAF grant at all — a revoked grant launched the batch anyway.
        assertFalse(DirectoryBatchValidator.isOutputValid(
                true, false, "/storage/ABCD-1234/Output"));
    }

    @Test
    public void output_manualPath_nonEmpty_isValid() {
        // Output may not exist yet (created during processing).
        assertTrue(DirectoryBatchValidator.isOutputValid(
                false, false, "/storage/emulated/0/DCIM/RealSR/new-dir"));
    }

    @Test
    public void output_manualPath_empty_isRejected() {
        assertFalse(DirectoryBatchValidator.isOutputValid(false, false, ""));
        assertFalse(DirectoryBatchValidator.isOutputValid(false, false, "  "));
        assertFalse(DirectoryBatchValidator.isOutputValid(false, false, null));
    }

    // ---------------------------------------------------------------
    // canStart
    // ---------------------------------------------------------------

    @Test
    public void canStart_requiresBothValid() {
        assertTrue(DirectoryBatchValidator.canStart(true, true));
        assertFalse(DirectoryBatchValidator.canStart(true, false));
        assertFalse(DirectoryBatchValidator.canStart(false, true));
        assertFalse(DirectoryBatchValidator.canStart(false, false));
    }

    // ---------------------------------------------------------------
    // End-to-end regression scenario: SD card input + SAF output
    // ---------------------------------------------------------------

    @Test
    public void regression_sdCardBatch_fileInvisible_startEnabled() {
        // Full scenario from the bug report: user picks an SD-card input and
        // an SAF output via ACTION_OPEN_DOCUMENT_TREE.  Both SAF grants are
        // valid, but java.io.File cannot see either path.  The old code
        // disabled the Start button; the new logic must enable it.
        boolean inputValid = DirectoryBatchValidator.isInputValid(
                true, true, "/storage/ABCD-1234/Photos", INACCESSIBLE_PROBE);
        boolean outputValid = DirectoryBatchValidator.isOutputValid(
                true, true, "/storage/ABCD-1234/Photos-RealSR");
        assertTrue(DirectoryBatchValidator.canStart(inputValid, outputValid));
    }
}
