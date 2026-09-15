import React from "react";

export type ReleaseItem = {
  id: string;
  rawText: string;
  tag?: string;
  title?: string;
  content: React.ReactNode;
};

export type ReleaseCategory = {
  id: string;
  title: string;
  iconType: "feature" | "ui" | "fix" | "improvement" | "tool" | "general";
  items: ReleaseItem[];
};

export type ReleaseEntry = {
  id: string;
  version: string;
  rawVersionHeader: string;
  title: string;
  date?: string;
  isLatest: boolean;
  categories: ReleaseCategory[];
  summary?: string;
};

export type ParsedReleaseNotes = {
  mainTitle: string;
  releases: ReleaseEntry[];
};

function parseInlineFormatting(text: string): React.ReactNode[] {
  // Matches `code`, **bold**, *italic*, and [link](url)
  const tokenRegex = /(`[^`]+`|\*\*[^*]+\*\*|__[^_]+__|(?<!\*)\*[^*]+\*(?!\*)|(?<!_)_[^_]+_(?!_)|\[([^\]]+)\]\(([^)]+)\))/g;
  const parts: React.ReactNode[] = [];
  let lastIndex = 0;
  let match: RegExpExecArray | null;
  let keyCounter = 0;

  while ((match = tokenRegex.exec(text)) !== null) {
    if (match.index > lastIndex) {
      parts.push(text.slice(lastIndex, match.index));
    }

    const token = match[0];
    if (token.startsWith("`") && token.endsWith("`")) {
      parts.push(
        <code key={`code-${keyCounter++}`} className="releaseNotesCode">
          {token.slice(1, -1)}
        </code>
      );
    } else if (
      (token.startsWith("**") && token.endsWith("**")) ||
      (token.startsWith("__") && token.endsWith("__"))
    ) {
      parts.push(
        <strong key={`bold-${keyCounter++}`} className="releaseNotesBold">
          {token.slice(2, -2)}
        </strong>
      );
    } else if (
      (token.startsWith("*") && token.endsWith("*")) ||
      (token.startsWith("_") && token.endsWith("_"))
    ) {
      parts.push(
        <em key={`italic-${keyCounter++}`} className="releaseNotesItalic">
          {token.slice(1, -1)}
        </em>
      );
    } else if (token.startsWith("[") && token.includes("](") && token.endsWith(")")) {
      const linkMatch = token.match(/^\[([^\]]+)\]\(([^)]+)\)$/);
      if (linkMatch) {
        parts.push(
          <a
            key={`link-${keyCounter++}`}
            href={linkMatch[2]}
            target="_blank"
            rel="noopener noreferrer"
            className="releaseNotesLink"
          >
            {linkMatch[1]}
          </a>
        );
      } else {
        parts.push(token);
      }
    } else {
      parts.push(token);
    }

    lastIndex = tokenRegex.lastIndex;
  }

  if (lastIndex < text.length) {
    parts.push(text.slice(lastIndex));
  }

  return parts.length > 0 ? parts : [text];
}

function detectCategoryIcon(title: string): ReleaseCategory["iconType"] {
  const lower = title.toLowerCase();
  if (lower.includes("feature") || lower.includes("added") || lower.includes("✨") || lower.includes("🚀")) {
    return "feature";
  }
  if (lower.includes("ui") || lower.includes("design") || lower.includes("visual") || lower.includes("🎨") || lower.includes("layout")) {
    return "ui";
  }
  if (lower.includes("fix") || lower.includes("bug") || lower.includes("patch") || lower.includes("🐛") || lower.includes("security")) {
    return "fix";
  }
  if (lower.includes("tool") || lower.includes("config") || lower.includes("login") || lower.includes("resource") || lower.includes("⚙️") || lower.includes("🔗")) {
    return "tool";
  }
  if (lower.includes("improve") || lower.includes("polish") || lower.includes("perf") || lower.includes("🛠️") || lower.includes("refactor")) {
    return "improvement";
  }
  return "general";
}

export function parseReleaseNotes(markdown: string): ParsedReleaseNotes {
  if (!markdown || !markdown.trim()) {
    return {
      mainTitle: "Release Notes",
      releases: [],
    };
  }

  const lines = markdown.split(/\r?\n/);
  let mainTitle = "RO Toolbox – Release Notes";
  const releases: ReleaseEntry[] = [];

  let currentRelease: ReleaseEntry | null = null;
  let currentCategory: ReleaseCategory | null = null;
  let introLines: string[] = [];

  for (let i = 0; i < lines.length; i++) {
    const rawLine = lines[i];
    const trimmed = rawLine.trim();

    if (!trimmed) {
      continue;
    }

    // Main Header: # Title
    if (trimmed.startsWith("# ") && !trimmed.startsWith("## ")) {
      mainTitle = trimmed.replace(/^#\s+/, "").trim();
      continue;
    }

    // Version Header: ## v0.5.6 – Minor UI improvements (2026-09-15)
    if (trimmed.startsWith("## ")) {
      const headerText = trimmed.replace(/^##\s+/, "").trim();
      
      // Extract date if present (e.g. (2026-09-15) or [2026-09-15])
      let dateMatch = headerText.match(/[\(\[]\s*(\d{4}[-/.]\d{2}[-/.]\d{2}|[A-Za-z]+\s+\d{1,2},\s*\d{4})\s*[\)\]]/);
      let date = dateMatch ? dateMatch[1] : undefined;
      let cleanHeaderText = headerText;
      if (dateMatch) {
        cleanHeaderText = cleanHeaderText.replace(dateMatch[0], "").trim();
      }

      // Extract version tag
      const versionMatch = cleanHeaderText.match(/^(?:version\s*|v)?(\d+\.\d+(?:\.\d+)?(?:-[a-zA-Z0-9.]+)?)/i);
      let version = versionMatch ? `v${versionMatch[1].replace(/^v/i, "")}` : "";
      let title = cleanHeaderText;
      if (versionMatch) {
        title = cleanHeaderText.slice(versionMatch[0].length).replace(/^[\s–—\-:;]+/, "").trim();
      }

      if (!version) {
        version = headerText.split(/[\s–—\-]/)[0].trim() || "Update";
      }

      const isLatest = releases.length === 0;

      currentRelease = {
        id: `release-${releases.length + 1}-${version.replace(/[^a-zA-Z0-9]/g, "-")}`,
        version: version || "Release",
        rawVersionHeader: headerText,
        title: title || "Release Updates",
        date,
        isLatest,
        categories: [],
      };

      releases.push(currentRelease);
      currentCategory = null;
      continue;
    }

    // Category / Section Header: ### ✨ Improvements
    if (trimmed.startsWith("### ") || trimmed.startsWith("#### ")) {
      const categoryTitle = trimmed.replace(/^#{3,4}\s+/, "").trim();
      if (!currentRelease) {
        currentRelease = {
          id: `release-1-current`,
          version: "Latest",
          rawVersionHeader: "Latest Updates",
          title: "Recent Changes",
          isLatest: true,
          categories: [],
        };
        releases.push(currentRelease);
      }

      currentCategory = {
        id: `cat-${currentRelease.categories.length + 1}-${categoryTitle.replace(/[^a-zA-Z0-9]/g, "-")}`,
        title: categoryTitle,
        iconType: detectCategoryIcon(categoryTitle),
        items: [],
      };
      currentRelease.categories.push(currentCategory);
      continue;
    }

    // List item: - Item text or * Item text
    if (/^[-*+]\s+/.test(trimmed)) {
      const itemRaw = trimmed.replace(/^[-*+]\s+/, "").trim();
      
      if (!currentRelease) {
        currentRelease = {
          id: `release-1-current`,
          version: "Latest",
          rawVersionHeader: "Latest Updates",
          title: "Recent Changes",
          isLatest: true,
          categories: [],
        };
        releases.push(currentRelease);
      }

      if (!currentCategory) {
        currentCategory = {
          id: `cat-general-${currentRelease.categories.length + 1}`,
          title: "Highlights & Changes",
          iconType: "general",
          items: [],
        };
        currentRelease.categories.push(currentCategory);
      }

      // Check if item starts with a bold title e.g. **Launcher**: text
      let itemTitle: string | undefined;
      let itemTag: string | undefined;

      const tagMatch = itemRaw.match(/^\[([a-zA-Z0-9\s]+)\]/);
      if (tagMatch) {
        itemTag = tagMatch[1];
      }

      currentCategory.items.push({
        id: `item-${currentRelease.id}-${currentCategory.id}-${currentCategory.items.length + 1}`,
        rawText: itemRaw,
        tag: itemTag,
        title: itemTitle,
        content: parseInlineFormatting(itemRaw),
      });
      continue;
    }

    // Plain text / paragraph
    if (!currentRelease) {
      introLines.push(trimmed);
    } else if (currentCategory && currentCategory.items.length > 0) {
      // Append as extra line/details to last item
      const lastItem = currentCategory.items[currentCategory.items.length - 1];
      lastItem.rawText += ` ${trimmed}`;
      lastItem.content = parseInlineFormatting(lastItem.rawText);
    } else if (currentRelease && !currentRelease.summary) {
      currentRelease.summary = trimmed;
    }
  }

  // Fallback if no structured releases found
  if (releases.length === 0 && markdown.trim()) {
    releases.push({
      id: "release-default",
      version: "Notes",
      rawVersionHeader: "Release Notes",
      title: "Changelog",
      isLatest: true,
      categories: [
        {
          id: "cat-default",
          title: "Updates",
          iconType: "general",
          items: markdown
            .split(/\r?\n/)
            .map((l) => l.trim())
            .filter(Boolean)
            .map((line, idx) => ({
              id: `default-item-${idx}`,
              rawText: line,
              content: parseInlineFormatting(line),
            })),
        },
      ],
    });
  }

  return {
    mainTitle,
    releases,
  };
}
