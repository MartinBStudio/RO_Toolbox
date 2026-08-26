import { copyFileSync, cpSync, existsSync, mkdirSync, rmSync } from "node:fs";
import { resolve } from "node:path";
import { spawnSync } from "node:child_process";

const root = resolve(process.cwd(), "..");
const sourceJar = resolve(root, "build", "libs", "RO_Toolbox.jar");
const resourcesDir = resolve(process.cwd(), "src-tauri", "resources");
const targetJar = resolve(resourcesDir, "RO_Toolbox.jar");
const targetJre = resolve(resourcesDir, "jre");

if (!existsSync(sourceJar)) {
  throw new Error(`Backend jar not found: ${sourceJar}. Run npm run backend:jar first.`);
}

const sourceJre = resolveJavaHome();
if (!sourceJre) {
  throw new Error(
    "Java runtime not found. Set JAVA_HOME or ensure `java` is on PATH before building so runtime can be bundled."
  );
}

mkdirSync(resourcesDir, { recursive: true });
copyFileSync(sourceJar, targetJar);
rmSync(targetJre, { recursive: true, force: true });
cpSync(sourceJre, targetJre, { recursive: true });
console.log(`Copied backend jar to ${targetJar}`);
console.log(`Copied bundled Java runtime to ${targetJre}`);

function resolveJavaHome() {
  const envHome = process.env.JAVA_HOME;
  if (envHome && existsSync(envHome)) {
    return resolve(envHome);
  }

  const probe = spawnSync("java", ["-XshowSettings:properties", "-version"], {
    encoding: "utf8",
  });
  const output = `${probe.stdout ?? ""}\n${probe.stderr ?? ""}`;
  const match = output.match(/^\s*java\.home\s*=\s*(.+)$/m);
  if (!match) {
    return null;
  }

  const detectedHome = match[1].trim();
  if (!detectedHome || !existsSync(detectedHome)) {
    return null;
  }
  return resolve(detectedHome);
}
