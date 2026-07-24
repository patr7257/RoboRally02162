// CRA auto-loads this file (via setupFilesAfterEnv) before every test file runs.
// See: https://create-react-app.dev/docs/running-tests/#src-setuptestsjs

import "@testing-library/jest-dom";

/**
 * Minimal EventSource stand-in for jsdom, which has no native implementation.
 * Installed on globalThis.EventSource so code under test (client/src/utils/ws.ts)
 * can `new EventSource(url)` without a real network connection. Tests drive the
 * connection by grabbing the latest instance and calling fire()/fireError().
 */
export class FakeEventSource {
  static instances: FakeEventSource[] = [];

  readonly url: string;
  readyState = 0; // CONNECTING
  onmessage: ((ev: MessageEvent) => void) | null = null;
  onerror: ((ev: Event) => void) | null = null;
  onopen: ((ev: Event) => void) | null = null;

  constructor(url: string) {
    this.url = url;
    FakeEventSource.instances.push(this);
  }

  /** Simulates a server-sent message. `data` is JSON.stringified unless already a string. */
  fire(data: unknown): void {
    const payload = typeof data === "string" ? data : JSON.stringify(data);
    this.onmessage?.(new MessageEvent("message", { data: payload }));
  }

  /** Simulates the connection erroring out (EventSource auto-reconnects in real browsers). */
  fireError(): void {
    this.onerror?.(new Event("error"));
  }

  close(): void {
    this.readyState = 2; // CLOSED
  }

  /** Drops every recorded instance; call from a test's beforeEach/afterEach. */
  static reset(): void {
    FakeEventSource.instances = [];
  }
}

(globalThis as unknown as { EventSource: typeof FakeEventSource }).EventSource =
  FakeEventSource;

afterEach(() => {
  FakeEventSource.reset();
  sessionStorage.clear();
});
