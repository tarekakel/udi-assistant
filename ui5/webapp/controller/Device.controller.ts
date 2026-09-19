import JSONModel from "sap/ui/model/json/JSONModel";
import Event from "sap/ui/base/Event";
import Button from "sap/m/Button";
import MessageToast from "sap/m/MessageToast";
import { Route$PatternMatchedEvent } from "sap/ui/core/routing/Route";
import BaseController from "./BaseController";
import { DeviceDto, RegistrationStatus, RiskClass } from "../model/ApiClient";

const STATUS_DIALOG = "udi.ui5.view.fragment.StatusDialog";
const EDIT_DIALOG = "udi.ui5.view.fragment.EditDialog";

interface DeviceForm {
    title: string;
    status: RegistrationStatus | "";
    name: string;
    manufacturer: string;
    riskClass: RiskClass;
    version: number;
    reason: string;
}

/**
 * One device: master data, the transitions the lifecycle allows next, and its audit trail. Every write asks for a
 * reason and sends the record version, so a stale edit is refused by the service instead of silently overwriting.
 * @namespace udi.ui5.controller
 */
export default class Device extends BaseController {

    private id = "";

    public override onInit(): void {
        this.getView()!.setModel(new JSONModel({ device: null, trail: [], busy: true }), "device");
        this.getView()!.setModel(new JSONModel(Device.emptyForm()), "form");
        this.router().getRoute("device")!.attachPatternMatched(this.onRouteMatched, this);
    }

    public onNavBack(): void {
        this.router().navTo("devices");
    }

    /** Transition buttons are bound to the device's allowedTransitions array; the item itself is the target status. */
    public transitionText(status?: string): string {
        return status ? this.text(`transition.${status}`) : "";
    }

    public async onTransition(event: Event): Promise<void> {
        const status = event.getSource<Button>().getBindingContext("device")!.getObject() as RegistrationStatus;
        this.model("form").setData({ ...Device.emptyForm(), status, title: this.text("dialog.status", [this.statusText(status)]) });
        (await this.dialog(STATUS_DIALOG)).open();
    }

    public async onConfirmStatus(): Promise<void> {
        const form = this.model("form").getData() as DeviceForm;
        try {
            await this.api().changeStatus(this.id, { status: form.status as RegistrationStatus, reason: form.reason.trim() });
            (await this.dialog(STATUS_DIALOG)).close();
            MessageToast.show(this.text("msg.statusChanged", [this.statusText(form.status)]));
            await this.load();
        } catch (error) {
            this.showError(error);
        }
    }

    public async onEdit(): Promise<void> {
        const device = this.model("device").getProperty("/device") as DeviceDto;
        this.model("form").setData({
            ...Device.emptyForm(), title: this.text("dialog.edit"),
            name: device.name, manufacturer: device.manufacturer, riskClass: device.riskClass, version: device.version
        });
        (await this.dialog(EDIT_DIALOG)).open();
    }

    public async onSaveEdit(): Promise<void> {
        const form = this.model("form").getData() as DeviceForm;
        try {
            await this.api().updateDevice(this.id, {
                name: form.name.trim(), manufacturer: form.manufacturer.trim(), riskClass: form.riskClass,
                version: form.version, reason: form.reason.trim()
            });
            (await this.dialog(EDIT_DIALOG)).close();
            MessageToast.show(this.text("msg.saved"));
            await this.load();
        } catch (error) {
            this.showError(error);
        }
    }

    private onRouteMatched(event: Route$PatternMatchedEvent): void {
        this.id = (event.getParameter("arguments") as { id: string }).id;
        void this.load();
    }

    private async load(): Promise<void> {
        const model = this.model("device");
        model.setProperty("/busy", true);
        try {
            const [device, trail] = await Promise.all([this.api().getDevice(this.id), this.api().auditTrail(this.id)]);
            model.setProperty("/device", device);
            model.setProperty("/trail", trail);
        } catch (error) {
            this.showError(error);
            this.onNavBack();
        } finally {
            model.setProperty("/busy", false);
        }
    }

    private static emptyForm(): DeviceForm {
        return { title: "", status: "", name: "", manufacturer: "", riskClass: "IIA", version: 0, reason: "" };
    }
}
