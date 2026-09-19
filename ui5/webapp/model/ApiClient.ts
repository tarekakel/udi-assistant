/**
 * Thin, typed client for the udi-assistant REST API. Same origin in both deployments: on BTP the approuter
 * forwards the XSUAA token, locally Spring serves the built app and identifies the user by the X-User header.
 */

export type Mode = "local" | "btp";
export type RegistrationStatus = "DRAFT" | "SUBMITTED" | "REGISTERED" | "WITHDRAWN";
export type RiskClass = "I" | "IIA" | "IIB" | "III";

export interface Session {
    mode: Mode;
    user: string;
    roles: string[];
    canEdit: boolean;
}

export interface DeviceDto {
    id: string;
    udiDi: string;
    name: string;
    manufacturer: string;
    riskClass: RiskClass;
    registrationStatus: RegistrationStatus;
    allowedTransitions: RegistrationStatus[];
    version: number;
    createdAt: string;
    createdBy: string;
    updatedAt: string;
    updatedBy: string;
}

export interface AuditEntryDto {
    id: string;
    action: "CREATE" | "UPDATE" | "STATUS_CHANGE";
    field: string | null;
    oldValue: string | null;
    newValue: string | null;
    reason: string | null;
    performedBy: string;
    performedAt: string;
}

export interface CreateDevice {
    udiDi: string;
    name: string;
    manufacturer: string;
    riskClass: RiskClass;
}

export interface UpdateDevice {
    name: string;
    manufacturer: string;
    riskClass: RiskClass;
    version: number;
    reason: string;
}

export interface ChangeStatus {
    status: RegistrationStatus;
    reason: string;
}

interface ProblemDetail {
    title?: string;
    detail?: string;
    errors?: { field: string; message: string }[];
}

/** An RFC 9457 problem detail from the service, surfaced with its human-readable message. */
export class ApiError extends Error {
    constructor(public readonly status: number, message: string, public readonly errors: { field: string; message: string }[] = []) {
        super(message);
    }
}

interface CurrentUser {
    name?: string;
    email?: string;
    scopes?: string[];
}

export default class ApiClient {

    private mode: Mode = "btp";
    private user = "";

    /** Resolves how we are deployed: approuter user API present (BTP) or 404 from Spring (local, name from storage). */
    public async session(): Promise<Session> {
        let response: Response;
        try {
            response = await fetch("/user-api/currentUser", { headers: { Accept: "application/json" } });
        } catch (error) {
            return this.signInAgain();
        }
        if (response.status === 404) {
            this.mode = "local";
            this.user = this.storedName() || "ui5-user";
            return { mode: "local", user: this.user, roles: ["Viewer", "Editor"], canEdit: true };
        }
        if (!response.ok || !ApiClient.isJson(response)) {
            return this.signInAgain();
        }
        const current = (await response.json()) as CurrentUser;
        const roles = (current.scopes ?? []).map((scope) => scope.substring(scope.lastIndexOf(".") + 1));
        this.user = current.email ?? current.name ?? "";
        return { mode: "btp", user: this.user, roles, canEdit: roles.includes("Editor") };
    }

    public listDevices(search: string, status: string): Promise<DeviceDto[]> {
        const params = new URLSearchParams({ size: "200", sort: "name" });
        if (search) { params.set("search", search); }
        if (status) { params.set("status", status); }
        return this.request<{ content: DeviceDto[] }>(`/api/devices?${params}`).then((page) => page.content);
    }

    public getDevice(id: string): Promise<DeviceDto> {
        return this.request<DeviceDto>(`/api/devices/${encodeURIComponent(id)}`);
    }

    public auditTrail(id: string): Promise<AuditEntryDto[]> {
        return this.request<AuditEntryDto[]>(`/api/devices/${encodeURIComponent(id)}/audit-trail`);
    }

    public createDevice(body: CreateDevice): Promise<DeviceDto> {
        return this.request<DeviceDto>("/api/devices", { method: "POST", body: JSON.stringify(body) });
    }

    public updateDevice(id: string, body: UpdateDevice): Promise<DeviceDto> {
        return this.request<DeviceDto>(`/api/devices/${encodeURIComponent(id)}`, { method: "PUT", body: JSON.stringify(body) });
    }

    public changeStatus(id: string, body: ChangeStatus): Promise<DeviceDto> {
        return this.request<DeviceDto>(`/api/devices/${encodeURIComponent(id)}/status`, { method: "POST", body: JSON.stringify(body) });
    }

    private async request<T>(path: string, init: RequestInit = {}): Promise<T> {
        const headers: Record<string, string> = { Accept: "application/json" };
        if (init.body) { headers["Content-Type"] = "application/json"; }
        if (this.mode === "local") { headers["X-User"] = this.user; }
        const response = await fetch(path, { ...init, headers });
        if (response.redirected || (!ApiClient.isJson(response) && response.status !== 204)) {
            return this.signInAgain();   // the router's session expired: it answered with the login page
        }
        if (!response.ok) {
            const problem = (await response.json()) as ProblemDetail;
            const details = (problem.errors ?? []).map((e) => `${e.field}: ${e.message}`).join("\n");
            throw new ApiError(response.status, [problem.detail ?? problem.title ?? response.statusText, details].filter(Boolean).join("\n"), problem.errors);
        }
        return (await response.json()) as T;
    }

    private signInAgain<T>(): Promise<T> {
        window.location.replace("/index.html");
        return new Promise<T>(() => { /* navigating away; never resolves */ });
    }

    private storedName(): string | null {
        try { return window.localStorage.getItem("udiUser"); } catch (error) { return null; }
    }

    private static isJson(response: Response): boolean {
        return (response.headers.get("content-type") ?? "").includes("json");
    }
}
