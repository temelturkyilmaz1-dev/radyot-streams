import { createHash } from "node:crypto";
import { readFile, writeFile } from "node:fs/promises";
import path from "node:path";

const root = path.resolve(import.meta.dirname, "..");
const catalogPath = path.join(root, "stations.json");
const logoDir = path.join(root, "logos");
const reportPath = path.join(root, "external-logo-migration-report.json");
const workerHost = "autumn-sun-8d69.temel-turkyilmaz1.workers.dev";
const workerBase = `https://${workerHost}/logos/`;
const applyChanges = process.argv.includes("--apply");
const fallbackSources = new Map([
  ["https://www.yeniradyo.com.tr/images/1234.png", "https://www.google.com/s2/favicons?domain=yeniradyo.com.tr&sz=256"],
  ["https://cobrox.radio.fm/apple-touch-icon.png", "https://www.google.com/s2/favicons?domain=cobrox.radio.fm&sz=256"],
  ["https://www.emisoraejercito.mil.co/og.png", "https://www.google.com/s2/favicons?domain=emisoraejercito.mil.co&sz=256"],
]);

const catalog = JSON.parse(await readFile(catalogPath, "utf8"));
const groups = new Map();
for (const station of catalog.stations) {
  if (!station.logo) continue;
  let parsed;
  try { parsed = new URL(station.logo); } catch { continue; }
  if (parsed.hostname === workerHost) continue;
  if (!groups.has(station.logo)) groups.set(station.logo, []);
  groups.get(station.logo).push(station);
}

function detect(bytes, contentType = "") {
  if (bytes.length >= 12 && bytes.subarray(0, 4).toString() === "RIFF" && bytes.subarray(8, 12).toString() === "WEBP") return ".webp";
  if (bytes.length >= 8 && bytes.subarray(0, 8).equals(Buffer.from([137,80,78,71,13,10,26,10]))) return ".png";
  if (bytes.length >= 3 && bytes[0] === 255 && bytes[1] === 216 && bytes[2] === 255) return ".jpg";
  if (bytes.length >= 6 && ["GIF87a", "GIF89a"].includes(bytes.subarray(0, 6).toString())) return ".gif";
  if (bytes.length >= 4 && bytes[0] === 0 && bytes[1] === 0 && bytes[2] === 1 && bytes[3] === 0) return ".ico";
  if (bytes.length >= 12 && bytes.subarray(4, 12).toString().includes("ftypavif")) return ".avif";
  const head = bytes.subarray(0, 512).toString("utf8").trimStart().toLowerCase();
  if (contentType.includes("svg") || head.startsWith("<svg") || head.includes("<svg")) return ".svg";
  return null;
}

async function download([url, stations]) {
  const stem = "migrated_" + createHash("sha256").update(url).digest("hex").slice(0, 20);
  let lastError;
  for (const sourceUrl of [url, fallbackSources.get(url)].filter(Boolean)) {
    try {
      const response = await fetch(sourceUrl, {
        redirect: "follow",
        headers: { "User-Agent": "Mozilla/5.0 RadiOnlineLogoMigration/1.0", "Accept": "image/avif,image/webp,image/png,image/jpeg,image/svg+xml,image/*,*/*;q=0.8" },
        signal: AbortSignal.timeout(30000),
      });
      if (!response.ok) throw new Error(`HTTP ${response.status}`);
      const bytes = Buffer.from(await response.arrayBuffer());
      const extension = detect(bytes, response.headers.get("content-type") || "");
      if (!extension) throw new Error(`görsel biçimi tanınmadı (${response.headers.get("content-type") || "bilinmiyor"}, ${bytes.length} bayt)`);
      if (bytes.length < 200) throw new Error(`dosya çok küçük (${bytes.length} bayt)`);
      if (applyChanges) await writeFile(path.join(logoDir, stem + extension), bytes);
      return { url, sourceUrl, stem, extension, bytes: bytes.length, stationIds: stations.map(s => s.id), names: stations.map(s => s.name), ok: true };
    } catch (error) {
      lastError = error;
    }
  }
  return { url, stem, stationIds: stations.map(s => s.id), names: stations.map(s => s.name), ok: false, error: String(lastError?.message || lastError) };
}

const entries = [...groups.entries()];
const results = [];
let cursor = 0;
const workers = Array.from({ length: 12 }, async () => {
  while (cursor < entries.length) {
    const index = cursor++;
    results[index] = await download(entries[index]);
  }
});
await Promise.all(workers);

if (applyChanges) {
  const byUrl = new Map(results.filter(r => r.ok).map(r => [r.url, r]));
  for (const station of catalog.stations) {
    const result = byUrl.get(station.logo);
    if (result) station.logo = `${workerBase}${result.stem}.webp`;
  }
  await writeFile(catalogPath, JSON.stringify(catalog, null, 2) + "\n", "utf8");
}

const report = {
  generatedAt: new Date().toISOString(),
  applyChanges,
  externalStationCount: entries.reduce((sum, [, stations]) => sum + stations.length, 0),
  uniqueExternalUrlCount: entries.length,
  successfulUrlCount: results.filter(r => r.ok).length,
  failedUrlCount: results.filter(r => !r.ok).length,
  migratedStationCount: results.filter(r => r.ok).reduce((sum, r) => sum + r.stationIds.length, 0),
  failedStationCount: results.filter(r => !r.ok).reduce((sum, r) => sum + r.stationIds.length, 0),
  results,
};
await writeFile(reportPath, JSON.stringify(report, null, 2) + "\n", "utf8");
console.log(JSON.stringify({
  externalStations: report.externalStationCount,
  uniqueUrls: report.uniqueExternalUrlCount,
  successfulUrls: report.successfulUrlCount,
  failedUrls: report.failedUrlCount,
  migratedStations: report.migratedStationCount,
  failedStations: report.failedStationCount,
}, null, 2));
