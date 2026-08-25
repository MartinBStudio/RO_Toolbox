import fs from "node:fs";
import path from "node:path";

const rootDir = process.cwd();
const tauriConfigPath = path.join(rootDir, "src-tauri", "tauri.conf.json");
const msiDir = path.join(rootDir, "src-tauri", "target", "release", "bundle", "msi");

const repo = process.env.UPDATE_REPO || "MartinBStudio/RO_Toolbox";
const notes = process.env.UPDATE_NOTES || "Initial release";
const platformKey = "windows-x86_64";

function readJson(filePath) {
  return JSON.parse(fs.readFileSync(filePath, "utf8"));
}

function newestFileMatching(regex) {
  const files = fs
    .readdirSync(msiDir)
    .filter((name) => regex.test(name))
    .map((name) => {
      const fullPath = path.join(msiDir, name);
      return { name, fullPath, mtimeMs: fs.statSync(fullPath).mtimeMs };
    })
    .sort((a, b) => b.mtimeMs - a.mtimeMs);

  return files[0];
}

const tauriConfig = readJson(tauriConfigPath);
const version = tauriConfig.version;

if (!version) {
  throw new Error(`Missing version in ${tauriConfigPath}`);
}

if (!fs.existsSync(msiDir)) {
  throw new Error(`MSI output directory not found: ${msiDir}`);
}

const escapedVersion = version.replace(/\./g, "\\.");
const msiPattern = new RegExp(`_${escapedVersion}_x64_en-US\\.msi$`);
const sigPattern = new RegExp(`_${escapedVersion}_x64_en-US\\.msi\\.sig$`);

const msiFile = newestFileMatching(msiPattern);
const sigFile = newestFileMatching(sigPattern);

if (!msiFile) {
  throw new Error(`No MSI found for version ${version} in ${msiDir}`);
}

if (!sigFile) {
  throw new Error(`No MSI signature found for version ${version} in ${msiDir}`);
}

const signature = fs.readFileSync(sigFile.fullPath, "utf8").trim();
const encodedFileName = encodeURIComponent(msiFile.name);
const url = `https://github.com/${repo}/releases/download/v${version}/${encodedFileName}`;

const latestJson = {
  version,
  notes,
  pub_date: new Date().toISOString(),
  platforms: {
    [platformKey]: {
      signature,
      url
    }
  }
};

const latestJsonPath = path.join(msiDir, "latest.json");
fs.writeFileSync(latestJsonPath, `${JSON.stringify(latestJson, null, 2)}\n`, "utf8");
console.log(`Generated ${latestJsonPath} for ${version}`);
