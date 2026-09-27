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

connectSearch("previewSearch", "#previewGrid article", (item) => {
  return `${item.dataset.name || ""} ${item.textContent || ""}`;
});

connectSearch("releaseSearch", "#releaseTimeline article", (item) => {
  return `${item.dataset.text || ""} ${item.textContent || ""}`;
});
