import { describe, expect, it } from "vitest";
import {
  normalizeActivationCode,
  parseActivationCodeRecords,
  redactActivationCode,
} from "./importSocialGlowzActivationCodes";

describe("importSocialGlowzActivationCodes", () => {
  it("normalizes activation codes without exposing raw values", () => {
    expect(normalizeActivationCode(" ltd  007 ")).toBe("LTD-007");
    expect(redactActivationCode(" ltd  007 ")).toBe("LTD-...07");
    expect(redactActivationCode("abc")).toBe("[redacted]");
  });

  it("parses JSON batches with safe defaults", () => {
    const records = parseActivationCodeRecords(
      JSON.stringify([
        {
          code: "early bird 001",
          sourceRef: "campaign-1",
        },
      ]),
      "json",
    );

    expect(records).toEqual([
      {
        code: "EARLY-BIRD-001",
        productId: "socialglowz",
        planId: "lifetime_deal",
        source: "direct",
        status: "available",
        sourceRef: "campaign-1",
        externalOrderId: undefined,
        note: undefined,
      },
    ]);
  });

  it("parses CSV batches with quoted notes", () => {
    const records = parseActivationCodeRecords(
      [
        "code,source,status,sourceRef,note",
        '"partner 001",partner,disabled,agency-a,"First batch, private"',
      ].join("\n"),
      "csv",
    );

    expect(records[0]).toMatchObject({
      code: "PARTNER-001",
      source: "partner",
      status: "disabled",
      sourceRef: "agency-a",
      note: "First batch, private",
    });
  });

  it("rejects product, source, and status values outside the allowlist", () => {
    expect(() =>
      parseActivationCodeRecords(
        JSON.stringify([{ code: "x", productId: "other" }]),
        "json",
      ),
    ).toThrow(/product_not_allowed/);

    expect(() =>
      parseActivationCodeRecords(
        JSON.stringify([{ code: "x", source: "unknown" }]),
        "json",
      ),
    ).toThrow(/unsupported_source/);

    expect(() =>
      parseActivationCodeRecords(
        JSON.stringify([{ code: "x", status: "redeemed" }]),
        "json",
      ),
    ).toThrow(/unsupported_status/);
  });
});
