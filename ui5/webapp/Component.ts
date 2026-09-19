import UIComponent from "sap/ui/core/UIComponent";
import JSONModel from "sap/ui/model/json/JSONModel";
import Device from "sap/ui/Device";
import ApiClient from "./model/ApiClient";

/**
 * @namespace udi.ui5
 */
export default class Component extends UIComponent {

    public static metadata = {
        manifest: "json",
        interfaces: ["sap.ui.core.IAsyncContentCreation"]
    };

    public api: ApiClient;
    private contentDensityClass: string;

    public override init(): void {
        super.init();
        this.api = new ApiClient();
        // Who is signed in and what they may do: the sign-in page (XSUAA on BTP, a name locally) decided that already.
        this.setModel(new JSONModel({ ready: false, mode: "", user: "", roles: [] as string[], canEdit: false }), "session");
        void this.resolveSession();
        this.getRouter().initialize();
    }

    private async resolveSession(): Promise<void> {
        const session = await this.api.session();
        (this.getModel("session") as JSONModel).setData({ ...session, ready: true });
    }

    public getContentDensityClass(): string {
        if (this.contentDensityClass === undefined) {
            this.contentDensityClass = Device.support.touch ? "sapUiSizeCozy" : "sapUiSizeCompact";
        }
        return this.contentDensityClass;
    }
}
