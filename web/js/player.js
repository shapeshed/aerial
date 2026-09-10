// Thin wrapper around the browser's native <audio> element. No streaming
// library, no Media Session polyfills beyond the built-in API — see
// web/SCOPE.md ("Plain HTML, CSS, and JavaScript only").

export class Player {
  constructor(audioElement, { onStateChange } = {}) {
    this.audio = audioElement;
    this.current = null;
    this.onStateChange = onStateChange || (() => {});

    this.audio.addEventListener("playing", () => this._emit("playing"));
    this.audio.addEventListener("pause", () => this._emit("paused"));
    this.audio.addEventListener("waiting", () => this._emit("loading"));
    this.audio.addEventListener("error", () => this._emit("error"));
  }

  _emit(state) {
    this.onStateChange(state, this.current);
  }

  play(station) {
    this.current = station;
    this.audio.src = station.streamUrl;
    this._emit("loading");
    this.audio.play().catch(() => this._emit("error"));

    if ("mediaSession" in navigator) {
      navigator.mediaSession.metadata = new MediaMetadata({
        title: station.name,
        artist: station.country || "",
        artwork: station.logoUrl ? [{ src: station.logoUrl }] : [],
      });
    }
  }

  pause() {
    this.audio.pause();
  }

  togglePlayPause(station) {
    if (this.current && stationSameStream(this.current, station) && !this.audio.paused) {
      this.pause();
    } else {
      this.play(station);
    }
  }

  isPlaying(station) {
    return (
      this.current !== null &&
      stationSameStream(this.current, station) &&
      !this.audio.paused
    );
  }
}

function stationSameStream(a, b) {
  return a.streamUrl === b.streamUrl;
}
