import { describe, expect, it } from "vitest";
import { getSafeBillingError } from "./useBillingAccess";

describe("getSafeBillingError", () => {
  it.each([
    [new Error("Redemption code not found"), "billing.errors.not_found"],
    [new Error("Redemption code is disabled"), "billing.errors.disabled"],
    [new Error("Redemption code has already been used"), "billing.errors.used"],
    [new Error("Code is required"), "billing.errors.required"],
    [new Error("Not authenticated"), "billing.errors.unauthorized"],
    [new Error("Unexpected backend detail"), "billing.errors.generic"],
  ])("maps %s to %s", (error, key) => {
    expect(getSafeBillingError(error)).toBe(key);
  });
});
