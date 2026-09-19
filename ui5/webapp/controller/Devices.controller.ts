import JSONModel from "sap/ui/model/json/JSONModel";
import Event from "sap/ui/base/Event";
import ListItemBase from "sap/m/ListItemBase";
import MessageToast from "sap/m/MessageToast";
import BaseController from "./BaseController";
import { CreateDevice } from "../model/ApiClient";

const NEW_DEVICE_DIALOG = "udi.ui5.view.fragment.NewDeviceDialog";

/**
 * Device list: server-side search and status filter, navigation to the detail page, creation for editors.
 * @namespace udi.ui5.controller
 */
export default class Devices extends BaseController {

    public override onInit(): void {
        this.getView()!.setModel(new JSONModel({ items: [], count: 0, search: "", status: "", busy: true }), "devices");
        this.getView()!.setModel(new JSONModel(Devices.emptyForm()), "form");
        this.router().getRoute("devices")!.attachPatternMatched(() => void this.load(), this);
    }

    public onSearch(): void {
        void this.load();
    }

    public onItemPress(event: Event): void {
        const context = event.getSource<ListItemBase>().getBindingContext("devices")!;
        this.router().navTo("device", { id: context.getProperty("id") as string });
    }

    public async onNewDevice(): Promise<void> {
        this.model("form").setData(Devices.emptyForm());
        (await this.dialog(NEW_DEVICE_DIALOG)).open();
    }

    public async onCreateDevice(): Promise<void> {
        const form = this.model("form").getData() as CreateDevice;
        try {
            const created = await this.api().createDevice({ ...form, udiDi: form.udiDi.trim(), name: form.name.trim(), manufacturer: form.manufacturer.trim() });
            (await this.dialog(NEW_DEVICE_DIALOG)).close();
            MessageToast.show(this.text("msg.created", [created.udiDi]));
            this.router().navTo("device", { id: created.id });
        } catch (error) {
            this.showError(error);
        }
    }

    private async load(): Promise<void> {
        const model = this.model("devices");
        model.setProperty("/busy", true);
        try {
            const items = await this.api().listDevices(model.getProperty("/search") as string, model.getProperty("/status") as string);
            model.setProperty("/items", items);
            model.setProperty("/count", items.length);
        } catch (error) {
            this.showError(error);
        } finally {
            model.setProperty("/busy", false);
        }
    }

    private static emptyForm(): CreateDevice {
        return { udiDi: "", name: "", manufacturer: "", riskClass: "IIA" };
    }
}
