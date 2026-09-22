package com.tumuyan.ncnn.realsr;

/**
 * Pure-Java (Android-framework-free) string logic for Storage Access Framework
 * tree URIs.
 * <p>
 * Everything in this class is static and depends only on {@link String}, so it
 * can be compiled and unit-tested on a plain JVM with javac/JUnit — no Android
 * SDK, emulator, or device required.  {@link SafPathHelper} wraps these methods
 * with the Android-specific glue (Uri, DocumentsContract, DocumentFile).
 */
public final class SafPathResolver {

    private SafPathResolver() {
    }

    /**
     * Checks whether a string looks like a SAF content URI ("content://...").
     * Used to detect when an EditText holds a raw URI rather than a filesystem
     * path (happens for document providers whose tree URI cannot be mapped to
     * a filesystem path).
     */
    public static boolean isSafUriString(String s) {
        return s != null && s.startsWith("content://");
    }

    /**
     * Best-effort conversion of a tree-URI document ID to a filesystem path.
     * <p>
     * Handles two common patterns:
     * <ol>
     *   <li>{@code primary:<relative>} → {@code <storageBase>/<relative>}</li>
     *   <li>{@code <volume-id>:<relative>} → {@code /storage/<volume-id>/<relative>}</li>
     * </ol>
     * Falls back to {@code <storageBase>/<docId>} when the document ID does not
     * contain a colon separator (some third-party providers).
     *
     * @param docId       the tree document ID (e.g. "primary:DCIM", "ABCD-1234:Photos")
     * @param storageBase the external-storage root (e.g. "/storage/emulated/0")
     * @return an absolute path, or empty string when docId is null
     */
    public static String resolveTreeDocIdToPath(String docId, String storageBase) {
        if (docId == null) return "";
        if (docId.contains(":")) {
            String[] split = docId.split(":", 2);
            if (split.length == 2) {
                if ("primary".equals(split[0])) {
                    return storageBase + "/" + split[1];
                } else {
                    return "/storage/" + split[0] + "/" + split[1];
                }
            }
        }
        // docId without colon separator (e.g. some third-party providers)
        return storageBase + "/" + docId;
    }

    /**
     * Extracts a human-readable directory name from a tree document ID.
     * <p>
     * For a document ID like "primary:DCIM/Photos", returns "Photos".
     * For "ABCD-1234:DCIM", returns "DCIM".
     *
     * @param docId the tree document ID
     * @return the last path component, or null if docId is unusable
     */
    public static String extractTreeDisplayName(String docId) {
        if (docId == null || docId.isEmpty()) return null;
        if (docId.contains(":")) {
            String[] split = docId.split(":", 2);
            if (split.length == 2 && !split[1].isEmpty()) {
                String path = split[1];
                int lastSlash = path.lastIndexOf('/');
                if (lastSlash >= 0 && lastSlash < path.length() - 1) {
                    return path.substring(lastSlash + 1);
                }
                return path;
            }
        }
        return docId;
    }
}
