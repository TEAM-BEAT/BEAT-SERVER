// BEAT Image CDN — viewer-request CloudFront Function.
//
// Rewrites /{prefix}/{uuid}-{filename}[?w=...&format=...] into
//   /{prefix}/{uuid}-{filename}/format=<fmt>,width=<w>
// (or /original for unmodified delivery), giving every variant a stable
// S3 key and a one-to-one cache key.
//
// Runtime: cloudfront-js-2.0
// Pattern: derived from aws-samples/image-optimization (MIT-0), with a
// strict variant whitelist and explicit 400 rejection on disallowed
// inputs (vs. the upstream sample's silent fallback).

var ALLOWED_WIDTHS = { 240: 1, 480: 1, 960: 1, 1920: 1 };
var ALLOWED_FORMATS = { avif: 1, webp: 1, jpeg: 1, png: 1 };
var ALLOWED_PREFIXES = { poster: 1, cast: 1, staff: 1, performance: 1, carousel: 1, banner: 1 };
var DEFAULT_WIDTH = 960;

function handler(event) {
    var request = event.request;

    var uri = request.uri;
    if (!isAllowedPath(uri)) {
        return reject(400, 'invalid path');
    }

    var qs = request.querystring || {};

    var widthRaw = qs.w && qs.w.value;
    var width = widthRaw ? parseInt(widthRaw, 10) : DEFAULT_WIDTH;
    if (!ALLOWED_WIDTHS[width]) {
        return reject(400, 'invalid width');
    }

    var formatRaw = qs.format && qs.format.value ? qs.format.value.toLowerCase() : 'auto';
    var format;
    if (formatRaw === 'auto') {
        format = negotiateFormat(request.headers);
    } else if (ALLOWED_FORMATS[formatRaw]) {
        format = formatRaw;
    } else {
        return reject(400, 'invalid format');
    }

    request.uri = uri + '/format=' + format + ',width=' + width;
    request.querystring = {};
    return request;
}

function isAllowedPath(uri) {
    if (uri.length < 4 || uri.length > 512) return false;
    if (uri.indexOf('/') !== 0) return false;
    var rest = uri.substring(1);
    var slashIdx = rest.indexOf('/');
    if (slashIdx <= 0) return false;
    var prefix = rest.substring(0, slashIdx);
    if (!ALLOWED_PREFIXES[prefix]) return false;
    var filename = rest.substring(slashIdx + 1);
    if (filename.length === 0 || filename.indexOf('/') !== -1) return false;
    return true;
}

function negotiateFormat(headers) {
    var accept = headers && headers.accept;
    if (!accept) return 'jpeg';
    if (acceptIncludes(accept, 'image/avif')) return 'avif';
    if (acceptIncludes(accept, 'image/webp')) return 'webp';
    return 'jpeg';
}

function acceptIncludes(acceptHeader, token) {
    if (acceptHeader.value && acceptHeader.value.toLowerCase().indexOf(token) !== -1) return true;
    if (acceptHeader.multiValue) {
        for (var i = 0; i < acceptHeader.multiValue.length; i++) {
            var v = acceptHeader.multiValue[i].value;
            if (v && v.toLowerCase().indexOf(token) !== -1) return true;
        }
    }
    return false;
}

function reject(statusCode, message) {
    return {
        statusCode: statusCode,
        statusDescription: statusCode === 400 ? 'Bad Request' : 'Error',
        headers: {
            'content-type': { value: 'text/plain' },
            'cache-control': { value: 'no-store' }
        },
        body: message
    };
}
