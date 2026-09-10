package com.orabank.backend.export;

/** Classeur pret a etre telecharge. */
public record ExportedFile(String filename, byte[] content) {

    public static final String CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
}
