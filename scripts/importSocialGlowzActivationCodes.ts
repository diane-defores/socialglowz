import { readFile } from "node:fs/promises";
import { basename } from "node:path";
import { pathToFileURL } from "node:url";
import { ConvexHttpClient } from "convex/browser";
import { Command } from "commander";
import { api } from "../convex/_generated/api";

const DEFAULT_PRODUCT_ID = "socialglowz";
const DEFAULT_PLAN_ID = "lifetime_deal";
const DEFAULT_SOURCE = "direct";
const DEFAULT_STATUS = "available";

const allowedSources = new Set([
  "appsumo",
  "direct",
  "legacy",
  "manual",
  "partner",
]);

const allowedStatuses = new Set(["available", "disabled"]);

export type ActivationCodeSource =
  | "appsumo"
  | "direct"
  | "legacy"
  | "manual"
  | "partner";

export type ActivationCodeStatus = "available" | "disabled";

export type ActivationCodeRecord = {
  code: string;
  productId: string;
  planId: string;
  source: ActivationCodeSource;
  status: ActivationCodeStatus;
  sourceRef?: string;
  externalOrderId?: string;
  note?: string;
};

type ImportResult = {
  index: number;
  code: string;
  ok: boolean;
  created?: boolean;
  error?: string;
};

type RawActivationCodeRecord = {
  code?: unknown;
  productId?: unknown;
  planId?: unknown;
  source?: unknown;
  status?: unknown;
  sourceRef?: unknown;
  externalOrderId?: unknown;
  note?: unknown;
};

export function normalizeActivationCode(code: string): string {
  return code.trim().toUpperCase().replace(/\s+/g, "-");
}

export function redactActivationCode(code: string): string {
  const normalized = normalizeActivationCode(code);
  if (normalized.length <= 6) return "[redacted]";
  return `${normalized.slice(0, 4)}...${normalized.slice(-2)}`;
}

function asOptionalString(value: unknown): string | undefined {
  if (value === undefined || value === null) return undefined;
  const normalized = String(value).trim();
  return normalized ? normalized : undefined;
}

function normalizeSource(value: unknown): ActivationCodeSource {
  const normalized = asOptionalString(value)?.toLowerCase() ?? DEFAULT_SOURCE;
  if (!allowedSources.has(normalized)) {
    throw new Error(`unsupported_source:${normalized}`);
  }
  return normalized as ActivationCodeSource;
}

function normalizeStatus(value: unknown): ActivationCodeStatus {
  const normalized = asOptionalString(value)?.toLowerCase() ?? DEFAULT_STATUS;
  if (!allowedStatuses.has(normalized)) {
    throw new Error(`unsupported_status:${normalized}`);
  }
  return normalized as ActivationCodeStatus;
}

function normalizeRecord(raw: RawActivationCodeRecord, index: number): ActivationCodeRecord {
  const code = normalizeActivationCode(asOptionalString(raw.code) ?? "");
  if (!code) {
    throw new Error(`row_${index}_code_required`);
  }

  const productId = asOptionalString(raw.productId) ?? DEFAULT_PRODUCT_ID;
  if (productId !== DEFAULT_PRODUCT_ID) {
    throw new Error(`row_${index}_product_not_allowed:${productId}`);
  }

  const planId = asOptionalString(raw.planId) ?? DEFAULT_PLAN_ID;
  if (planId !== DEFAULT_PLAN_ID && planId !== "founder_ltd") {
    throw new Error(`row_${index}_plan_not_allowed:${planId}`);
  }

  return {
    code,
    productId,
    planId,
    source: normalizeSource(raw.source),
    status: normalizeStatus(raw.status),
    sourceRef: asOptionalString(raw.sourceRef),
    externalOrderId: asOptionalString(raw.externalOrderId),
    note: asOptionalString(raw.note),
  };
}

function parseJson(raw: string): RawActivationCodeRecord[] {
  const payload = JSON.parse(raw) as unknown;
  if (Array.isArray(payload)) return payload as RawActivationCodeRecord[];
  if (
    payload &&
    typeof payload === "object" &&
    "codes" in payload &&
    Array.isArray((payload as { codes: unknown }).codes)
  ) {
    return (payload as { codes: RawActivationCodeRecord[] }).codes;
  }
  throw new Error("json_input_must_be_array_or_codes_object");
}

function parseJsonl(raw: string): RawActivationCodeRecord[] {
  return raw
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean)
    .map((line) => JSON.parse(line) as RawActivationCodeRecord);
}

function parseCsvRows(raw: string): string[][] {
  const rows: string[][] = [];
  let row: string[] = [];
  let cell = "";
  let inQuotes = false;

  for (let i = 0; i < raw.length; i += 1) {
    const char = raw[i];
    const next = raw[i + 1];

    if (char === '"' && inQuotes && next === '"') {
      cell += '"';
      i += 1;
      continue;
    }
    if (char === '"') {
      inQuotes = !inQuotes;
      continue;
    }
    if (char === "," && !inQuotes) {
      row.push(cell.trim());
      cell = "";
      continue;
    }
    if ((char === "\n" || char === "\r") && !inQuotes) {
      if (char === "\r" && next === "\n") i += 1;
      row.push(cell.trim());
      if (row.some(Boolean)) rows.push(row);
      row = [];
      cell = "";
      continue;
    }
    cell += char;
  }

  row.push(cell.trim());
  if (row.some(Boolean)) rows.push(row);
  return rows;
}

function parseCsv(raw: string): RawActivationCodeRecord[] {
  const rows = parseCsvRows(raw);
  if (rows.length === 0) return [];

  const headers = rows[0].map((header) => header.trim());
  if (!headers.includes("code")) {
    throw new Error("csv_header_requires_code");
  }

  return rows.slice(1).map((row) => {
    const record: Record<string, string> = {};
    headers.forEach((header, index) => {
      record[header] = row[index] ?? "";
    });
    return record;
  });
}

export function parseActivationCodeRecords(
  raw: string,
  format: "json" | "jsonl" | "csv",
): ActivationCodeRecord[] {
  const parsed =
    format === "json"
      ? parseJson(raw)
      : format === "jsonl"
        ? parseJsonl(raw)
        : parseCsv(raw);

  return parsed.map((record, index) => normalizeRecord(record, index + 1));
}

function inferFormat(filePath: string, explicitFormat?: string): "json" | "jsonl" | "csv" {
  if (explicitFormat === "json" || explicitFormat === "jsonl" || explicitFormat === "csv") {
    return explicitFormat;
  }
  const name = basename(filePath).toLowerCase();
  if (name.endsWith(".jsonl") || name.endsWith(".ndjson")) return "jsonl";
  if (name.endsWith(".csv")) return "csv";
  return "json";
}

async function importRecord(
  client: ConvexHttpClient,
  adminSecret: string,
  record: ActivationCodeRecord,
): Promise<{ created: boolean }> {
  const result = await client.action(api.billing.adminUpsertRedemptionCode, {
    adminSecret,
    code: record.code,
    productId: record.productId,
    planId: record.planId,
    source: record.source,
    status: record.status,
    sourceRef: record.sourceRef,
    externalOrderId: record.externalOrderId,
    note: record.note,
  });

  return { created: Boolean(result.created) };
}

async function runCli() {
  const argv = process.argv.filter((arg, index) => index < 2 || arg !== "--");
  const program = new Command()
    .name("importSocialGlowzActivationCodes")
    .description("Import SocialGlowz Lifetime Deal or early-bird activation codes through the suite entitlement bridge.")
    .requiredOption("-f, --file <path>", "JSON, JSONL, or CSV batch file")
    .option("--format <format>", "json, jsonl, or csv")
    .option("--convex-url <url>", "Convex deployment URL; defaults to VITE_CONVEX_URL or CONVEX_URL")
    .option("--admin-secret <secret>", "Admin secret; defaults to SOCIALGLOWZ_BILLING_ADMIN_SECRET")
    .option("--dry-run", "Parse and validate the batch without calling Convex")
    .option("--continue-on-error", "Continue after a row import fails")
    .parse(argv);

  const options = program.opts<{
    file: string;
    format?: string;
    convexUrl?: string;
    adminSecret?: string;
    dryRun?: boolean;
    continueOnError?: boolean;
  }>();

  const format = inferFormat(options.file, options.format);
  const raw = await readFile(options.file, "utf8");
  const records = parseActivationCodeRecords(raw, format);

  const results: ImportResult[] = [];

  if (options.dryRun) {
    for (const [index, record] of records.entries()) {
      results.push({ index: index + 1, code: record.code, ok: true, created: false });
    }
    printSummary(results, true);
    return;
  }

  const adminSecret = options.adminSecret ?? process.env.SOCIALGLOWZ_BILLING_ADMIN_SECRET;
  if (!adminSecret) throw new Error("missing_socialglowz_billing_admin_secret");

  const convexUrl = options.convexUrl ?? process.env.VITE_CONVEX_URL ?? process.env.CONVEX_URL;
  if (!convexUrl) throw new Error("missing_convex_url");

  const client = new ConvexHttpClient(convexUrl);
  for (const [index, record] of records.entries()) {
    try {
      const result = await importRecord(client, adminSecret, record);
      results.push({ index: index + 1, code: record.code, ok: true, created: result.created });
    } catch (error) {
      const message = error instanceof Error ? error.message : "unknown_error";
      results.push({ index: index + 1, code: record.code, ok: false, error: message });
      if (!options.continueOnError) {
        printSummary(results, false);
        process.exitCode = 1;
        return;
      }
    }
  }

  printSummary(results, false);
  if (results.some((result) => !result.ok)) {
    process.exitCode = 1;
  }
}

function printSummary(results: ImportResult[], dryRun: boolean) {
  const ok = results.filter((result) => result.ok).length;
  const failed = results.length - ok;
  const created = results.filter((result) => result.created).length;

  console.info(
    JSON.stringify(
      {
        dryRun,
        total: results.length,
        ok,
        failed,
        created,
        rows: results.map((result) => ({
          index: result.index,
          code: redactActivationCode(result.code),
          ok: result.ok,
          created: result.created,
          error: result.error,
        })),
      },
      null,
      2,
    ),
  );
}

if (import.meta.url === pathToFileURL(process.argv[1] ?? "").href) {
  runCli().catch((error) => {
    console.error(error instanceof Error ? error.message : error);
    process.exitCode = 1;
  });
}
