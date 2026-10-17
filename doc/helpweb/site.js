const HELP_DATA = window.HELP_WEB_DATA || null;
const HELP_ROOT = "../helpjar/";
const HOME_TARGET = HELP_DATA ? HELP_DATA.homeTarget : "hem_html";
const HOME_URL = HELP_DATA ? normalizeUrl(HELP_DATA.homeUrl) : `${HELP_ROOT}html/hem.html`;

const contentsTab = document.getElementById("contentsTab");
const searchTab = document.getElementById("searchTab");
const contentsPanel = document.getElementById("contentsPanel");
const searchPanel = document.getElementById("searchPanel");
const tocTree = document.getElementById("tocTree");
const searchInput = document.getElementById("searchInput");
const searchResults = document.getElementById("searchResults");
const contentFrame = document.getElementById("contentFrame");
const pageTitle = document.getElementById("pageTitle");
const openExternal = document.getElementById("openExternal");

let activeTarget = HOME_TARGET;
let allTopics = [];
let topicByTarget = new Map();
let tocData = [];
let mapData = new Map();
let searchReady = false;

function normalizeUrl(url) {
    if (!url) {
        return url;
    }
    if (/^(https?:|file:|\/|\.{1,2}\/)/i.test(url)) {
        return url;
    }
    return `../${url}`;
}

function parseMap(text) {
    const result = new Map();
    const regex = /<mapID\s+target="([^"]+)"\s+url="([^"]+)"\/>/g;
    let match;
    while ((match = regex.exec(text)) !== null) {
        result.set(match[1], normalizeUrl(`${HELP_ROOT}${match[2]}`));
    }
    return result;
}

function parseTocNode(node) {
    const items = [];
    Array.from(node.children).forEach((child) => {
        if (child.tagName !== "tocitem") {
            return;
        }

        const item = {
            text: child.getAttribute("text") || "",
            target: child.getAttribute("target") || "",
            image: child.getAttribute("image") || "",
            children: parseTocNode(child).children,
        };
        items.push(item);
    });
    return { children: items };
}

function flattenTopics(items, trail = []) {
    items.forEach((item) => {
        const path = trail.concat(item.text || item.target || "Okänt");
        const url = item.target && mapData.get(item.target) ? mapData.get(item.target) : null;
        const topic = {
            text: item.text || item.target || "Okänt",
            target: item.target || "",
            url,
            path,
            searchText: `${path.join(" ")} ${item.target || ""}`.toLowerCase(),
            children: item.children || [],
        };
        if (topic.target) {
            topicByTarget.set(topic.target, topic);
        }
        allTopics.push(topic);
        if (item.children && item.children.length) {
            flattenTopics(item.children, path);
        }
    });
}

function buildFromEmbeddedData(data) {
    tocData = data.toc || [];
    allTopics = [];
    topicByTarget = new Map();
    mapData = new Map();

    const walk = (items, trail = []) => {
        items.forEach((item) => {
            const path = trail.concat(item.text || item.target || "Okänt");
            const topic = {
                text: item.text || item.target || "Okänt",
                target: item.target || "",
                url: normalizeUrl(item.url || ""),
                path,
                searchText: (item.searchText || `${path.join(" ")} ${item.target || ""}`).toLowerCase(),
                children: item.children || [],
            };
            if (topic.target) {
                topicByTarget.set(topic.target, topic);
            }
            if (topic.url && topic.target) {
                mapData.set(topic.target, topic.url);
            }
            allTopics.push(topic);
            if (item.children && item.children.length) {
                walk(item.children, path);
            }
        });
    };

    walk(tocData);
    searchReady = true;
}

function buildFromFiles(mapText, tocText) {
    mapData = parseMap(mapText);
    const tocDocument = new DOMParser().parseFromString(tocText, "application/xml");
    tocData = parseTocNode(tocDocument.documentElement).children;
    allTopics = [];
    topicByTarget = new Map();
    flattenTopics(tocData);
}

function createTreeNode(topic, level = 0) {
    const li = document.createElement("li");
    li.className = "toc-node";

    if (topic.target) {
        const button = document.createElement("button");
        button.type = "button";
        button.className = "toc-button";
        button.dataset.target = topic.target;
        button.style.paddingLeft = `${8 + level * 10}px`;
        button.textContent = topic.text;
        button.addEventListener("click", () => openTopic(topic.target, true));
        li.appendChild(button);
    } else {
        const label = document.createElement("div");
        label.className = "toc-label";
        label.style.paddingLeft = `${8 + level * 10}px`;
        label.textContent = topic.text;
        li.appendChild(label);
    }

    if (topic.children && topic.children.length) {
        const childList = document.createElement("ul");
        childList.className = "toc-children";
        topic.children.forEach((child) => {
            childList.appendChild(createTreeNode(child, level + 1));
        });
        li.appendChild(childList);
    }

    return li;
}

function renderTree(items) {
    tocTree.innerHTML = "";
    const ul = document.createElement("ul");
    items.forEach((item) => {
        ul.appendChild(createTreeNode(item));
    });
    tocTree.appendChild(ul);
    highlightActive();
}

function renderSearchResults(query) {
    const normalized = query.trim().toLowerCase();
    searchResults.innerHTML = "";

    if (!searchReady) {
        const loading = document.createElement("div");
        loading.className = "panel-hint";
        loading.textContent = "Bygger sökindex...";
        searchResults.appendChild(loading);
        return;
    }

    if (!normalized) {
        const empty = document.createElement("div");
        empty.className = "panel-hint";
        empty.textContent = "Skriv några bokstäver för att söka i innehållet.";
        searchResults.appendChild(empty);
        return;
    }

    const matches = allTopics.filter((topic) => topic.searchText.includes(normalized)).slice(0, 100);

    if (matches.length === 0) {
        const empty = document.createElement("div");
        empty.className = "panel-hint";
        empty.textContent = "Inga träffar.";
        searchResults.appendChild(empty);
        return;
    }

    const list = document.createElement("ul");
    matches.forEach((topic) => {
        const li = document.createElement("li");
        li.className = "search-result";
        const button = document.createElement("button");
        button.type = "button";
        button.className = "result-button";
        button.textContent = topic.path.join(" > ");
        button.addEventListener("click", () => openTopic(topic.target, true));
        li.appendChild(button);

        const meta = document.createElement("span");
        meta.className = "search-meta";
        meta.textContent = topic.url ? topic.url.replace("../", "") : "Ingen länkad sida";
        li.appendChild(meta);

        list.appendChild(li);
    });
    searchResults.appendChild(list);
}

function setActiveTab(tabName) {
    const isContents = tabName === "contents";
    contentsTab.classList.toggle("active", isContents);
    searchTab.classList.toggle("active", !isContents);
    contentsPanel.classList.toggle("active", isContents);
    searchPanel.classList.toggle("active", !isContents);
}

function highlightActive() {
    document.querySelectorAll(".toc-button, .result-button").forEach((element) => {
        const target = element.dataset.target;
        element.classList.toggle("active", target === activeTarget);
    });
}

function openTopic(target, pushHistory) {
    const topic = topicByTarget.get(target);
    const url = topic && topic.url ? topic.url : HOME_URL;

    activeTarget = target || HOME_TARGET;
    contentFrame.src = url;
    pageTitle.textContent = topic ? topic.text : "Hem";
    openExternal.href = url;
    highlightActive();

    if (pushHistory) {
        history.pushState({ target: activeTarget }, "", `#${encodeURIComponent(activeTarget)}`);
    }
}

function selectInitialTopic() {
    const hash = decodeURIComponent((location.hash || "").replace(/^#/, ""));
    if (hash && topicByTarget.has(hash)) {
        openTopic(hash, false);
        return;
    }
    openTopic(HOME_TARGET, false);
}

contentsTab.addEventListener("click", () => setActiveTab("contents"));
searchTab.addEventListener("click", () => setActiveTab("search"));
searchInput.addEventListener("input", (event) => renderSearchResults(event.target.value));
window.addEventListener("popstate", (event) => {
    const target = event.state && event.state.target ? event.state.target : HOME_TARGET;
    openTopic(target, false);
});

if (HELP_DATA) {
    buildFromEmbeddedData(HELP_DATA);
    renderTree(tocData);
    renderSearchResults("");
    selectInitialTopic();
} else {
    Promise.all([
        fetch(`${HELP_ROOT}default.map`).then((response) => response.text()),
        fetch(`${HELP_ROOT}toc.xml`).then((response) => response.text()),
    ]).then(([mapText, tocText]) => {
        buildFromFiles(mapText, tocText);
        renderTree(tocData);
        renderSearchResults("");
        selectInitialTopic();
        searchReady = true;
        return Promise.resolve();
    }).then(() => {
        renderSearchResults(searchInput.value);
    }).catch((error) => {
        tocTree.innerHTML = `<div class="panel-hint">Kunde inte läsa hjälpens innehåll.</div>`;
        searchResults.innerHTML = `<div class="panel-hint">Kunde inte läsa sökindexet.</div>`;
        pageTitle.textContent = "Fel";
        openExternal.removeAttribute("href");
        console.error(error);
    });
}
