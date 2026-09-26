import { useEffect, useRef, useState } from "react";
import * as THREE from "three";
import { CubeIcon } from "@heroicons/react/24/outline";
import { getLootModelPreview, type LootModelPreview } from "../backendConnector/api.ts";

interface LootModelPreviewCanvasProps {
  folder: string;
  profileId?: string | null;
  size?: "table" | "card" | "modal";
  onOpen?: () => void;
  onOpenFallback?: () => void;
  fallbackImage?: string;
}

export function LootModelPreviewCanvas({
  folder,
  profileId,
  size = "table",
  onOpen,
  onOpenFallback,
  fallbackImage
}: LootModelPreviewCanvasProps) {
  const mountRef = useRef<HTMLDivElement | null>(null);
  const [preview, setPreview] = useState<LootModelPreview | null>(null);
  const [failed, setFailed] = useState(false);
  const [isVisible, setIsVisible] = useState(size === "modal");

  useEffect(() => {
    let cancelled = false;
    setPreview(null);
    setFailed(false);
    getLootModelPreview(folder, profileId)
      .then((result) => {
        if (!cancelled) {
          setPreview(result);
          setFailed(Boolean(result.error) || result.positions.length === 0 || result.indices.length === 0);
        }
      })
      .catch(() => {
        if (!cancelled) {
          setFailed(true);
        }
      });
    return () => {
      cancelled = true;
    };
  }, [folder, profileId]);

  useEffect(() => {
    const mount = mountRef.current;
    if (!mount || size === "modal") {
      setIsVisible(true);
      return;
    }
    if (!("IntersectionObserver" in window)) {
      setIsVisible(true);
      return;
    }

    const observer = new IntersectionObserver(
      ([entry]) => setIsVisible(entry.isIntersecting),
      { root: null, rootMargin: "160px" }
    );
    observer.observe(mount);
    return () => observer.disconnect();
  }, [size]);

  useEffect(() => {
    const mount = mountRef.current;
    if (!mount || !preview || failed || !isVisible) {
      return;
    }

    const width = Math.max(1, mount.clientWidth);
    const height = Math.max(1, mount.clientHeight);
    const renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true });
    renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, 2));
    renderer.setSize(width, height, false);
    mount.replaceChildren(renderer.domElement);
    const handleContextLost = (event: Event) => {
      event.preventDefault();
    };
    renderer.domElement.addEventListener("webglcontextlost", handleContextLost, false);

    const scene = new THREE.Scene();
    const camera = new THREE.PerspectiveCamera(35, width / height, 0.01, 100000);
    scene.add(new THREE.HemisphereLight(0xffffff, 0x28313d, 2.4));
    const keyLight = new THREE.DirectionalLight(0xffffff, 2.2);
    keyLight.position.set(1, 2, 3);
    scene.add(keyLight);

    const geometry = new THREE.BufferGeometry();
    geometry.setAttribute("position", new THREE.Float32BufferAttribute(preview.positions, 3));
    if (preview.normals.length === preview.positions.length) {
      geometry.setAttribute("normal", new THREE.Float32BufferAttribute(preview.normals, 3));
    }
    if (preview.colors.length === (preview.positions.length / 3) * 4) {
      geometry.setAttribute("color", new THREE.Float32BufferAttribute(preview.colors, 4));
    }
    geometry.setIndex(preview.indices);
    geometry.rotateX(-Math.PI / 2);
    geometry.computeBoundingBox();
    geometry.computeBoundingSphere();
    if (!geometry.getAttribute("normal")) {
      geometry.computeVertexNormals();
    }

    const material = new THREE.MeshStandardMaterial({
      color: 0xd8e2ff,
      metalness: 0.08,
      roughness: 0.58,
      side: THREE.DoubleSide,
      vertexColors: preview.colors.length > 0
    });
    const mesh = new THREE.Mesh(geometry, material);
    scene.add(mesh);

    const box = geometry.boundingBox ?? new THREE.Box3().setFromObject(mesh);
    const center = new THREE.Vector3();
    const size = new THREE.Vector3();
    box.getCenter(center);
    box.getSize(size);
    geometry.translate(-center.x, -center.y, -center.z);
    geometry.computeBoundingBox();
    geometry.computeBoundingSphere();

    const largestAxis = Math.max(size.x, size.y, size.z);
    const normalizedSize = 1.65;
    if (Number.isFinite(largestAxis) && largestAxis > 0) {
      mesh.scale.setScalar(normalizedSize / largestAxis);
    }
    camera.position.set(1.6, 1.1, 2.45);
    camera.near = 0.01;
    camera.far = 100;
    camera.lookAt(0, 0, 0);
    camera.updateProjectionMatrix();

    let frameId = 0;
    const render = () => {
      mesh.rotation.y += 0.012;
      mesh.rotation.x = -0.25;
      renderer.render(scene, camera);
      frameId = window.requestAnimationFrame(render);
    };
    render();

    return () => {
      window.cancelAnimationFrame(frameId);
      renderer.domElement.removeEventListener("webglcontextlost", handleContextLost);
      geometry.dispose();
      material.dispose();
      renderer.dispose();
      mount.replaceChildren();
    };
  }, [failed, isVisible, preview]);

  if (failed && fallbackImage) {
    return (
      <button
        type="button"
        className={`lootModelPreviewCanvasButton is-${size}`}
        onClick={onOpenFallback}
        title={`Click to view ${folder} preview`}
        aria-label={`View ${folder} preview`}
      >
        <img src={fallbackImage} alt={`${folder} preview`} className="lootFolderPreviewThumb" loading="lazy" />
      </button>
    );
  }

  const content = (
    <>
      {failed ? (
        <span className="lootFolderPreviewEmpty" title={preview?.error ?? "No mesh preview available"}>
          <CubeIcon className="lootFolderPreviewEmptyIcon" />
          <span className="lootFolderPreviewEmptyText">-</span>
        </span>
      ) : null}
      <div ref={mountRef} className="lootModelPreviewCanvasMount" />
    </>
  );

  if (onOpen && !failed) {
    return (
      <button
        type="button"
        className={`lootModelPreviewCanvasButton is-${size}`}
        onClick={onOpen}
        title={preview?.fileName ? `View ${preview.fileName} mesh preview` : "View mesh preview"}
        aria-label={`View ${folder} mesh preview`}
      >
        {content}
      </button>
    );
  }

  return (
    <div
      className={`lootModelPreviewCanvas is-${size}${failed ? " isFailed" : ""}`}
      title={preview?.fileName ? `${preview.fileName} mesh preview` : "Loading mesh preview"}
    >
      {content}
    </div>
  );
}
