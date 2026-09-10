import { loadStations, stationKey } from "./registry.js";
import { curatedGroups } from "./curated.js";
import { searchStations } from "./search.js";
import { listFavorites, isFavorite, toggleFavorite, mergeImported } from "./favorites.js";
import { Player } from "./player.js";
import { exportBackup, importBackup } from "./backup.js";

const el = (id) => document.getElementById(id);

const audioEl = el("audio");
const player = new Player(audioEl, { onStateChange: onPlayerStateChange });

let allStations = [];

init();

async function init() {
  wireTabs();
  wireSearch();
  wireBackup();
  wireNowPlaying();

  try {
    allStations = await loadStations();
    el("browse-status").hidden = true;
    renderBrowse();
  } catch (err) {
    el("browse-status").textContent = `Could not load stations: ${err.message}`;
  }

  renderFavorites();
}

// ---- Tabs ----------------------------------------------------------------

function wireTabs() {
  document.querySelectorAll(".tab").forEach((tab) => {
    tab.addEventListener("click", () => {
      document.querySelectorAll(".tab").forEach((t) => t.classList.remove("active"));
      document.querySelectorAll(".view").forEach((v) => v.classList.remove("active"));
      tab.classList.add("active");
      el(`view-${tab.dataset.view}`).classList.add("active");
      if (tab.dataset.view === "favorites") renderFavorites();
    });
  });
}

// ---- Browse ----------------------------------------------------------------

function renderBrowse() {
  const container = el("curated-groups");
  container.innerHTML = "";

  const groups = curatedGroups(allStations);
  for (const group of groups) {
    const section = document.createElement("section");
    section.className = "mood-group";

    const heading = document.createElement("h2");
    heading.textContent = group.title;
    const desc = document.createElement("p");
    desc.className = "mood-desc";
    desc.textContent = group.desc;

    const list = renderStationList(group.stations);

    section.append(heading, desc, list);
    container.append(section);
  }
}

// ---- Search ----------------------------------------------------------------

function wireSearch() {
  el("search-input").addEventListener("input", (e) => {
    const results = searchStations(allStations, e.target.value);
    const list = el("search-results");
    list.innerHTML = "";
    list.append(...renderStationList(results.slice(0, 200)).children);
  });
}

// ---- Favorites ----------------------------------------------------------------

function renderFavorites() {
  const favorites = listFavorites();
  const list = el("favorites-list");
  list.innerHTML = "";
  el("favorites-empty").hidden = favorites.length > 0;

  // Favorites are stored favorite-record-shaped; the station list/player
  // code works off the registry shape (logoUrl vs logoPath), so adapt.
  const asStations = favorites.map((f) => ({ ...f, logoUrl: f.logoPath }));
  list.append(...renderStationList(asStations).children);
}

function wireBackup() {
  el("export-btn").addEventListener("click", () => {
    const favorites = listFavorites();
    const blob = exportBackup(favorites);
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = "aerial-backup.zip";
    a.click();
    URL.revokeObjectURL(url);
    showBackupStatus(`Exported ${favorites.length} station(s).`);
  });

  el("import-input").addEventListener("change", async (e) => {
    const file = e.target.files[0];
    if (!file) return;
    try {
      const records = await importBackup(file);
      mergeImported(records);
      renderFavorites();
      showBackupStatus(`Imported ${records.length} station(s).`);
    } catch (err) {
      showBackupStatus(`Import failed: ${err.message}`);
    } finally {
      e.target.value = "";
    }
  });
}

function showBackupStatus(message) {
  const status = el("backup-status");
  status.textContent = message;
  status.hidden = false;
}

// ---- Shared station list rendering ----------------------------------------

function renderStationList(stations) {
  const list = document.createElement("ul");
  list.className = "station-list";

  for (const station of stations) {
    const item = document.createElement("li");
    item.className = "station-row";

    const logo = document.createElement("img");
    logo.className = "station-logo";
    logo.alt = "";
    logo.loading = "lazy";
    if (station.logoUrl) logo.src = station.logoUrl;

    const main = document.createElement("div");
    main.className = "station-main";
    const name = document.createElement("div");
    name.className = "station-name";
    name.textContent = station.name;
    const meta = document.createElement("div");
    meta.className = "station-meta";
    meta.textContent = [station.country, station.tags].filter(Boolean).join(" · ");
    main.append(name, meta);

    const playBtn = document.createElement("button");
    playBtn.className = "play-btn";
    playBtn.textContent = player.isPlaying(station) ? "⏸" : "▶";
    playBtn.setAttribute("aria-label", "Play");
    playBtn.addEventListener("click", () => {
      player.togglePlayPause(station);
    });

    const favBtn = document.createElement("button");
    favBtn.className = "fav-btn";
    const fav = isFavorite(station);
    favBtn.textContent = fav ? "★" : "☆";
    favBtn.classList.toggle("active", fav);
    favBtn.setAttribute("aria-label", "Toggle favorite");
    favBtn.addEventListener("click", () => {
      toggleFavorite(station);
      favBtn.classList.toggle("active");
      favBtn.textContent = favBtn.classList.contains("active") ? "★" : "☆";
      if (el("view-favorites").classList.contains("active")) renderFavorites();
    });

    item.append(logo, main, playBtn, favBtn);
    list.append(item);
  }

  return list;
}

// ---- Now playing bar ----------------------------------------------------------------

function wireNowPlaying() {
  el("now-playing-toggle").addEventListener("click", () => {
    if (player.current) player.togglePlayPause(player.current);
  });
}

function onPlayerStateChange(state, station) {
  if (!station) return;

  const bar = el("now-playing");
  bar.hidden = false;
  el("now-playing-name").textContent = station.name;
  el("now-playing-logo").src = station.logoUrl || "";
  el("now-playing-toggle").textContent = state === "paused" ? "Play" : "Pause";

  const statusText = { loading: "Connecting…", playing: "Playing", paused: "Paused", error: "Could not play this station" };
  el("now-playing-status").textContent = statusText[state] || "";

  // Refresh play/pause glyphs on any visible station rows for this stream.
  document.querySelectorAll(".station-row").forEach((row) => {
    const name = row.querySelector(".station-name")?.textContent;
    if (name !== station.name) return;
    const btn = row.querySelector(".play-btn");
    if (btn) btn.textContent = state === "playing" || state === "loading" ? "⏸" : "▶";
  });
}
