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
const packageGrid = document.getElementById("packageGrid");
const packageFilters = document.getElementById("packageFilters");
const packageSearch = document.getElementById("packageSearch");
let activeCategory = "All";

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
    pkg.version,
    pkg.author,
    pkg.description
  ].join(" ").toLowerCase();
}

function renderPackageFilters() {
  if (!packageFilters) {
    return;
  }

  const categories = ["All", ...new Set(packages.map((pkg) => pkg.category).filter(Boolean))];
  packageFilters.innerHTML = categories
    .map((category) => {
      const count = category === "All" ? packages.length : packages.filter((pkg) => pkg.category === category).length;
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
  const visible = packages.filter((pkg) => {
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

      return `
        <article class="package-card">
          <a class="package-media" href="${escapeHtml(pkg.packageUrl)}" target="_blank" rel="noopener noreferrer" aria-label="Open ${escapeHtml(pkg.name)} package">
            <img src="${escapeHtml(firstPreview)}" alt="${escapeHtml(pkg.name)} preview" loading="lazy" />
            ${previewCount}
          </a>
          <div>
            <div class="package-meta">
              <span class="package-pill">${escapeHtml(pkg.category)}</span>
              ${version}
              ${author}
            </div>
            <h3>${escapeHtml(pkg.name)}</h3>
            <p>${escapeHtml(pkg.description)}</p>
          </div>
          <footer>
            <span>${escapeHtml(pkg.repo)}</span>
            <a href="${escapeHtml(pkg.packageUrl)}" target="_blank" rel="noopener noreferrer">Open package</a>
          </footer>
        </article>
      `;
    })
    .join("");
}

if (packageSearch) {
  packageSearch.addEventListener("input", renderPackages);
}

renderPackageFilters();
renderPackages();

connectSearch("releaseSearch", "#releaseTimeline article", (item) => {
  return `${item.dataset.text || ""} ${item.textContent || ""}`;
});
