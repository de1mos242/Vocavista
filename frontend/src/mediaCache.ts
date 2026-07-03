import { unwrap } from "./api";
import { getDictionaryVideos } from "./api/generated/sdk.gen";
import { absoluteUrl } from "./domain";

const PRONUNCIATION_VIDEO_CACHE = "pronunciation-videos";
const DICTIONARY_VIDEO_MANIFEST_KEY = "vocavista.dictionaryVideoManifest.v1";

export async function syncDictionaryVideoCache() {
  if (!("caches" in window)) return;

  const manifest = await unwrap(getDictionaryVideos());
  const cache = await caches.open(PRONUNCIATION_VIDEO_CACHE);
  const previousManifest = readCachedDictionaryVideoManifest();
  const nextManifest: Record<string, string> = {};
  const expectedUrls = new Set(manifest.items.map((item) => absoluteUrl(item.videoUrl)));

  for (const item of manifest.items) {
    const url = absoluteUrl(item.videoUrl);
    const updatedAt = item.updatedAt;
    nextManifest[url] = updatedAt;
    const cached = await cache.match(url);
    if (cached && previousManifest[url] === updatedAt) continue;

    const response = await fetch(cacheRefreshUrl(url, updatedAt), { credentials: "include", cache: "reload" });
    if (!response.ok) throw new Error(`Could not cache pronunciation video ${item.pronunciationAssetId}.`);
    await cache.put(url, response.clone());
  }

  for (const request of await cache.keys()) {
    if (isSmallPronunciationVideoUrl(request.url) && !expectedUrls.has(request.url)) {
      await cache.delete(request);
    }
  }
  localStorage.setItem(DICTIONARY_VIDEO_MANIFEST_KEY, JSON.stringify(nextManifest));
}

function readCachedDictionaryVideoManifest(): Record<string, string> {
  try {
    const rawValue = localStorage.getItem(DICTIONARY_VIDEO_MANIFEST_KEY);
    if (!rawValue) return {};
    const value = JSON.parse(rawValue) as Record<string, string>;
    return value && typeof value === "object" ? value : {};
  }
  catch {
    return {};
  }
}

function cacheRefreshUrl(value: string, updatedAt: string) {
  const url = new URL(value);
  url.searchParams.set("cacheVersion", updatedAt);
  return url.toString();
}

function isSmallPronunciationVideoUrl(value: string) {
  const url = new URL(value);
  return /^\/api\/v1\/media\/pronunciations\/[^/]+\/video\/small$/.test(url.pathname);
}
