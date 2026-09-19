import Controller from "sap/ui/core/mvc/Controller";
import JSONModel from "sap/ui/model/json/JSONModel";
import ResourceModel from "sap/ui/model/resource/ResourceModel";
import ResourceBundle from "sap/base/i18n/ResourceBundle";
import MessageBox from "sap/m/MessageBox";
import Router from "sap/ui/core/routing/Router";
import Dialog from "sap/m/Dialog";
import Event from "sap/ui/base/Event";
import Control from "sap/ui/core/Control";
import Component from "../Component";
import ApiClient, { ApiError } from "../model/ApiClient";
import formatter from "../model/formatter";

/**
 * What every view controller needs: the component's API client, the router, texts, and one way to show errors.
 * @namespace udi.ui5.controller
 */
export default abstract class BaseController extends Controller {

    public formatter = formatter;

    protected component(): Component {
        return this.getOwnerComponent() as Component;
    }

    protected api(): ApiClient {
        return this.component().api;
    }

    protected router(): Router {
        return this.component().getRouter();
    }

    protected model(name: string): JSONModel {
        return this.getView()!.getModel(name) as JSONModel;
    }

    protected text(key: string, args?: unknown[]): string {
        const bundle = (this.component().getModel("i18n") as ResourceModel).getResourceBundle() as ResourceBundle;
        return bundle.getText(key, args as string[]) ?? key;
    }

    /** Status labels come from i18n so the German UI reads "Registriert", not "REGISTERED". */
    public statusText(status?: string): string {
        return status ? this.text(`status.${status}`) : "";
    }

    protected showError(error: unknown): void {
        const message = error instanceof ApiError ? error.message : error instanceof Error ? error.message : String(error);
        MessageBox.error(message);
    }

    /** Any dialog's Cancel button: the button's parent is the dialog. */
    public onCloseDialog(event: Event): void {
        (event.getSource<Control>().getParent() as Dialog).close();
    }

    /** Fragments are loaded once and reused; ids are view-prefixed, so byId() finds them afterwards. */
    protected async dialog(name: string): Promise<Dialog> {
        const id = name.substring(name.lastIndexOf(".") + 1);
        const existing = this.byId(id) as Dialog | undefined;
        if (existing) {
            return existing;
        }
        const dialog = (await this.loadFragment({ name, id: this.getView()!.getId() })) as Dialog;
        dialog.addStyleClass(this.component().getContentDensityClass());
        return dialog;
    }
}
