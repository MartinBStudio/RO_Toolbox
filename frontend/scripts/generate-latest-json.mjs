import fs from "node:fs";
import path from "node:path";

const rootDir = process.cwd();
const tauriConfigPath = path.join(rootDir, "src-tauri", "tauri.conf.json");
const bundleDir = path.join(rootDir, "src-tauri", "target", "release", "bundle");
const nsisDir = path.join(bundleDir, "nsis");
const msiDir = path.join(bundleDir, "msi");

const repo = process.env.UPDATE_REPO || "MartinBStudio/RO_Toolbox";
const notes = process.env.UPDATE_NOTES || "Initial release";
const platformKey = "windows-x86_64";

function readJson(filePath) {
  return JSON.parse(fs.readFileSync(filePath, "utf8"));
}

function newestFileMatching(dirPath, regex) {
  const files = fs
    .readdirSync(dirPath)
    .filter((name) => regex.test(name))
    .map((name) => {
      const fullPath = path.join(dirPath, name);
      return { name, fullPath, mtimeMs: fs.statSync(fullPath).mtimeMs };
    })
    .sort((a, b) => b.mtimeMs - a.mtimeMs);

  return files[0];
}

function resolveUpdaterArtifact(version) {
  const escapedVersion = version.replace(/\./g, "\\.");
  const nsisPattern = new RegExp(`_${escapedVersion}_x64-setup\\.exe$`);
  const nsisSigPattern = new RegExp(`_${escapedVersion}_x64-setup\\.exe\\.sig$`);

  if (fs.existsSync(nsisDir)) {
    const nsisFile = newestFileMatching(nsisDir, nsisPattern);
    const nsisSig = newestFileMatching(nsisDir, nsisSigPattern);
    if (nsisFile && nsisSig) {
      return { file: nsisFile, sig: nsisSig, outputDir: nsisDir };
    }
  }

  // Fallback for older pipelines still producing MSI-based updater metadata.
  const msiPattern = new RegExp(`_${escapedVersion}_x64_en-US\\.msi$`);
  const msiSigPattern = new RegExp(`_${escapedVersion}_x64_en-US\\.msi\\.sig$`);

  if (!fs.existsSync(msiDir)) {
    throw new Error(`Neither NSIS nor MSI output directory exists under ${bundleDir}`);
  }

  const msiFile = newestFileMatching(msiDir, msiPattern);
  const msiSig = newestFileMatching(msiDir, msiSigPattern);
  if (!msiFile || !msiSig) {
    throw new Error(`No updater artifact found for version ${version} in ${bundleDir}`);
  }
  return { file: msiFile, sig: msiSig, outputDir: msiDir };
}

const tauriConfig = readJson(tauriConfigPath);
const version = tauriConfig.version;

if (!version) {
  throw new Error(`Missing version in ${tauriConfigPath}`);
}

const artifact = resolveUpdaterArtifact(version);

const signature = fs.readFileSync(artifact.sig.fullPath, "utf8").trim();
const encodedFileName = encodeURIComponent(artifact.file.name);
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

const latestJsonPath = path.join(artifact.outputDir, "latest.json");
fs.writeFileSync(latestJsonPath, `${JSON.stringify(latestJson, null, 2)}\n`, "utf8");
console.log(`Generated ${latestJsonPath} for ${version}`);
