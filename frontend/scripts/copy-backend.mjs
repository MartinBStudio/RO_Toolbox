import { cpSync, copyFileSync, existsSync, mkdirSync, rmSync } from "node:fs";
import { resolve } from "node:path";

const root = resolve(process.cwd(), "..");
const sourceJar = resolve(root, "build", "libs", "RO_Toolbox.jar");
const resourcesDir = resolve(process.cwd(), "src-tauri", "resources");
const targetJar = resolve(resourcesDir, "RO_Toolbox.jar");
const javaHome = process.env.JAVA_HOME;
const bundledJreDir = resolve(resourcesDir, "jre");
const legacyJreDir = javaHome ? resolve(javaHome, "jre") : null;
const runtimeEntries = ["bin", "conf", "legal", "lib", "release"];

if (!existsSync(sourceJar)) {
  throw new Error(`Backend jar not found: ${sourceJar}. Run npm run backend:jar first.`);
}

mkdirSync(resourcesDir, { recursive: true });
copyFileSync(sourceJar, targetJar);
console.log(`Copied backend jar to ${targetJar}`);

if (!javaHome || !existsSync(resolve(javaHome, "bin"))) {
  console.warn("JAVA_HOME is not set to a valid JDK/JRE path. Skipping bundled Java runtime copy.");
} else {
  rmSync(bundledJreDir, { recursive: true, force: true });

  if (legacyJreDir && existsSync(legacyJreDir)) {
    cpSync(legacyJreDir, bundledJreDir, { recursive: true });
  } else {
    mkdirSync(bundledJreDir, { recursive: true });
    let copiedEntries = 0;
    for (const entry of runtimeEntries) {
      const source = resolve(javaHome, entry);
      if (existsSync(source)) {
        cpSync(source, resolve(bundledJreDir, entry), { recursive: true });
        copiedEntries += 1;
      }
    }
    if (copiedEntries === 0) {
      console.warn(`No Java runtime entries were copied from ${javaHome}. The app will fall back to system Java if available.`);
    }
  }

  console.log(`Bundled Java runtime components from ${javaHome} to ${bundledJreDir}`);
}
