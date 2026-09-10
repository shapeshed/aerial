import { test } from "node:test";
import assert from "node:assert/strict";
import { Player } from "../js/player.js";

// A minimal stand-in for a real <audio> element — just enough surface for
// Player to drive: src, paused, play()/pause(), and the events it listens
// for. No jsdom, no browser — see web/SCOPE.md ("no framework, no
// bundler").
function fakeAudioElement() {
  const listeners = {};
  return {
    src: "",
    paused: true,
    addEventListener(event, handler) {
      (listeners[event] ||= []).push(handler);
    },
    emit(event) {
      for (const handler of listeners[event] || []) handler();
    },
    play() {
      this.paused = false;
      this.emit("playing");
      return Promise.resolve();
    },
    pause() {
      this.paused = true;
      this.emit("pause");
    },
  };
}

const stationA = { name: "A", streamUrl: "https://a.example/stream.mp3", country: "", logoUrl: "" };
const stationB = { name: "B", streamUrl: "https://b.example/stream.mp3", country: "", logoUrl: "" };

test("play() sets the audio src and starts playback", () => {
  const audio = fakeAudioElement();
  const player = new Player(audio);

  player.play(stationA);

  assert.equal(audio.src, stationA.streamUrl);
  assert.equal(audio.paused, false);
  assert.equal(player.isPlaying(stationA), true);
});

test("togglePlayPause pauses the currently playing station", () => {
  const audio = fakeAudioElement();
  const player = new Player(audio);

  player.play(stationA);
  player.togglePlayPause(stationA);

  assert.equal(audio.paused, true);
  assert.equal(player.isPlaying(stationA), false);
});

test("togglePlayPause switches to a different station rather than pausing", () => {
  const audio = fakeAudioElement();
  const player = new Player(audio);

  player.play(stationA);
  player.togglePlayPause(stationB);

  assert.equal(audio.src, stationB.streamUrl);
  assert.equal(player.isPlaying(stationB), true);
  assert.equal(player.isPlaying(stationA), false);
});

test("onStateChange callback fires with the current station on playing/paused", () => {
  const audio = fakeAudioElement();
  const events = [];
  const player = new Player(audio, { onStateChange: (state, station) => events.push([state, station?.name]) });

  player.play(stationA);
  player.pause();

  assert.deepEqual(events, [
    ["loading", "A"],
    ["playing", "A"],
    ["paused", "A"],
  ]);
});
