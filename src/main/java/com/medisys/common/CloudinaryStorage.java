package com.medisys.common;

import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import com.cloudinary.api.exceptions.NotFound;
import com.cloudinary.utils.ObjectUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.NoSuchFileException;
import java.time.Duration;
import java.util.Map;

/**
 * STRATEGY 2 of 2: keeps uploaded files in Cloudinary (used by the live system).
 *
 * Setting: cloudinary.url (on the server: the CLOUDINARY_URL environment
 * variable), copied from the Cloudinary dashboard:
 *     cloudinary://<api key>:<api secret>@<cloud name>
 *
 * Privacy: prescriptions and profile photos are personal data, so they are
 * uploaded as type "authenticated". Such a file has no public address - only
 * a link signed with our secret key can download it, and that link is only
 * ever used here on the server. The browser still gets the file from our
 * servlet, after it checked who is asking (the same as with LocalStorage).
 *
 * Product photos (folder "medicines/") are not private, so they are uploaded
 * as normal public images. The catalog links straight to Cloudinary's CDN
 * (publicUrl), which resizes them and picks the best format (WebP/AVIF) for
 * each browser - the pages load fast and our server does no work.
 *
 * Key "prescriptions/5f1c.png" is stored in Cloudinary as public id
 * "medisys/prescriptions/5f1c" with format "png".
 *
 * Module : Shared - agree with the team before changing it
 * Owner  : Whole team
 */
public class CloudinaryStorage implements StorageStrategy {

    private static final String PRIVATE = "authenticated";
    private static final String PUBLIC = "upload";
    private static final String RESOURCE_TYPE = "image";     // Cloudinary keeps PDFs as images too

    private final Cloudinary cloudinary;
    private final String folder;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public CloudinaryStorage(String cloudinaryUrl) {
        cloudinary = new Cloudinary(cloudinaryUrl);
        cloudinary.config.secure = true;
        String configured = AppConfig.get("cloudinary.folder");
        folder = configured.isEmpty() ? "medisys" : configured;
    }

    @Override
    public void put(String key, byte[] data, String contentType) throws IOException {
        cloudinary.uploader().upload(data, ObjectUtils.asMap(
                "public_id", publicId(key),
                "resource_type", RESOURCE_TYPE,
                "type", typeOf(key),
                "overwrite", true));
    }

    @Override
    public boolean exists(String key) throws IOException {
        try {
            cloudinary.api().resource(publicId(key), options(key));
            return true;
        } catch (NotFound e) {
            return false;
        } catch (Exception e) {
            throw new IOException("Cloudinary lookup failed: " + e.getMessage(), e);
        }
    }

    @Override
    public InputStream open(String key) throws IOException {
        String url;
        try {
            // A short-lived link signed with the API secret.
            url = cloudinary.privateDownload(publicId(key), format(key), options(key));
        } catch (Exception e) {
            throw new IOException("Could not sign the Cloudinary link: " + e.getMessage(), e);
        }
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();
        try {
            HttpResponse<InputStream> response = http.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() != 200) {
                response.body().close();
                if (response.statusCode() == 404) {
                    // the same exception LocalStorage gives, so the servlets answer 404
                    throw new NoSuchFileException(key);
                }
                throw new IOException("Cloudinary answered " + response.statusCode() + " for " + key);
            }
            return response.body();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Download interrupted", e);
        }
    }

    @Override
    public void delete(String key) throws IOException {
        Map<String, Object> options = options(key);
        options.put("invalidate", true);
        cloudinary.uploader().destroy(publicId(key), options);
    }

    @Override
    public String publicUrl(String key, int size) {
        if (!isPublic(key)) {
            return null;
        }
        // c_pad + white background: any photo becomes a neat square without being cut.
        return cloudinary.url()
                .secure(true)
                .transformation(new Transformation<>()
                        .width(size).height(size).crop("pad").background("white")
                        .fetchFormat("auto").quality("auto"))
                .format(format(key))
                .generate(publicId(key));
    }

    @Override
    public String describe() {
        return "Cloudinary (cloud " + cloudinary.config.cloudName + ", folder " + folder + ")";
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> options(String key) {
        return ObjectUtils.asMap("resource_type", RESOURCE_TYPE, "type", typeOf(key));
    }

    /** Only product photos are public. */
    private static boolean isPublic(String key) {
        return key.startsWith("medicines/");
    }

    private static String typeOf(String key) {
        return isPublic(key) ? PUBLIC : PRIVATE;
    }

    /** "prescriptions/5f1c.png" -> "medisys/prescriptions/5f1c" */
    private String publicId(String key) {
        return folder + "/" + key.substring(0, key.lastIndexOf('.'));
    }

    /** "prescriptions/5f1c.png" -> "png" */
    private static String format(String key) {
        return key.substring(key.lastIndexOf('.') + 1);
    }
}
