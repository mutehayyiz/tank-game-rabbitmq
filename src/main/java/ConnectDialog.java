import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

class ConnectDialog extends Dialog {
    Button loginButton = new Button("Login");
    TextField textFieldUserName = new TextField("", 16);
    Label usernameLabel = new Label("Username :");
    TankGame tankGame;

    ConnectDialog(TankGame tankGame) {
        super(tankGame, true);
        this.tankGame = tankGame;
        this.setLayout(new FlowLayout());
        this.add(usernameLabel);
        this.add(textFieldUserName);
        this.add(loginButton);

        loginButton.addActionListener(e -> login(textFieldUserName.getText().trim()));

        this.pack();
        this.setResizable(false);
        this.setLocationRelativeTo(null);

        this.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                //TODO update
                if (!tankGame.client.connectionID.equals("")) {
                    tankGame.client.send(MsgType.CLOSE_APP, new CloseApp(tankGame.client.connectionID, tankGame.user.username).Token());
                }
                System.exit(0);
            }
        });

    }

    private void login(String username) {
        if (username.equals("")) {
            return;
        }

        tankGame.user = new User(username);
        this.setVisible(false);
        tankGame.client.send(MsgType.LOGIN_REQUEST, new LoginRequest(username).Token());
    }
}