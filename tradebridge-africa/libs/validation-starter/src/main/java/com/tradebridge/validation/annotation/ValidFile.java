package com.tradebridge.validation.annotation;

import com.tradebridge.validation.file.FileMetadataValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Class-level annotation validating filename + MIME type + size on a DTO
 * whose fields are discoverable by name.
 *
 * <p>Requires the target type to expose:
 * <ul>
 *   <li>{@code String originalFilename()} or {@code getOriginalFilename()}</li>
 *   <li>{@code String mimeType()} or {@code getMimeType()}</li>
 *   <li>{@code Long sizeBytes()} or {@code getSizeBytes()}</li>
 * </ul>
 *
 * <p>Alternatively, services can call {@link FileMetadataValidator#validate}
 * directly in their business logic; the annotation exists for DTO-level
 * enforcement where the country's allowed MIME list is not known at compile
 * time (in which case service-layer call is the right choice).
 */
@Documented
@Constraint(validatedBy = { })
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidFile {
    /** Optional list override; empty = use module defaults. */
    String[] allowedMimeTypes() default { };

    /** Max size in bytes; 0 = use module default. */
    long maxSizeBytes() default 0L;

    String message() default "Invalid file metadata";
    Class<?>[] groups() default { };
    Class<? extends Payload>[] payload() default { };
}
