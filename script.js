function connectSearch(inputId, itemSelector, textReader) {
  const input = document.getElementById(inputId);
  const items = Array.from(document.querySelectorAll(itemSelector));

  if (!input || items.length === 0) {
    return;
  }

  input.addEventListener("input", () => {
    const query = input.value.trim().toLowerCase();
    items.forEach((item) => {
      const text = textReader(item).toLowerCase();
      item.classList.toggle("is-hidden", query.length > 0 && !text.includes(query));
    });
  });
}

const packages = Array.isArray(window.RO_TOOLBOX_PACKAGES) ? window.RO_TOOLBOX_PACKAGES : [];
const displayPackages = buildDisplayPackages(packages);
const packageGrid = document.getElementById("packageGrid");
const packageFilters = document.getElementById("packageFilters");
const packageSearch = document.getElementById("packageSearch");
const previewModal = document.getElementById("previewModal");
const previewModalTitle = document.getElementById("previewModalTitle");
const previewModalMeta = document.getElementById("previewModalMeta");
const previewModalImage = document.getElementById("previewModalImage");
const previewModalCounter = document.getElementById("previewModalCounter");
const previewPrev = document.getElementById("previewPrev");
const previewNext = document.getElementById("previewNext");
let activeCategory = "All";
let activePreviewPackage = null;
let activePreviewIndex = 0;

function escapeHtml(value) {
  return String(value || "")
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");
}

function packageSearchText(pkg) {
  return [
    pkg.category,
    pkg.repo,
    pkg.name,
    pkg.folder,
    ...(pkg.variantNames || []),
    ...(pkg.variantFolders || []),
    pkg.version,
    pkg.author,
    pkg.description
  ].join(" ").toLowerCase();
}

function titleCasePackageName(value) {
  return String(value || "")
    .replace(/([a-z])([A-Z])/g, "$1 $2")
    .replace(/[-_]+/g, " ")
    .replace(/\s+/g, " ")
    .trim()
    .split(" ")
    .map((word) => word ? word.charAt(0).toUpperCase() + word.slice(1) : "")
    .join(" ");
}

function normalizedPackageFamily(pkg) {
  const source = String(pkg.folder || pkg.name || pkg.id || "").trim();
  const normalized = source
    .replace(/([a-z])([A-Z])/g, "$1 $2")
    .replace(/[-_]+/g, " ")
    .replace(/\s+/g, " ")
    .trim()
    .toLowerCase();

  if (pkg.category !== "Combat text") {
    return normalized;
  }

  return normalized
    .replace(/\s+crits\s+only$/i, "")
    .replace(/\s+large$/i, "")
    .trim();
}

function packageFamilyKey(pkg) {
  return `${pkg.category || "Package"}:${normalizedPackageFamily(pkg)}`;
}

function mergeUnique(values) {
  const seen = new Set();
  return values.filter((value) => {
    if (!value || seen.has(value)) {
      return false;
    }

    seen.add(value);
    return true;
  });
}

function buildDisplayPackages(sourcePackages) {
  const groups = new Map();

  sourcePackages.forEach((pkg) => {
    const key = packageFamilyKey(pkg);
    if (!groups.has(key)) {
      groups.set(key, []);
    }

    groups.get(key).push(pkg);
  });

  return Array.from(groups.entries()).map(([familyKey, group]) => {
    const family = familyKey.split(":").slice(1).join(":");
    const base = group.find((pkg) => String(pkg.folder || "").trim().toLowerCase() === family) || group[0];

    return {
      ...base,
      id: familyKey,
      name: titleCasePackageName(family || base.name),
      previews: mergeUnique(group.flatMap((pkg) => pkg.previews || [])),
      variantNames: mergeUnique(group.map((pkg) => pkg.name)),
      variantFolders: mergeUnique(group.map((pkg) => pkg.folder)),
      variantCount: group.length
    };
  });
}

function toClassName(value) {
  return String(value || "")
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "");
}

function renderPackageFilters() {
  if (!packageFilters) {
    return;
  }

  const categories = ["All", ...new Set(displayPackages.map((pkg) => pkg.category).filter(Boolean))];
  packageFilters.innerHTML = categories
    .map((category) => {
      const count = category === "All" ? displayPackages.length : displayPackages.filter((pkg) => pkg.category === category).length;
      return `<button type="button" class="filter-button${category === activeCategory ? " is-active" : ""}" data-category="${escapeHtml(category)}">${escapeHtml(category)} (${count})</button>`;
    })
    .join("");

  packageFilters.querySelectorAll("button").forEach((button) => {
    button.addEventListener("click", () => {
      activeCategory = button.dataset.category || "All";
      renderPackageFilters();
      renderPackages();
    });
  });
}

function renderPackages() {
  if (!packageGrid) {
    return;
  }

  const query = packageSearch ? packageSearch.value.trim().toLowerCase() : "";
  const visible = displayPackages.filter((pkg) => {
    const categoryMatches = activeCategory === "All" || pkg.category === activeCategory;
    const searchMatches = !query || packageSearchText(pkg).includes(query);
    return categoryMatches && searchMatches;
  });

  if (visible.length === 0) {
    packageGrid.innerHTML = `<div class="package-empty">No packages match this filter.</div>`;
    return;
  }

  packageGrid.innerHTML = visible
    .map((pkg) => {
      const firstPreview = pkg.previews && pkg.previews.length > 0 ? pkg.previews[0] : "";
      const previewCount = pkg.previews && pkg.previews.length > 1 ? `<span class="preview-count">${pkg.previews.length} previews</span>` : "";
      const version = pkg.version ? `<span class="package-pill">v${escapeHtml(pkg.version)}</span>` : "";
      const author = pkg.author ? `<span class="package-pill">by ${escapeHtml(pkg.author)}</span>` : "";
      const variants = pkg.variantCount > 1 ? `<span class="package-pill">${pkg.variantCount} variants</span>` : "";
      const categoryClass = toClassName(pkg.category);

      return `
        <article class="package-card is-${escapeHtml(categoryClass)}">
          <button type="button" class="package-media" data-preview-package="${escapeHtml(pkg.id)}" aria-label="Open ${escapeHtml(pkg.name)} preview">
            <img src="${escapeHtml(firstPreview)}" alt="${escapeHtml(pkg.name)} preview" loading="lazy" />
            ${previewCount}
          </button>
          <div>
            <div class="package-meta">
              <span class="package-pill">${escapeHtml(pkg.category)}</span>
              ${version}
              ${author}
              ${variants}
            </div>
            <h3>${escapeHtml(pkg.name)}</h3>
            <p>${escapeHtml(pkg.description)}</p>
          </div>
          <footer>
            <span>${escapeHtml(pkg.repo)}</span>
          </footer>
        </article>
      `;
    })
    .join("");

  packageGrid.querySelectorAll("[data-preview-package]").forEach((button) => {
    button.addEventListener("click", () => {
      openPreview(button.dataset.previewPackage || "");
    });
  });
}

function updatePreviewModal() {
  if (!activePreviewPackage || !previewModalImage || !previewModalTitle) {
    return;
  }

  const previews = activePreviewPackage.previews || [];
  const src = previews[activePreviewIndex] || "";
  previewModalTitle.textContent = activePreviewPackage.name || "Preview";
  previewModalMeta.textContent = `${activePreviewPackage.category || "Package"} · ${activePreviewPackage.repo || ""}`;
  previewModalImage.src = src;
  previewModalImage.alt = `${activePreviewPackage.name || "Package"} preview ${activePreviewIndex + 1}`;
  previewModalCounter.textContent = `${activePreviewIndex + 1} of ${previews.length}`;

  const hasMultiple = previews.length > 1;
  previewPrev.disabled = !hasMultiple;
  previewNext.disabled = !hasMultiple;
}

function openPreview(packageId) {
  const pkg = displayPackages.find((item) => item.id === packageId);
  if (!pkg || !previewModal) {
    return;
  }

  activePreviewPackage = pkg;
  activePreviewIndex = 0;
  updatePreviewModal();
  previewModal.classList.add("is-open");
  previewModal.setAttribute("aria-hidden", "false");
  document.body.classList.add("modal-open");
  previewModal.querySelector("[data-preview-close]").focus();
}

function closePreview() {
  if (!previewModal) {
    return;
  }

  previewModal.classList.remove("is-open");
  previewModal.setAttribute("aria-hidden", "true");
  document.body.classList.remove("modal-open");
  activePreviewPackage = null;
  activePreviewIndex = 0;
}

function movePreview(direction) {
  if (!activePreviewPackage) {
    return;
  }

  const total = activePreviewPackage.previews.length;
  if (total <= 1) {
    return;
  }

  activePreviewIndex = (activePreviewIndex + direction + total) % total;
  updatePreviewModal();
}

if (packageSearch) {
  packageSearch.addEventListener("input", renderPackages);
}

renderPackageFilters();
renderPackages();

document.querySelectorAll("[data-preview-close]").forEach((button) => {
  button.addEventListener("click", closePreview);
});

if (previewPrev) {
  previewPrev.addEventListener("click", () => movePreview(-1));
}

if (previewNext) {
  previewNext.addEventListener("click", () => movePreview(1));
}

window.addEventListener("keydown", (event) => {
  if (!previewModal || !previewModal.classList.contains("is-open")) {
    return;
  }

  if (event.key === "Escape") {
    closePreview();
  } else if (event.key === "ArrowLeft") {
    movePreview(-1);
  } else if (event.key === "ArrowRight") {
    movePreview(1);
  }
});

connectSearch("releaseSearch", "#releaseTimeline article", (item) => {
  return `${item.dataset.text || ""} ${item.textContent || ""}`;
});
