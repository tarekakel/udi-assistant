import BaseController from "./BaseController";

/**
 * @namespace udi.ui5.controller
 */
export default class App extends BaseController {

    public override onInit(): void {
        this.getView()!.addStyleClass(this.component().getContentDensityClass());
    }
}
