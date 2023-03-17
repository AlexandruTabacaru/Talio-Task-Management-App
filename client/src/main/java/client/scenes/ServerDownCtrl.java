package client.scenes;


import javax.inject.Inject;


public class ServerDownCtrl {

    private final MainCtrl mainCtrl;

    @Inject
    public ServerDownCtrl(final MainCtrl mainCtrl) {
        this.mainCtrl = mainCtrl;
    }



    //used for the go back button
    public void showSelectServer() {
        mainCtrl.showSelectServer();
    }
}
