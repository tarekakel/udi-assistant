import DateFormat from "sap/ui/core/format/DateFormat";
import { ValueState } from "sap/ui/core/library";

const dateTime = DateFormat.getDateTimeInstance({ style: "medium" });

const RISK_CLASS_LABELS: Record<string, string> = { I: "I", IIA: "IIa", IIB: "IIb", III: "III" };

export default {

    /** Semantic colour of a registration status: what a reviewer's eye should land on. */
    statusState(status?: string): ValueState {
        switch (status) {
            case "REGISTERED": return ValueState.Success;
            case "SUBMITTED": return ValueState.Warning;
            case "WITHDRAWN": return ValueState.Error;
            default: return ValueState.None;
        }
    },

    dateTime(iso?: string | null): string {
        return iso ? dateTime.format(new Date(iso)) : "";
    },

    riskClass(value?: string | null): string {
        return value ? RISK_CLASS_LABELS[value] ?? value : "";
    },

    /** Audit trail cells: an empty old value on CREATE reads better as a dash than as nothing. */
    valueOrDash(value?: string | null): string {
        return value === null || value === undefined || value === "" ? "–" : value;
    }
};
