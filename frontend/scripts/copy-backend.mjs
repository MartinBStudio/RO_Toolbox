import { cpSync, copyFileSync, existsSync, mkdirSync, rmSync } from "node:fs";
import { resolve } from "node:path";

const root = resolve(process.cwd(), "..");
const sourceJar = resolve(root, "build", "libs", "RO_Toolbox.jar");
const resourcesDir = resolve(process.cwd(), "src-tauri", "resources");
const targetJar = resolve(resourcesDir, "RO_Toolbox.jar");
const javaHome = process.env.JAVA_HOME;
const bundledJreDir = resolve(resourcesDir, "jre");

if (!existsSync(sourceJar)) {
  throw new Error(`Backend jar not found: ${sourceJar}. Run npm run backend:jar first.`);
}

mkdirSync(resourcesDir, { recursive: true });
copyFileSync(sourceJar, targetJar);
console.log(`Copied backend jar to ${targetJar}`);

if (!javaHome || !existsSync(resolve(javaHome, "bin"))) {
  throw new Error("JAVA_HOME is not set to a valid JDK/JRE path. It is required to bundle a local Java runtime.");
}

rmSync(bundledJreDir, { recursive: true, force: true });
cpSync(javaHome, bundledJreDir, { recursive: true });
console.log(`Bundled Java runtime from ${javaHome} to ${bundledJreDir}`);
