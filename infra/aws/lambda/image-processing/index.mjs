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
const MAX_IMAGE_SIZE = parseInt(process.env.MAX_IMAGE_SIZE || "6291456", 10);

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
    let contentType;
    try {
        const out = await s3.send(new GetObjectCommand({ Bucket: ORIGINAL_BUCKET, Key: originalKey }));
        originalBytes = await out.Body.transformToByteArray();
        contentType = out.ContentType;
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
    try {
        let pipeline = Sharp(originalBytes, { failOn: "none", animated: true }).rotate();

        if (operations.width || operations.height) {
            pipeline = pipeline.resize({
                width: operations.width,
                height: operations.height,
                withoutEnlargement: true,
            });
        }

        if (operations.format) {
            contentType = `image/${operations.format === "jpeg" ? "jpeg" : operations.format}`;
            const formatOpts = operations.quality && isLossy(operations.format)
                ? { quality: operations.quality }
                : undefined;
            pipeline = pipeline.toFormat(operations.format, formatOpts);
        } else if (contentType === "image/svg+xml") {
            contentType = "image/png";
            pipeline = pipeline.toFormat("png");
        }

        transformedBuffer = await pipeline.toBuffer();
    } catch (error) {
        console.error("Sharp transform failed", { key: originalKey, operations, error });
        return errorResponse(500, "image transform failed");
    }
    const transformMs = Math.round(performance.now() - transformStart);

    const timing = `download;dur=${downloadMs},transform;dur=${transformMs}`;

    if (TRANSFORMED_BUCKET) {
        const transformedKey = `${originalKey}/${operationsPrefix}`;
        try {
            await s3.send(new PutObjectCommand({
                Bucket: TRANSFORMED_BUCKET,
                Key: transformedKey,
                Body: transformedBuffer,
                ContentType: contentType,
                CacheControl: TRANSFORMED_CACHE_CONTROL,
            }));
        } catch (error) {
            console.error("S3 PutObject (transformed) failed; serving inline", { key: transformedKey, error });
        }
    }

    if (Buffer.byteLength(transformedBuffer) > MAX_IMAGE_SIZE) {
        return errorResponse(413, "transformed image exceeds Lambda payload limit");
    }

    return {
        statusCode: 200,
        isBase64Encoded: true,
        headers: {
            "Content-Type": contentType,
            "Cache-Control": TRANSFORMED_CACHE_CONTROL,
            "Server-Timing": timing,
        },
        body: transformedBuffer.toString("base64"),
    };
};

function parseOperations(prefix) {
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

function isLossy(format) {
    return format === "jpeg" || format === "webp" || format === "avif";
}

function errorResponse(statusCode, message) {
    return {
        statusCode,
        headers: { "Content-Type": "text/plain" },
        body: message,
    };
}
