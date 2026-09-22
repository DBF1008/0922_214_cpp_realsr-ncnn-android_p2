package com.tumuyan.ncnn.realsr;

/**
 * Pure-Java validation decisions for the directory batch-processing screen.
 * <p>
 * The rules are decoupled from {@code java.io.File} and
 * {@code androidx.documentfile.provider.DocumentFile} so the exact regression
 * scenario — a SAF-granted directory (SD card, USB-OTG, third-party document
 * provider) where {@code new File(path).exists()} returns false — can be
 * unit-tested on a plain JVM:
 * <ul>
 *   <li>When a SAF tree URI is available, validity comes from the SAF grant
 *       (DocumentFile check performed by the caller), never from File.</li>
 *   <li>Only for manually typed paths (no SAF URI) does the filesystem probe
 *       get consulted.</li>
 * </ul>
 */
public final class DirectoryBatchValidator {

    /**
     * Abstraction over filesystem existence/type checks so tests can supply
     * fakes.  The production implementation delegates to {@code java.io.File}.
     */
    public interface PathProbe {
        boolean exists(String path);

        boolean isDirectory(String path);
    }

    private DirectoryBatchValidator() {
    }

    /**
     * Validates the input directory.
     *
     * @param useSaf      true when a SAF tree URI is stored for the input
     *                    (i.e. the directory was picked via
     *                    ACTION_OPEN_DOCUMENT_TREE)
     * @param safUriValid result of the DocumentFile-based SAF check
     *                    (ignored when useSaf is false)
     * @param inputPath   the path string shown in the input field
     * @param probe       filesystem probe used only when useSaf is false
     * @return true when the input may be used for batch processing
     */
    public static boolean isInputValid(boolean useSaf, boolean safUriValid,
                                       String inputPath, PathProbe probe) {
        if (useSaf) {
            return safUriValid;
        }
        String path = inputPath == null ? "" : inputPath.trim();
        return !path.isEmpty() && probe.exists(path) && probe.isDirectory(path);
    }

    /**
     * Validates the output directory.  A manually typed output path only needs
     * to be non-empty (it may be created during processing); a SAF-picked
     * output must still hold a valid grant.
     *
     * @param useSaf      true when a SAF tree URI is stored for the output
     * @param safUriValid result of the DocumentFile-based SAF check
     * @param outputPath  the path string shown in the output field
     * @return true when the output may be used for batch processing
     */
    public static boolean isOutputValid(boolean useSaf, boolean safUriValid,
                                        String outputPath) {
        if (useSaf) {
            return safUriValid;
        }
        return outputPath != null && !outputPath.trim().isEmpty();
    }

    /**
     * The batch process may start only when both directories are valid.
     */
    public static boolean canStart(boolean inputValid, boolean outputValid) {
        return inputValid && outputValid;
    }
}
