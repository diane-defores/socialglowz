import { describe, expect, it } from "vitest";
import {
  assertAvatar,
  assertEntityId,
  assertFriendNames,
  assertHttpUrl,
  assertLanguage,
  assertNetworkId,
  assertTextZoom,
} from "./validators";

describe("convex validators", () => {
  it("accepts valid http/https URLs and rejects unsafe schemes", () => {
    expect(() => assertHttpUrl("https://example.com/a?b=1")).not.toThrow();
    expect(() => assertHttpUrl("http://example.com")).not.toThrow();
    expect(() => assertHttpUrl("javascript:alert(1)")).toThrow(/protocol/i);
    expect(() => assertHttpUrl("file:///tmp/x")).toThrow(/protocol/i);
    expect(() => assertHttpUrl("https://example.com/\nfoo")).toThrow(/line breaks/i);
  });

  it("enforces id and network formats", () => {
    expect(() => assertEntityId("custom-123_ABC", "profileId")).not.toThrow();
    expect(() => assertEntityId(" bad ", "profileId")).toThrow(/spaces/i);
    expect(() => assertNetworkId("twitter")).not.toThrow();
    expect(() => assertNetworkId("tw itter")).toThrow(/unsupported/i);
  });

  it("enforces language and text zoom constraints", () => {
    expect(() => assertLanguage("fr")).not.toThrow();
    expect(() => assertLanguage("en-US")).not.toThrow();
    expect(() => assertLanguage("fr_fr")).toThrow(/match/i);
    expect(() => assertTextZoom(50)).not.toThrow();
    expect(() => assertTextZoom(100)).not.toThrow();
    expect(() => assertTextZoom(45)).toThrow(/between 50 and 200/i);
  });

  it("validates avatar payload shape", () => {
    expect(() => assertAvatar(undefined)).not.toThrow();
    expect(() => assertAvatar("https://example.com/avatar.png")).not.toThrow();
    expect(() => assertAvatar("data:image/png;base64,abcd")).not.toThrow();
    expect(() => assertAvatar("data:text/plain;base64,abcd")).toThrow(/image/i);
    expect(() => assertAvatar("intent://x")).toThrow(/http or https/i);
  });

  it("enforces friend names list invariants", () => {
    expect(() => assertFriendNames(["Alice", "Bob"])).not.toThrow();
    expect(() => assertFriendNames(["Alice", "alice"])).toThrow(/unique/i);
    expect(() => assertFriendNames([""])).toThrow(/length/i);
  });
});
