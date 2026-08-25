import { copyFileSync, existsSync, mkdirSync } from "node:fs";
import { resolve } from "node:path";

const root = resolve(process.cwd(), "..");
const sourceJar = resolve(root, "build", "libs", "RO_Toolbox.jar");
const resourcesDir = resolve(process.cwd(), "src-tauri", "resources");
const targetJar = resolve(resourcesDir, "RO_Toolbox.jar");

if (!existsSync(sourceJar)) {
  throw new Error(`Backend jar not found: ${sourceJar}. Run npm run backend:jar first.`);
}

mkdirSync(resourcesDir, { recursive: true });
copyFileSync(sourceJar, targetJar);
console.log(`Copied backend jar to ${targetJar}`);
