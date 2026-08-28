// BEAT Image CDN — Lambda image processor.
//
// Invoked by CloudFront via Lambda Function URL (AWS_IAM) when the
// transformed-images origin returns 403/404. Downloads the original from
// the originals bucket, transforms it with Sharp, and writes the result
// back to the transformed-images bucket for subsequent cache hits.
//
// Path contract enforced by the viewer-request CloudFront Function:
//   /{prefix}/{uuid}-{filename}/format=<fmt>,width=<w>
//   /{prefix}/{uuid}-{filename}/original
//
// Pattern derived from aws-samples/image-optimization (MIT-0).

import { GetObjectCommand, PutObjectCommand, S3Client } from "@aws-sdk/client-s3";
import Sharp from "sharp";

const s3 = new S3Client();
const ORIGINAL_BUCKET = process.env.ORIGINAL_IMAGE_BUCKET;
const TRANSFORMED_BUCKET = process.env.TRANSFORMED_IMAGE_BUCKET;
const TRANSFORMED_CACHE_CONTROL = process.env.TRANSFORMED_IMAGE_CACHE_CONTROL || "public, max-age=31536000, immutable";
const MAX_INLINE_IMAGE_SIZE = parseInt(process.env.MAX_INLINE_IMAGE_SIZE || "4700000", 10);

export const handler = async (event) => {
    if (event?.requestContext?.http?.method !== "GET") {
        return errorResponse(405, "method not allowed");
    }

    const path = event.requestContext.http.path || "";
    const segments = path.split("/").filter(Boolean);
    if (segments.length < 2) {
        return errorResponse(400, "invalid path");
    }

    const operationsPrefix = segments.pop();
    const originalKey = segments.join("/");

    const operations = parseOperations(operationsPrefix);
    if (!operations) {
        return errorResponse(400, "invalid operations");
    }

    const downloadStart = performance.now();
    let originalBytes;
    let originalContentType;
    try {
        const out = await s3.send(new GetObjectCommand({ Bucket: ORIGINAL_BUCKET, Key: originalKey }));
        originalBytes = await out.Body.transformToByteArray();
        originalContentType = resolveOriginalContentType(out.ContentType, originalBytes);
    } catch (error) {
        if (error.name === "NoSuchKey") {
            return errorResponse(404, "original image not found");
        }
        console.error("S3 GetObject failed", { key: originalKey, error });
        return errorResponse(500, "could not load original image");
    }
    const downloadMs = Math.round(performance.now() - downloadStart);

    const transformStart = performance.now();
    let transformedBuffer;
    let transformedContentType;
    try {
        let pipeline = Sharp(originalBytes, { failOn: "none" }).rotate();

        if (operations.width || operations.height) {
            pipeline = pipeline.resize({
                width: operations.width,
                height: operations.height,
                withoutEnlargement: true,
            });
        }

        if (operations.format) {
            transformedContentType = `image/${operations.format === "jpeg" ? "jpeg" : operations.format}`;
            const formatOpts = operations.quality && isLossy(operations.format)
                ? { quality: operations.quality }
                : undefined;
            pipeline = pipeline.toFormat(operations.format, formatOpts);
        } else if (originalContentType === "image/svg+xml") {
            transformedContentType = "image/png";
            pipeline = pipeline.toFormat("png");
        } else {
            transformedContentType = originalContentType;
        }

        transformedBuffer = await pipeline.toBuffer();
    } catch (error) {
        const metadata = await readMetadataSafely(originalBytes);
        console.error("Sharp transform failed", {
            originalKey,
            operations,
            originalByteSize: originalBytes?.byteLength,
            originalContentType,
            errorName: error?.name,
            errorMessage: error?.message,
            metadata,
        });

        if (isBrowserRenderable(originalContentType) && originalBytes.byteLength <= MAX_INLINE_IMAGE_SIZE) {
            return {
                statusCode: 200,
                isBase64Encoded: true,
                headers: {
                    "Content-Type": originalContentType,
                    "Cache-Control": "private, no-store",
                    "Server-Timing": `download;dur=${downloadMs},transform;dur=0,fallback;dur=1`,
                },
                body: Buffer.from(originalBytes).toString("base64"),
            };
        }

        return errorResponse(500, "image transform failed");
    }
    const transformMs = Math.round(performance.now() - transformStart);

    const timing = `download;dur=${downloadMs},transform;dur=${transformMs}`;

    let stored = false;
    if (TRANSFORMED_BUCKET) {
        const transformedKey = `${originalKey}/${operationsPrefix}`;
        try {
            await s3.send(new PutObjectCommand({
                Bucket: TRANSFORMED_BUCKET,
                Key: transformedKey,
                Body: transformedBuffer,
                ContentType: transformedContentType,
                CacheControl: TRANSFORMED_CACHE_CONTROL,
            }));
            stored = true;
        } catch (error) {
            console.error("S3 PutObject (transformed) failed", { key: transformedKey, error });
        }
    }

    if (transformedBuffer.byteLength > MAX_INLINE_IMAGE_SIZE) {
        if (!stored) {
            return errorResponse(500, "transformed image too large and could not be cached");
        }

        const redirectLocation = buildPublicViewerUrl(originalKey, operations);
        return {
            statusCode: 302,
            headers: {
                Location: redirectLocation,
                "Cache-Control": "private, no-store",
                "Server-Timing": timing,
            },
        };
    }

    return {
        statusCode: 200,
        isBase64Encoded: true,
        headers: {
            "Content-Type": transformedContentType,
            "Cache-Control": TRANSFORMED_CACHE_CONTROL,
            "Server-Timing": timing,
        },
        body: transformedBuffer.toString("base64"),
    };
};

export function buildPublicViewerUrl(originalKey, operations) {
    const params = [];
    if (operations?.width) {
        params.push(`w=${operations.width}`);
    }
    if (operations?.format) {
        params.push(`format=${operations.format}`);
    }
    const qs = params.length > 0 ? `?${params.join("&")}` : "";
    return `/${originalKey}${qs}`;
}

export async function readMetadataSafely(bytes) {
    if (!bytes || bytes.length === 0) return null;
    try {
        const metadata = await Sharp(bytes, { failOn: "none" }).metadata();
        return {
            format: metadata.format,
            width: metadata.width,
            height: metadata.height,
            pages: metadata.pages,
            space: metadata.space,
            orientation: metadata.orientation,
        };
    } catch {
        return null;
    }
}

export function parseOperations(prefix) {
    if (prefix === "original") return {};

    const result = {};
    const pairs = prefix.split(",");
    for (const pair of pairs) {
        const [rawKey, rawValue] = pair.split("=");
        if (!rawKey || rawValue === undefined) return null;
        const key = rawKey.toLowerCase();
        const value = rawValue.toLowerCase();

        switch (key) {
            case "format":
                if (!["avif", "webp", "jpeg", "png"].includes(value)) return null;
                result.format = value;
                break;
            case "width":
            case "height": {
                const n = parseInt(value, 10);
                if (Number.isNaN(n) || n <= 0 || n > 4096) return null;
                result[key] = n;
                break;
            }
            case "quality": {
                const q = parseInt(value, 10);
                if (Number.isNaN(q) || q <= 0 || q > 100) return null;
                result.quality = q;
                break;
            }
            default:
                return null;
        }
    }
    return result;
}

export function isLossy(format) {
    return format === "jpeg" || format === "webp" || format === "avif";
}

export function isBrowserRenderable(mimeType) {
    if (!mimeType) return false;
    const lower = mimeType.toLowerCase();
    return [
        "image/jpeg",
        "image/png",
        "image/webp",
        "image/avif",
        "image/gif",
        "image/svg+xml",
    ].includes(lower);
}

export function resolveOriginalContentType(s3ContentType, bytes) {
    if (s3ContentType && s3ContentType !== "application/octet-stream" && s3ContentType !== "binary/octet-stream") {
        return s3ContentType;
    }
    return detectMimeType(bytes);
}

export function detectMimeType(bytes) {
    if (!bytes || bytes.length < 4) return "application/octet-stream";

    // JPEG: FF D8 FF
    if (bytes[0] === 0xff && bytes[1] === 0xd8 && bytes[2] === 0xff) {
        return "image/jpeg";
    }

    // PNG: 89 50 4E 47
    if (bytes[0] === 0x89 && bytes[1] === 0x50 && bytes[2] === 0x4e && bytes[3] === 0x47) {
        return "image/png";
    }

    // GIF: 47 49 46 38 ('GIF8')
    if (bytes[0] === 0x47 && bytes[1] === 0x49 && bytes[2] === 0x46 && bytes[3] === 0x38) {
        return "image/gif";
    }

    // WebP: RIFF....WEBP
    if (bytes.length >= 12 &&
        bytes[0] === 0x52 && bytes[1] === 0x49 && bytes[2] === 0x46 && bytes[3] === 0x46 &&
        bytes[8] === 0x57 && bytes[9] === 0x45 && bytes[10] === 0x42 && bytes[11] === 0x50
    ) {
        return "image/webp";
    }

    // AVIF: ....ftyp(avif|avis)
    if (bytes.length >= 12 &&
        bytes[4] === 0x66 && bytes[5] === 0x74 && bytes[6] === 0x79 && bytes[7] === 0x70
    ) {
        const majorBrand = String.fromCharCode(bytes[8], bytes[9], bytes[10], bytes[11]);
        if (majorBrand === "avif" || majorBrand === "avis") {
            return "image/avif";
        }
    }

    return "application/octet-stream";
}

function errorResponse(statusCode, message) {
    return {
        statusCode,
        headers: {
            "Content-Type": "text/plain",
            "Cache-Control": "no-store",
        },
        body: message,
    };
}
